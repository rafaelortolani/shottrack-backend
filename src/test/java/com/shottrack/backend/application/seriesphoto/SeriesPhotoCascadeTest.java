package com.shottrack.backend.application.seriesphoto;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shottrack.backend.application.series.dto.RegisterSeriesRequest;
import com.shottrack.backend.application.seriesphoto.gateway.repository.SeriesPhotoRepository;
import com.shottrack.backend.application.seriesphoto.storage.StoragePort;
import com.shottrack.backend.application.user.dto.DeleteAccountRequest;
import com.shottrack.backend.application.user.gateway.repository.PendingRegistrationRepository;
import com.shottrack.backend.application.user.gateway.repository.UserRepository;
import com.shottrack.backend.support.SeriesTestSupport;
import com.shottrack.backend.support.TestImages;
import com.shottrack.backend.support.TestUsers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.io.UncheckedIOException;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * ADR-0026 / UC41, UC43, UC44, UC52: excluir série, treino, visita ou conta apaga as fotos — linhas em cascata e
 * arquivos DEPOIS do commit. NÃO é @Transactional: o commit precisa acontecer de verdade para o gancho pós-commit
 * rodar. Os dados criados são removidos no fim (exclusão das duas contas).
 */
@SpringBootTest
@AutoConfigureMockMvc
class SeriesPhotoCascadeTest {

    private static final String PASSWORD = "senha12345";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private PendingRegistrationRepository pendingRegistrationRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private SeriesPhotoRepository photoRepository;
    @SpyBean
    private StoragePort storage;

    private String emailA;
    private String emailB;
    private String tokenA;
    private String tokenB;
    private UUID athleteA;
    private UUID athleteB;
    private UUID visitA;
    private UUID trainingA;
    private UUID seriesA1;
    private UUID seriesA2;
    private UUID seriesB;

    @BeforeEach
    void prepare() throws Exception {
        String suffix = UUID.randomUUID().toString();
        emailA = "foto.cascata.a." + suffix + "@shottrack.com";
        emailB = "foto.cascata.b." + suffix + "@shottrack.com";
        tokenA = TestUsers.registerAndLogin(mockMvc, objectMapper, pendingRegistrationRepository, "Atleta A", emailA, PASSWORD);
        tokenB = TestUsers.registerAndLogin(mockMvc, objectMapper, pendingRegistrationRepository, "Atleta B", emailB, PASSWORD);
        athleteA = userRepository.findByEmail(emailA).orElseThrow().getId();
        athleteB = userRepository.findByEmail(emailB).orElseThrow().getId();

        visitA = SeriesTestSupport.openVisit(mockMvc, objectMapper, tokenA);
        trainingA = SeriesTestSupport.openTrainingInVisit(mockMvc, objectMapper, tokenA, visitA, "IPSC");
        seriesA1 = registerSeries(tokenA, trainingA);
        seriesA2 = registerSeries(tokenA, trainingA);
        seriesB = registerSeries(tokenB, SeriesTestSupport.openTraining(mockMvc, objectMapper, tokenB, "IPSC"));
        photo(tokenA, seriesA1);
        photo(tokenA, seriesA1);
        photo(tokenA, seriesA2);
        photo(tokenB, seriesB);
        Mockito.reset(storage);
    }

