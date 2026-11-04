package com.shottrack.backend.application.seriesphoto;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shottrack.backend.application.series.dto.RegisterSeriesRequest;
import com.shottrack.backend.application.user.gateway.repository.PendingRegistrationRepository;
import com.shottrack.backend.support.SeriesTestSupport;
import com.shottrack.backend.support.TestImages;
import com.shottrack.backend.support.TestUsers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * ADR-0026 / UC61: as rotas de imagem respondem o erro de negócio (404) e o 401 SEMPRE com o envelope JSON, qualquer que
 * seja o Accept — inclusive `Accept: image/jpeg` (o que o navegador manda para uma tag img). Antes, com esse Accept a
 * escrita do erro falhava, o erro caía no dispatch de erro do container e virava 401 (com ERROR e stack no log).
 * Erro esperado nunca loga em ERROR nem com stack trace.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ExtendWith(OutputCaptureExtension.class)
class PhotoImageErrorResponseTest {

    private static final String PASSWORD = "senha12345";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private PendingRegistrationRepository pendingRegistrationRepository;

    private String tokenA;
    private String tokenB;
    private UUID seriesOne;
    private UUID seriesTwo;
    private UUID photoId;

    @BeforeEach
    void prepare() throws Exception {
        tokenA = TestUsers.registerAndLogin(mockMvc, objectMapper, pendingRegistrationRepository, "Atleta A",
                "img.a." + UUID.randomUUID() + "@shottrack.com", PASSWORD);
        tokenB = TestUsers.registerAndLogin(mockMvc, objectMapper, pendingRegistrationRepository, "Atleta B",
                "img.b." + UUID.randomUUID() + "@shottrack.com", PASSWORD);
        UUID training = SeriesTestSupport.openTraining(mockMvc, objectMapper, tokenA, "IPSC");
        seriesOne = registerSeries(tokenA, training);
        seriesTwo = registerSeries(tokenA, training);
        MvcResult uploaded = mockMvc.perform(multipart("/api/series/" + seriesOne + "/photos")
                        .file(new MockMultipartFile("file", "a.jpg", "image/jpeg", TestImages.jpeg(64, 64)))
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isCreated()).andReturn();
        photoId = UUID.fromString(objectMapper.readTree(uploaded.getResponse().getContentAsString()).get("data").get("id").asText());
    }

    private UUID registerSeries(String bearer, UUID training) throws Exception {
        var result = mockMvc.perform(post("/api/series").header("Authorization", "Bearer " + bearer).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RegisterSeriesRequest(training, null, null, null, null, null, null))))
                .andExpect(status().isCreated()).andReturn();
        return UUID.fromString(objectMapper.readTree(result.getResponse().getContentAsString()).get("data").get("id").asText());
    }

    private MvcResult fetch(String path, String bearer, String accept) throws Exception {
        MockHttpServletRequestBuilder request = get(path);
        if (bearer != null) {
            request.header("Authorization", "Bearer " + bearer);
        }
        if (!accept.isEmpty()) {
            request.header("Accept", accept);
        }
        return mockMvc.perform(request).andReturn();
    }

    private void assertError(MvcResult result, int status, String code) throws Exception {
        assertThat(result.getResponse().getStatus()).isEqualTo(status);
        assertThat(result.getResponse().getContentType()).startsWith("application/json");
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(body.get("data").isNull()).isTrue();
        assertThat(body.get("error").get("code").asText()).isEqualTo(code);
        assertThat(body.get("error").get("message").asText()).isNotBlank();
    }

    @ParameterizedTest(name = "Accept: \"{0}\"")
    @ValueSource(strings = {"image/jpeg", "*/*", "", "image/jpeg,application/json;q=0.9"})
    void shouldAnswerTheJsonErrorEnvelopeForContentAndThumbnailWhateverTheAcceptHeaderIs(String accept, CapturedOutput output) throws Exception {
        int logOffset = output.getAll().length();
        for (String kind : new String[]{"content", "thumbnail"}) {
            String own = "/api/series/" + seriesOne + "/photos/" + photoId + "/" + kind;

            assertError(fetch("/api/series/" + seriesOne + "/photos/" + UUID.randomUUID() + "/" + kind, tokenA, accept), 404, "PHOTO_NOT_FOUND");
            assertError(fetch(own, tokenB, accept), 404, "SERIES_NOT_FOUND");
            assertError(fetch("/api/series/" + seriesTwo + "/photos/" + photoId + "/" + kind, tokenA, accept), 404, "PHOTO_NOT_FOUND");
            assertError(fetch(own, null, accept), 401, "UNAUTHORIZED");

            MvcResult ok = fetch(own, tokenA, accept);
            assertThat(ok.getResponse().getStatus()).isEqualTo(200);
            assertThat(ok.getResponse().getContentType()).isEqualTo("image/jpeg");
            assertThat(ok.getResponse().getContentAsByteArray()).isNotEmpty();
        }

        String logged = output.getAll().substring(logOffset);
        assertThat(logged).as("erro esperado não loga em ERROR").doesNotContain(" ERROR ");
        assertThat(logged).as("nem com stack trace").doesNotContain("\n\tat ").doesNotContain("Exception");
    }

    @ParameterizedTest(name = "Accept: \"{0}\"")
    @ValueSource(strings = {"image/jpeg", "*/*", "image/jpeg,application/json;q=0.9"})
    void shouldKeepTheErrorsInUtf8JsonForTheAuthenticationFailureToo(String accept) throws Exception {
        MvcResult result = fetch("/api/series/" + seriesOne + "/photos/" + photoId + "/content", null, accept);

        assertThat(result.getResponse().getContentType()).startsWith("application/json");
        assertThat(result.getResponse().getCharacterEncoding()).isEqualToIgnoringCase("UTF-8");
    }
}
