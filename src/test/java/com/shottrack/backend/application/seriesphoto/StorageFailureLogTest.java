package com.shottrack.backend.application.seriesphoto;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shottrack.backend.application.seriesphoto.gateway.SeriesPhotoGateway;
import com.shottrack.backend.application.seriesphoto.model.SeriesPhoto;
import com.shottrack.backend.application.series.dto.RegisterSeriesRequest;
import com.shottrack.backend.application.user.dto.DeleteAccountRequest;
import com.shottrack.backend.application.user.gateway.repository.PendingRegistrationRepository;
import com.shottrack.backend.support.SeriesTestSupport;
import com.shottrack.backend.support.TestImages;
import com.shottrack.backend.support.TestUsers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * ADR-0026: falha de armazenamento (gravar, ler, apagar) vai para o log só com a OPERAÇÃO e a CLASSE da exceção —
 * sem caminho, sem UUID de atleta/série/foto, sem nome de arquivo e sem stack trace. A falha é real (permissão do
 * diretório/arquivo), não simulada. NÃO é @Transactional: o apagar roda depois do commit. Sem efeito como root.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ExtendWith(OutputCaptureExtension.class)
class StorageFailureLogTest {

    private static final String PASSWORD = "senha12345";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private PendingRegistrationRepository pendingRegistrationRepository;
    @Autowired
    private SeriesPhotoGateway photoGateway;
    @Autowired
    private com.shottrack.backend.application.seriesphoto.storage.StorageProperties storageProperties;
    @Autowired
    private com.shottrack.backend.application.user.gateway.repository.UserRepository userRepository;

    private String email;
    private String token;
    private UUID seriesId;
    private UUID photoId;
    private UUID athleteId;
    private Path photoFile;
    private Path seriesDir;

    @BeforeEach
    void prepare() throws Exception {
        email = "foto.log." + UUID.randomUUID() + "@shottrack.com";
        token = TestUsers.registerAndLogin(mockMvc, objectMapper, pendingRegistrationRepository, "Atleta Log", email, PASSWORD);
        athleteId = userRepository.findByEmail(email).orElseThrow().getId();
        UUID trainingId = SeriesTestSupport.openTraining(mockMvc, objectMapper, token, "IPSC");
        var created = mockMvc.perform(post("/api/series").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RegisterSeriesRequest(trainingId, null, null, null, null, null, null))))
                .andExpect(status().isCreated()).andReturn();
        seriesId = UUID.fromString(objectMapper.readTree(created.getResponse().getContentAsString()).get("data").get("id").asText());
        upload(TestImages.jpeg(64, 64)).andExpect(status().isCreated());
        SeriesPhoto photo = photoGateway.findAllBySeriesId(seriesId).get(0);
        photoId = photo.getId();
        photoFile = Path.of(storageProperties.localDir()).toAbsolutePath().normalize().resolve(photo.getStorageKey());
        seriesDir = photoFile.getParent();
    }

    @AfterEach
    void restorePermissionsAndCleanUp() throws Exception {
        Files.setPosixFilePermissions(seriesDir, PosixFilePermissions.fromString("rwxr-xr-x"));
        if (Files.exists(photoFile)) {
            Files.setPosixFilePermissions(photoFile, PosixFilePermissions.fromString("rw-r--r--"));
        }
        mockMvc.perform(post("/api/users/me/deletion").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(new DeleteAccountRequest(PASSWORD))));
    }

    private org.springframework.test.web.servlet.ResultActions upload(byte[] bytes) throws Exception {
        return mockMvc.perform(multipart("/api/series/" + seriesId + "/photos")
                .file(new MockMultipartFile("file", "meu-alvo-secreto.jpg", "image/jpeg", bytes))
                .header("Authorization", "Bearer " + token));
    }

    private void assertLogIsClean(String logged, String expectedMessage) {
        assertThat(logged).contains(expectedMessage);
        assertThat(logged)
                .doesNotContain(athleteId.toString()).doesNotContain(seriesId.toString()).doesNotContain(photoId.toString())
                .doesNotContain("athletes/").doesNotContain("shottrack-uploads").doesNotContain(".part").doesNotContain(".tmp-")
                .doesNotContain("meu-alvo-secreto").doesNotContain(".thumb")
                .doesNotContain("java.nio.file").doesNotContain("UncheckedIOException").doesNotContain("Caused by");
    }

    @Test
    void shouldLogOnlyTheOperationAndTheCauseClassWhenWritingFails(CapturedOutput output) throws Exception {
        Files.setPosixFilePermissions(seriesDir, PosixFilePermissions.fromString("r-xr-xr-x"));
        Assumptions.assumeFalse(Files.isWritable(seriesDir), "rodando como root: a permissão é ignorada");
        int offset = output.getAll().length();

        upload(TestImages.jpeg(64, 64)).andExpect(status().isInternalServerError());

        assertLogIsClean(output.getAll().substring(offset), "Falha de armazenamento (gravar): AccessDeniedException");
    }

    @Test
    void shouldLogOnlyTheOperationAndTheCauseClassWhenReadingFails(CapturedOutput output) throws Exception {
        Files.setPosixFilePermissions(photoFile, PosixFilePermissions.fromString("---------"));
        Assumptions.assumeFalse(Files.isReadable(photoFile), "rodando como root: a permissão é ignorada");
        int offset = output.getAll().length();

        mockMvc.perform(get("/api/series/" + seriesId + "/photos/" + photoId + "/content").header("Authorization", "Bearer " + token))
                .andExpect(status().isInternalServerError());

        assertLogIsClean(output.getAll().substring(offset), "Falha de armazenamento (ler): AccessDeniedException");
    }

    @Test
    void shouldLogOnlyTheOperationAndTheCauseClassWhenDeletingFails(CapturedOutput output) throws Exception {
        Files.setPosixFilePermissions(seriesDir, PosixFilePermissions.fromString("r-xr-xr-x"));
        Assumptions.assumeFalse(Files.isWritable(seriesDir), "rodando como root: a permissão é ignorada");
        int offset = output.getAll().length();

        mockMvc.perform(delete("/api/series/" + seriesId + "/photos/" + photoId).header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        assertThat(photoGateway.findById(photoId)).as("a linha some mesmo sem apagar o arquivo").isEmpty();
        assertLogIsClean(output.getAll().substring(offset), "Falha de armazenamento (apagar): AccessDeniedException");
    }

    @Test
    void shouldNotNameThePhotoWhenTheStoredFileIsMissing(CapturedOutput output) throws Exception {
        Files.delete(photoFile);
        int offset = output.getAll().length();

        mockMvc.perform(get("/api/series/" + seriesId + "/photos/" + photoId + "/content").header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());

        assertLogIsClean(output.getAll().substring(offset), "Arquivo de foto ausente do armazenamento");
    }
}
