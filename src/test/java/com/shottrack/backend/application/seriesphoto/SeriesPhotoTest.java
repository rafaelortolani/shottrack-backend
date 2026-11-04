package com.shottrack.backend.application.seriesphoto;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shottrack.backend.application.seriesphoto.storage.StoragePort;
import com.shottrack.backend.application.series.dto.RegisterSeriesRequest;
import com.shottrack.backend.application.user.gateway.repository.PendingRegistrationRepository;
import com.shottrack.backend.support.SeriesTestSupport;
import com.shottrack.backend.support.TestImages;
import com.shottrack.backend.support.TestUsers;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
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
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * UC60/UC61/UC62 (ADR-0026): anexar, consultar e remover a foto do alvo da série. O arquivo guardado é sempre o
 * JPEG reprocessado (sem metadados); a resposta nunca expõe caminho nem chave de armazenamento.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ExtendWith(OutputCaptureExtension.class)
@TestPropertySource(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
class SeriesPhotoTest {

    private static final String PASSWORD = "senha12345";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private PendingRegistrationRepository pendingRegistrationRepository;
    @Autowired
    private StoragePort storage;
    @Autowired
    private EntityManagerFactory entityManagerFactory;

    private String token;
    private UUID trainingId;
    private UUID seriesId;

    @BeforeEach
    void prepare() throws Exception {
        token = TestUsers.registerAndLogin(mockMvc, objectMapper, pendingRegistrationRepository, "Atleta Foto",
                "foto." + UUID.randomUUID() + "@shottrack.com", PASSWORD);
        trainingId = SeriesTestSupport.openTraining(mockMvc, objectMapper, token, "IPSC");
        seriesId = registerSeries(token, trainingId);
    }

    private UUID registerSeries(String bearer, UUID training) throws Exception {
        var result = mockMvc.perform(post("/api/series").header("Authorization", "Bearer " + bearer)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new RegisterSeriesRequest(training, null, null, null, null, null, null))))
                .andExpect(status().isCreated()).andReturn();
        return UUID.fromString(objectMapper.readTree(result.getResponse().getContentAsString()).get("data").get("id").asText());
    }

    private ResultActions upload(String bearer, UUID series, String filename, String contentType, byte[] bytes) throws Exception {
        MockMultipartHttpServletRequestBuilder request = multipart("/api/series/" + series + "/photos")
                .file(new MockMultipartFile("file", filename, contentType, bytes));
        if (bearer != null) {
            request.header("Authorization", "Bearer " + bearer);
        }
        return mockMvc.perform(request);
    }

    private JsonNode attach(byte[] bytes) throws Exception {
        var result = upload(token, seriesId, "alvo.jpg", "image/jpeg", bytes).andExpect(status().isCreated()).andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("data");
    }

    private String errorCode(ResultActions actions) throws Exception {
        return objectMapper.readTree(actions.andReturn().getResponse().getContentAsString()).get("error").get("code").asText();
    }

    // ---- UC60: fluxo principal ----

    @Test
    void shouldAttachJpegPngAndStoreOnlyReprocessedJpegWithoutMetadata() throws Exception {
        JsonNode jpeg = attach(TestImages.jpegWithExif(800, 600, 1));
        JsonNode png = attach(TestImages.png(500, 500));

        assertThat(jpeg.get("position").asInt()).isEqualTo(1);
        assertThat(png.get("position").asInt()).isEqualTo(2);
        assertThat(jpeg.get("widthPx").asInt()).isEqualTo(800);
        assertThat(jpeg.get("heightPx").asInt()).isEqualTo(600);
        assertThat(jpeg.get("sizeBytes").asLong()).isPositive();
        assertThat(jpeg.has("storageKey")).isFalse();
        assertThat(jpeg.has("thumbKey")).isFalse();

        byte[] original = mockMvc.perform(get("/api/series/" + seriesId + "/photos/" + jpeg.get("id").asText() + "/content")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray();
        assertThat(original).hasSize(jpeg.get("sizeBytes").asInt());
        assertThat(TestImages.contains(original, TestImages.SECRET_GPS)).isFalse();
        assertThat(TestImages.metadataMarkers(original)).isEmpty();
        assertThat(original[0] & 0xFF).isEqualTo(0xFF);
        assertThat(original[1] & 0xFF).isEqualTo(0xD8);
    }

    @Test
    void shouldStoreTheImageInTheCorrectOrientationWhenTheJpegIsRotatedByExif() throws Exception {
        JsonNode rotated = attach(TestImages.jpegWithExif(60, 40, 6));
        byte[] bytes = mockMvc.perform(get("/api/series/" + seriesId + "/photos/" + rotated.get("id").asText() + "/content")
                        .header("Authorization", "Bearer " + token))
                .andReturn().getResponse().getContentAsByteArray();

        var image = TestImages.read(bytes);

        assertThat(rotated.get("widthPx").asInt()).isEqualTo(40);
        assertThat(rotated.get("heightPx").asInt()).isEqualTo(60);
        assertThat(image.getWidth()).isEqualTo(40);
        assertThat(image.getHeight()).isEqualTo(60);
        assertThat(new java.awt.Color(image.getRGB(38, 2)).getRed()).isGreaterThan(150);
    }

    @Test
    void shouldAttachWhileTheTrainingIsClosed() throws Exception {
        mockMvc.perform(post("/api/trainings/" + trainingId + "/closure").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        upload(token, seriesId, "depois.jpg", "image/jpeg", TestImages.jpeg(100, 100)).andExpect(status().isCreated());
    }

    // ---- UC60: fluxos alternativos ----

    @Test
    void shouldRequireAuthenticationOnEveryEndpoint() throws Exception {
        UUID photo = UUID.randomUUID();
        String base = "/api/series/" + seriesId + "/photos";

        upload(null, seriesId, "a.jpg", "image/jpeg", TestImages.jpeg(10, 10)).andExpect(status().isUnauthorized());
        mockMvc.perform(get(base)).andExpect(status().isUnauthorized());
        mockMvc.perform(get(base + "/" + photo + "/content")).andExpect(status().isUnauthorized());
        mockMvc.perform(get(base + "/" + photo + "/thumbnail")).andExpect(status().isUnauthorized());
        mockMvc.perform(delete(base + "/" + photo)).andExpect(status().isUnauthorized());
        mockMvc.perform(get(base).header("Authorization", "Bearer token.invalido.aqui")).andExpect(status().isUnauthorized());
    }

    @Test
    void shouldAnswerSeriesNotFoundForAnUnknownSeries() throws Exception {
        ResultActions result = upload(token, UUID.randomUUID(), "a.jpg", "image/jpeg", TestImages.jpeg(10, 10)).andExpect(status().isNotFound());

        assertThat(errorCode(result)).isEqualTo("SERIES_NOT_FOUND");
    }

    @Test
    void shouldRejectARequestWithoutTheFileField() throws Exception {
        mockMvc.perform(multipart("/api/series/" + seriesId + "/photos").header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
        mockMvc.perform(multipart("/api/series/" + seriesId + "/photos")
                        .file(new MockMultipartFile("outro", "a.jpg", "image/jpeg", TestImages.jpeg(10, 10)))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    void shouldRejectAnEmptyFile() throws Exception {
        upload(token, seriesId, "vazio.jpg", "image/jpeg", new byte[0])
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    void shouldRejectAFileAbove10Mb() throws Exception {
        upload(token, seriesId, "grande.jpg", "image/jpeg", new byte[10 * 1024 * 1024 + 1])
                .andExpect(status().isPayloadTooLarge()).andExpect(jsonPath("$.error.code").value("PHOTO_TOO_LARGE"));
    }

    @Test
    void shouldRejectOtherFormatsEvenWhenNamedJpgOrSentAsImageJpeg() throws Exception {
        upload(token, seriesId, "alvo.jpg", "image/jpeg", TestImages.gif(30, 30))
                .andExpect(status().isUnsupportedMediaType()).andExpect(jsonPath("$.error.code").value("PHOTO_UNSUPPORTED_TYPE"));
        upload(token, seriesId, "alvo.jpg", "image/jpeg", "%PDF-1.7 documento".getBytes())
                .andExpect(status().isUnsupportedMediaType()).andExpect(jsonPath("$.error.code").value("PHOTO_UNSUPPORTED_TYPE"));
        upload(token, seriesId, "script.jpg", "image/jpeg", "<?php system($_GET['c']); ?>".getBytes())
                .andExpect(status().isUnsupportedMediaType());
        upload(token, seriesId, "alvo.webp", "image/webp", TestImages.fakeWebp())
                .andExpect(status().isUnsupportedMediaType());
    }

    @Test
    void shouldAcceptARealImageIgnoringAWrongNameAndContentType() throws Exception {
        upload(token, seriesId, "documento.pdf", "application/pdf", TestImages.png(50, 50)).andExpect(status().isCreated());
    }

    @Test
    void shouldRejectCorruptedImages() throws Exception {
        byte[] jpeg = TestImages.jpeg(400, 300);
        upload(token, seriesId, "x.jpg", "image/jpeg", java.util.Arrays.copyOf(jpeg, jpeg.length / 2))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error.code").value("PHOTO_INVALID_IMAGE"));
        upload(token, seriesId, "x.png", "image/png", java.util.Arrays.copyOf(TestImages.png(400, 300), 40))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error.code").value("PHOTO_INVALID_IMAGE"));
    }

    @Test
    void shouldRejectMoreThan40MegapixelsWithoutDecodingIt() throws Exception {
        upload(token, seriesId, "enorme.png", "image/png", TestImages.pngHeaderOnly(60_000, 60_000))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error.code").value("PHOTO_DIMENSIONS_TOO_LARGE"));
    }

    @Test
    void shouldRefuseASixthPhotoInTheSameSeries() throws Exception {
        for (int i = 0; i < 5; i++) {
            attach(TestImages.jpeg(64, 64));
        }

        ResultActions sixth = upload(token, seriesId, "6.jpg", "image/jpeg", TestImages.jpeg(64, 64)).andExpect(status().isConflict());

        assertThat(errorCode(sixth)).isEqualTo("PHOTO_LIMIT_PER_SERIES_REACHED");
        mockMvc.perform(get("/api/series/" + seriesId + "/photos").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.data.length()").value(5));
    }

    @Test
    void shouldKeepAMaliciousFileNameOutOfTheStoragePathAndTheLog(CapturedOutput output) throws Exception {
        String malicious = "../../../etc/passwd'; DROP TABLE series_photos;--<script>.jpg";

        upload(token, seriesId, malicious, "image/jpeg", TestImages.jpeg(64, 64)).andExpect(status().isCreated());

        var stored = storage.list().stream().map(o -> o.key()).filter(k -> k.contains(seriesId.toString())).toList();
        assertThat(stored).hasSize(2).allMatch(k -> k.matches("athletes/[0-9a-f-]{36}/series/" + seriesId + "/[0-9a-f-]{36}(\\.thumb)?\\.jpg"));
        assertThat(output.getAll()).doesNotContain("passwd").doesNotContain("DROP TABLE");
    }

    // ---- UC61 ----

    @Test
    void shouldListContentAndThumbnailWithTheRequiredHeaders() throws Exception {
        JsonNode photo = attach(TestImages.jpeg(1200, 900));
        String base = "/api/series/" + seriesId + "/photos";

        mockMvc.perform(get(base).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].id").value(photo.get("id").asText()))
                .andExpect(jsonPath("$.data[0].widthPx").value(1200))
                .andExpect(jsonPath("$.data[0].heightPx").value(900));

        for (String kind : new String[]{"content", "thumbnail"}) {
            byte[] bytes = mockMvc.perform(get(base + "/" + photo.get("id").asText() + "/" + kind).header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.IMAGE_JPEG))
                    .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                    .andExpect(header().string("Cache-Control", "no-store, private"))
                    .andReturn().getResponse().getContentAsByteArray();
            assertThat(TestImages.read(bytes)).isNotNull();
        }
        var thumb = mockMvc.perform(get(base + "/" + photo.get("id").asText() + "/thumbnail").header("Authorization", "Bearer " + token))
                .andReturn().getResponse().getContentAsByteArray();
        assertThat(TestImages.read(thumb).getWidth()).isEqualTo(400);
    }

    @Test
    void shouldReturnAnEmptyListForASeriesWithoutPhotos() throws Exception {
        mockMvc.perform(get("/api/series/" + seriesId + "/photos").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    void shouldAnswerPhotoNotFoundForUnknownPhotoAndSeriesNotFoundForUnknownSeries() throws Exception {
        String base = "/api/series/" + seriesId + "/photos/" + UUID.randomUUID();
        for (String suffix : new String[]{"/content", "/thumbnail"}) {
            mockMvc.perform(get(base + suffix).header("Authorization", "Bearer " + token))
                    .andExpect(status().isNotFound()).andExpect(jsonPath("$.error.code").value("PHOTO_NOT_FOUND"));
        }
        mockMvc.perform(get("/api/series/" + UUID.randomUUID() + "/photos").header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.error.code").value("SERIES_NOT_FOUND"));
    }

    @Test
    void shouldAnswerPhotoNotFoundForAPhotoOfAnotherSeriesOfTheSameAthlete() throws Exception {
        UUID otherSeries = registerSeries(token, trainingId);
        JsonNode photo = attach(TestImages.jpeg(64, 64));

        for (String suffix : new String[]{"/content", "/thumbnail"}) {
            mockMvc.perform(get("/api/series/" + otherSeries + "/photos/" + photo.get("id").asText() + suffix)
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isNotFound()).andExpect(jsonPath("$.error.code").value("PHOTO_NOT_FOUND"));
        }
        mockMvc.perform(delete("/api/series/" + otherSeries + "/photos/" + photo.get("id").asText()).header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.error.code").value("PHOTO_NOT_FOUND"));
    }

    @Test
    void shouldAnswerPhotoNotFoundAndLogAnErrorWhenTheFileIsGone(CapturedOutput output) throws Exception {
        JsonNode photo = attach(TestImages.jpeg(64, 64));
        storage.list().stream().filter(o -> o.key().contains(photo.get("id").asText())).forEach(o -> storage.delete(o.key()));

        mockMvc.perform(get("/api/series/" + seriesId + "/photos/" + photo.get("id").asText() + "/content").header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.error.code").value("PHOTO_NOT_FOUND"));

        assertThat(output.getAll()).contains("Arquivo de foto ausente do armazenamento (photoId=" + photo.get("id").asText() + ")");
    }

    @Test
    void shouldTranslateTheErrorMessagesToEnglish() throws Exception {
        mockMvc.perform(get("/api/series/" + seriesId + "/photos/" + UUID.randomUUID() + "/content")
                        .header("Authorization", "Bearer " + token).header("Accept-Language", "en"))
                .andExpect(jsonPath("$.error.message").value("Photo not found"));
        upload(token, seriesId, "a.jpg", "image/jpeg", TestImages.gif(5, 5));
        mockMvc.perform(multipart("/api/series/" + seriesId + "/photos")
                        .file(new MockMultipartFile("file", "a.jpg", "image/jpeg", TestImages.gif(5, 5)))
                        .header("Authorization", "Bearer " + token).header("Accept-Language", "en"))
                .andExpect(jsonPath("$.error.message").value("Unsupported image type (use JPEG or PNG)"));
    }

    // ---- UC62 ----

    @Test
    void shouldRemoveThePhotoRowAndItsFiles() throws Exception {
        JsonNode photo = attach(TestImages.jpeg(64, 64));
        String id = photo.get("id").asText();
        assertThat(storage.list().stream().filter(o -> o.key().contains(id))).hasSize(2);

        mockMvc.perform(delete("/api/series/" + seriesId + "/photos/" + id).header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/series/" + seriesId + "/photos").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.data.length()").value(0));
        mockMvc.perform(get("/api/series/" + seriesId + "/photos/" + id + "/content").header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldAnswerPhotoNotFoundWhenRemovingAPhotoThatDoesNotExist() throws Exception {
        mockMvc.perform(delete("/api/series/" + seriesId + "/photos/" + UUID.randomUUID()).header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.error.code").value("PHOTO_NOT_FOUND"));
        mockMvc.perform(delete("/api/series/" + UUID.randomUUID() + "/photos/" + UUID.randomUUID()).header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.error.code").value("SERIES_NOT_FOUND"));
    }

    // ---- UC37: photoCount sem N+1 ----

    @Test
    void shouldReportThePhotoCountPerSeriesWithASingleGroupedQuery() throws Exception {
        UUID second = registerSeries(token, trainingId);
        UUID third = registerSeries(token, trainingId);
        attach(TestImages.jpeg(64, 64));
        attach(TestImages.jpeg(64, 64));
        upload(token, second, "b.jpg", "image/jpeg", TestImages.jpeg(64, 64)).andExpect(status().isCreated());

        SessionFactory sessionFactory = entityManagerFactory.unwrap(SessionFactory.class);
        sessionFactory.getStatistics().clear();
        var result = mockMvc.perform(get("/api/trainings/" + trainingId + "/series").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andReturn();

        JsonNode list = objectMapper.readTree(result.getResponse().getContentAsString()).get("data");
        assertThat(list).hasSize(3);
        for (JsonNode series : list) {
            String id = series.get("id").asText();
            long expected = id.equals(seriesId.toString()) ? 2 : id.equals(second.toString()) ? 1 : 0;
            assertThat(series.get("photoCount").asLong()).as("photoCount de %s", id).isEqualTo(expected);
        }
        assertThat(third).isNotNull();
        long photoQueries = java.util.Arrays.stream(sessionFactory.getStatistics().getQueries()).filter(q -> q.contains("SeriesPhoto")).count();
        assertThat(photoQueries).as("uma única consulta de contagem para todas as séries").isEqualTo(1);
    }

    @Test
    void shouldReportPhotoCountInTheSeriesResponsesAndDropItAfterRemoval() throws Exception {
        JsonNode photo = attach(TestImages.jpeg(64, 64));

        mockMvc.perform(post("/api/series").header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RegisterSeriesRequest(trainingId, null, null, null, null, null, null))))
                .andExpect(jsonPath("$.data.photoCount").value(0));
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch("/api/series/" + seriesId)
                        .header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON).content("{\"notes\":\"x\"}"))
                .andExpect(jsonPath("$.data.photoCount").value(1));

        mockMvc.perform(delete("/api/series/" + seriesId + "/photos/" + photo.get("id").asText()).header("Authorization", "Bearer " + token));

        mockMvc.perform(get("/api/trainings/" + trainingId + "/series").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.data[?(@.id=='" + seriesId + "')].photoCount").value(0));
    }
}
