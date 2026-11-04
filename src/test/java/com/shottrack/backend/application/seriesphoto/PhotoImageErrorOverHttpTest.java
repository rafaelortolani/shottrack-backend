package com.shottrack.backend.application.seriesphoto;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shottrack.backend.application.series.dto.RegisterSeriesRequest;
import com.shottrack.backend.application.user.dto.DeleteAccountRequest;
import com.shottrack.backend.application.user.gateway.repository.PendingRegistrationRepository;
import com.shottrack.backend.support.SeriesTestSupport;
import com.shottrack.backend.support.TestImages;
import com.shottrack.backend.support.TestUsers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * O 401 no lugar do 404 só aparecia no container de verdade (dispatch de erro do Tomcat + Spring Security), que o MockMvc
 * não reproduz. Aqui há um servidor HTTP real e um cliente HTTP real, com `Accept: image/jpeg` como o de uma tag img.
 * NÃO é @Transactional (o servidor roda em outra thread); os dados saem na exclusão da conta.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ExtendWith(OutputCaptureExtension.class)
class PhotoImageErrorOverHttpTest {

    private static final String PASSWORD = "senha12345";

    @LocalServerPort
    private int port;
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private PendingRegistrationRepository pendingRegistrationRepository;

    private String token;

    @AfterEach
    void cleanUp() throws Exception {
        if (token != null) {
            mockMvc.perform(post("/api/users/me/deletion").header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(new DeleteAccountRequest(PASSWORD))));
        }
    }

    private HttpResponse<String> get(String path, String bearer, String accept) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).GET();
        if (bearer != null) {
            request.header("Authorization", "Bearer " + bearer);
        }
        if (accept != null) {
            request.header("Accept", accept);
        }
        return HttpClient.newHttpClient().send(request.build(), HttpResponse.BodyHandlers.ofString());
    }

    @Test
    void shouldAnswer404AndNot401ForAnImageRequestOverRealHttp(CapturedOutput output) throws Exception {
        token = TestUsers.registerAndLogin(mockMvc, objectMapper, pendingRegistrationRepository, "Atleta Http",
                "img.http." + UUID.randomUUID() + "@shottrack.com", PASSWORD);
        UUID training = SeriesTestSupport.openTraining(mockMvc, objectMapper, token, "IPSC");
        var created = mockMvc.perform(post("/api/series").header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RegisterSeriesRequest(training, null, null, null, null, null, null))))
                .andExpect(status().isCreated()).andReturn();
        UUID seriesId = UUID.fromString(objectMapper.readTree(created.getResponse().getContentAsString()).get("data").get("id").asText());
        var uploaded = mockMvc.perform(multipart("/api/series/" + seriesId + "/photos")
                        .file(new MockMultipartFile("file", "a.jpg", "image/jpeg", TestImages.jpeg(64, 64)))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated()).andReturn();
        String photoId = objectMapper.readTree(uploaded.getResponse().getContentAsString()).get("data").get("id").asText();
        int offset = output.getAll().length();

        for (String kind : new String[]{"content", "thumbnail"}) {
            for (String accept : new String[]{"image/jpeg", "*/*", null, "image/jpeg,application/json;q=0.9"}) {
                HttpResponse<String> missing = get("/api/series/" + seriesId + "/photos/" + UUID.randomUUID() + "/" + kind, token, accept);
                assertThat(missing.statusCode()).as("%s, Accept %s", kind, accept).isEqualTo(404);
                assertThat(missing.headers().firstValue("Content-Type").orElse("")).startsWith("application/json");
                assertThat(objectMapper.readTree(missing.body()).get("error").get("code").asText()).isEqualTo("PHOTO_NOT_FOUND");

                HttpResponse<String> noToken = get("/api/series/" + seriesId + "/photos/" + photoId + "/" + kind, null, accept);
                assertThat(noToken.statusCode()).isEqualTo(401);
                assertThat(noToken.headers().firstValue("Content-Type")).as("o 401 também é JSON em UTF-8")
                        .hasValueSatisfying(type -> assertThat(type).isEqualToIgnoringCase("application/json;charset=UTF-8"));
                assertThat(objectMapper.readTree(noToken.body()).get("error").get("code").asText()).isEqualTo("UNAUTHORIZED");

                HttpResponse<String> ok = get("/api/series/" + seriesId + "/photos/" + photoId + "/" + kind, token, accept);
                assertThat(ok.statusCode()).isEqualTo(200);
                assertThat(ok.headers().firstValue("Content-Type")).contains("image/jpeg");
            }
        }

        String logged = output.getAll().substring(offset);
        assertThat(logged).doesNotContain(" ERROR ").doesNotContain("\n\tat ");
    }
}
