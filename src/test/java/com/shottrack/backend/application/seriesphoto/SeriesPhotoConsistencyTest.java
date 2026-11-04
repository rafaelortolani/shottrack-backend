package com.shottrack.backend.application.seriesphoto;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shottrack.backend.application.series.dto.RegisterSeriesRequest;
import com.shottrack.backend.application.seriesphoto.gateway.SeriesPhotoGateway;
import com.shottrack.backend.application.seriesphoto.model.SeriesPhoto;
import com.shottrack.backend.application.seriesphoto.storage.StoragePort;
import com.shottrack.backend.application.user.gateway.repository.PendingRegistrationRepository;
import com.shottrack.backend.support.SeriesTestSupport;
import com.shottrack.backend.support.TestImages;
import com.shottrack.backend.support.TestUsers;
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
import org.springframework.transaction.annotation.Transactional;

import java.io.UncheckedIOException;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.endsWith;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * UC60 (consistência) e UC62 (falha ao apagar): falha injetada no armazenamento não deixa registro; falha injetada no
 * banco não deixa arquivo órfão; falha ao apagar o arquivo não derruba a remoção.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SeriesPhotoConsistencyTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private PendingRegistrationRepository pendingRegistrationRepository;
    @SpyBean
    private StoragePort storage;
    @SpyBean
    private SeriesPhotoGateway photoGateway;

    private String token;
    private UUID seriesId;

    @BeforeEach
    void prepare() throws Exception {
        token = TestUsers.registerAndLogin(mockMvc, objectMapper, pendingRegistrationRepository, "Atleta Consistência",
                "foto.consistencia." + UUID.randomUUID() + "@shottrack.com", "senha12345");
        UUID training = SeriesTestSupport.openTraining(mockMvc, objectMapper, token, "IPSC");
        var result = mockMvc.perform(post("/api/series").header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RegisterSeriesRequest(training, null, null, null, null, null, null))))
                .andReturn();
        seriesId = UUID.fromString(objectMapper.readTree(result.getResponse().getContentAsString()).get("data").get("id").asText());
        Mockito.reset(storage, photoGateway);
    }

    private org.springframework.test.web.servlet.ResultActions upload() throws Exception {
        return mockMvc.perform(multipart("/api/series/" + seriesId + "/photos")
                .file(new MockMultipartFile("file", "a.jpg", "image/jpeg", TestImages.jpeg(80, 60)))
                .header("Authorization", "Bearer " + token));
    }

    private long storedFilesOfSeries() {
        return storage.list().stream().filter(o -> o.key().contains("/series/" + seriesId + "/")).count();
    }

    private void assertNoPhotoRow() throws Exception {
        mockMvc.perform(get("/api/series/" + seriesId + "/photos").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    void shouldNotCreateARowWhenWritingTheImageFileFails() throws Exception {
        doThrow(new UncheckedIOException("disco cheio", new java.io.IOException("sem espaço")))
                .when(storage).put(any(), any());

        upload().andExpect(status().isInternalServerError());

        assertNoPhotoRow();
        assertThat(storedFilesOfSeries()).isZero();
    }

    @Test
    void shouldNotCreateARowAndShouldCleanTheFirstFileWhenWritingTheThumbnailFails() throws Exception {
        doThrow(new UncheckedIOException("disco cheio", new java.io.IOException("sem espaço")))
                .when(storage).put(endsWith(".thumb.jpg"), any());

        upload().andExpect(status().isInternalServerError());

        assertNoPhotoRow();
        assertThat(storedFilesOfSeries()).as("a imagem principal já gravada é apagada").isZero();
    }

    @Test
    void shouldDeleteTheFilesWhenSavingTheRowFails() throws Exception {
        doThrow(new IllegalStateException("banco fora do ar")).when(photoGateway).save(any(SeriesPhoto.class));

        upload().andExpect(status().isInternalServerError());

        assertThat(storedFilesOfSeries()).as("nenhum arquivo órfão").isZero();
        Mockito.reset(photoGateway);
        assertNoPhotoRow();
    }

    @Test
    void shouldStillRemoveTheRowWhenDeletingTheFileFails() throws Exception {
        var created = upload().andExpect(status().isCreated()).andReturn();
        String photoId = objectMapper.readTree(created.getResponse().getContentAsString()).get("data").get("id").asText();
        doThrow(new UncheckedIOException("sem permissão", new java.io.IOException("negado"))).when(storage).delete(any());

        mockMvc.perform(delete("/api/series/" + seriesId + "/photos/" + photoId).header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        assertNoPhotoRow();
    }
}