    @AfterEach
    void cleanUp() throws Exception {
        Mockito.reset(storage);
        for (String[] account : new String[][]{{emailA, tokenA}, {emailB, tokenB}}) {
            if (userRepository.findByEmail(account[0]).isPresent()) {
                mockMvc.perform(post("/api/users/me/deletion").header("Authorization", "Bearer " + account[1])
                        .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(new DeleteAccountRequest(PASSWORD))));
            }
        }
    }

    private UUID registerSeries(String token, UUID training) throws Exception {
        var result = mockMvc.perform(post("/api/series").header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RegisterSeriesRequest(training, null, null, null, null, null, null))))
                .andExpect(status().isCreated()).andReturn();
        return UUID.fromString(objectMapper.readTree(result.getResponse().getContentAsString()).get("data").get("id").asText());
    }

    private void photo(String token, UUID series) throws Exception {
        mockMvc.perform(multipart("/api/series/" + series + "/photos")
                        .file(new MockMultipartFile("file", "a.jpg", "image/jpeg", TestImages.jpeg(60, 40)))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated());
    }

    private List<String> filesOf(UUID athleteId) {
        return storage.list().stream().map(o -> o.key()).filter(k -> k.startsWith("athletes/" + athleteId + "/")).toList();
    }

    private List<String> filesOfSeries(UUID athleteId, UUID series) {
        return filesOf(athleteId).stream().filter(k -> k.contains("/series/" + series + "/")).toList();
    }

    @Test
    void shouldCreateTwoFilesPerPhotoAsTheStartingPoint() {
        assertThat(filesOf(athleteA)).hasSize(6);
        assertThat(photoRepository.countByAthleteId(athleteA)).isEqualTo(3);
        assertThat(filesOf(athleteB)).hasSize(2);
    }

    @Test
    void shouldDeleteRowsAndFilesWhenTheSeriesIsDeleted() throws Exception {
        mockMvc.perform(delete("/api/series/" + seriesA1).header("Authorization", "Bearer " + tokenA)).andExpect(status().isOk());

        assertThat(photoRepository.countBySeriesId(seriesA1)).isZero();
        assertThat(filesOfSeries(athleteA, seriesA1)).isEmpty();
        assertThat(filesOfSeries(athleteA, seriesA2)).as("a outra série não é tocada").hasSize(2);
        assertThat(filesOf(athleteB)).hasSize(2);
    }

    @Test
    void shouldDeleteRowsAndFilesWhenTheTrainingIsDeleted() throws Exception {
        mockMvc.perform(delete("/api/trainings/" + trainingA).header("Authorization", "Bearer " + tokenA)).andExpect(status().isOk());

        assertThat(photoRepository.countByAthleteId(athleteA)).isZero();
        assertThat(filesOf(athleteA)).isEmpty();
        assertThat(filesOf(athleteB)).hasSize(2);
    }

    @Test
    void shouldDeleteRowsAndFilesWhenTheVisitIsDeleted() throws Exception {
        mockMvc.perform(delete("/api/visits/" + visitA).header("Authorization", "Bearer " + tokenA)).andExpect(status().isOk());

        assertThat(photoRepository.countByAthleteId(athleteA)).isZero();
        assertThat(filesOf(athleteA)).isEmpty();
        assertThat(filesOf(athleteB)).hasSize(2);
    }

    @Test
    void shouldDeleteEveryRowAndFileOfTheAthleteWhenTheAccountIsDeletedAndLeaveOthersIntact() throws Exception {
        mockMvc.perform(post("/api/users/me/deletion").header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(new DeleteAccountRequest(PASSWORD))))
                .andExpect(status().isOk());

        assertThat(photoRepository.countByAthleteId(athleteA)).isZero();
        assertThat(filesOf(athleteA)).isEmpty();
        assertThat(photoRepository.countByAthleteId(athleteB)).isEqualTo(1);
        assertThat(filesOf(athleteB)).hasSize(2);
    }

    @Test
    void shouldKeepDeletingTheSeriesWhenAFileCannotBeDeleted() throws Exception {
        doThrow(new UncheckedIOException("sem permissão", new java.io.IOException("negado"))).when(storage).delete(any());

        mockMvc.perform(delete("/api/series/" + seriesA1).header("Authorization", "Bearer " + tokenA)).andExpect(status().isOk());

        assertThat(photoRepository.countBySeriesId(seriesA1)).as("a exclusão da série permanece").isZero();
        assertThat(filesOfSeries(athleteA, seriesA1)).as("os arquivos ficam para a rotina de órfãos").hasSize(4);
    }

    @Test
    void shouldKeepDeletingTheAccountWhenAFileCannotBeDeleted() throws Exception {
        doThrow(new UncheckedIOException("sem permissão", new java.io.IOException("negado"))).when(storage).delete(any());

        mockMvc.perform(post("/api/users/me/deletion").header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(new DeleteAccountRequest(PASSWORD))))
                .andExpect(status().isOk());

        assertThat(userRepository.findByEmail(emailA)).isEmpty();
        assertThat(photoRepository.countByAthleteId(athleteA)).isZero();
        // os arquivos ficam órfãos: a rotina semanal os remove
        Mockito.reset(storage);
        assertThat(filesOf(athleteA)).hasSize(6);
        filesOf(athleteA).forEach(storage::delete);
    }

    @Test
    void shouldKeepEveryFileWhenTheSeriesToDeleteDoesNotExist() throws Exception {
        mockMvc.perform(delete("/api/series/" + UUID.randomUUID()).header("Authorization", "Bearer " + tokenA)).andExpect(status().isNotFound());

        assertThat(filesOf(athleteA)).hasSize(6);
        assertThat(photoRepository.countByAthleteId(athleteA)).isEqualTo(3);
    }
}
