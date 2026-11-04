package com.shottrack.backend.common.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shottrack.backend.application.user.gateway.repository.PendingRegistrationRepository;
import com.shottrack.backend.support.TestUsers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * Toda resposta de erro é application/json (envelope padrão), qualquer que seja o Accept — não só nas rotas de imagem.
 * Cobre as rotas JSON comuns e a exportação de dados (download), que tem o mesmo risco.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ExtendWith(OutputCaptureExtension.class)
class ErrorResponseContentTypeTest {

    private static final String PASSWORD = "senha12345";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private PendingRegistrationRepository pendingRegistrationRepository;

    private String register() throws Exception {
        return TestUsers.registerAndLogin(mockMvc, objectMapper, pendingRegistrationRepository, "Atleta",
                "erro.ct." + UUID.randomUUID() + "@shottrack.com", PASSWORD);
    }

    private void assertJsonError(MvcResult result, int status, String code) throws Exception {
        assertThat(result.getResponse().getStatus()).isEqualTo(status);
        assertThat(result.getResponse().getContentType()).startsWith("application/json");
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(body.get("error").get("code").asText()).isEqualTo(code);
    }

    @Test
    void shouldAnswerBusinessErrorsAsJsonOnAnyJsonRouteWhenTheClientAsksForAnImage(CapturedOutput output) throws Exception {
        String token = register();
        int offset = output.getAll().length();

        assertJsonError(mockMvc.perform(get("/api/trainings/" + UUID.randomUUID() + "/series")
                .header("Authorization", "Bearer " + token).header("Accept", "image/jpeg")).andReturn(), 404, "TRAINING_NOT_FOUND");
        assertJsonError(mockMvc.perform(get("/api/inexistente").header("Authorization", "Bearer " + token)
                .header("Accept", "image/jpeg")).andReturn(), 404, "ENDPOINT_NOT_FOUND");
        assertJsonError(mockMvc.perform(get("/api/users/me").header("Accept", "image/jpeg")).andReturn(), 401, "UNAUTHORIZED");

        assertThat(output.getAll().substring(offset)).doesNotContain(" ERROR ").doesNotContain("\n\tat ");
    }

    @Test
    void shouldAnswerNotAcceptableAsJsonWithoutErrorLogWhenTheClientAcceptsOnlyAnImage(CapturedOutput output) throws Exception {
        String token = register();
        int offset = output.getAll().length();

        assertJsonError(mockMvc.perform(get("/api/users/me").header("Authorization", "Bearer " + token)
                .header("Accept", "image/jpeg")).andReturn(), 406, "NOT_ACCEPTABLE");

        assertThat(output.getAll().substring(offset)).doesNotContain(" ERROR ").doesNotContain("\n\tat ");
    }

    @Test
    void shouldAnswerTheDataExportErrorAsJsonEvenWhenTheClientAsksForADownloadType(CapturedOutput output) throws Exception {
        String token = register();
        int offset = output.getAll().length();
        String wrongPassword = "{\"currentPassword\":\"senha-errada-123\"}";

        MvcResult asJson = mockMvc.perform(post("/api/users/me/export").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(wrongPassword)).andReturn();
        for (String accept : new String[]{"application/octet-stream", "text/csv", "image/jpeg", "application/zip"}) {
            MvcResult result = mockMvc.perform(post("/api/users/me/export").header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON).header("Accept", accept).content(wrongPassword)).andReturn();
            assertThat(result.getResponse().getStatus()).as("Accept %s", accept).isEqualTo(asJson.getResponse().getStatus());
            assertThat(result.getResponse().getContentType()).startsWith("application/json");
            assertThat(objectMapper.readTree(result.getResponse().getContentAsString()).get("error").get("code").asText())
                    .isEqualTo(objectMapper.readTree(asJson.getResponse().getContentAsString()).get("error").get("code").asText());
        }

        assertThat(output.getAll().substring(offset)).doesNotContain(" ERROR ").doesNotContain("\n\tat ");
    }
}
