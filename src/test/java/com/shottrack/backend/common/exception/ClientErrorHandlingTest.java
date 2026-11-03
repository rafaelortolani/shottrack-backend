package com.shottrack.backend.common.exception;

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
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.MissingServletRequestParameterException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * S08: rota inexistente, método errado, Content-Type inválido, JSON malformado e parâmetro ausente
 * são erro de quem chama — respondem 404/405/415/400 no envelope de erro, sem log ERROR (que viraria
 * evento no Sentry) e sem 500.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ExtendWith(OutputCaptureExtension.class)
@Transactional
class ClientErrorHandlingTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ApiExceptionHandler handler;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PendingRegistrationRepository pendingRegistrationRepository;

    @MockBean
    private JavaMailSenderImpl mailSender;

    /** S23: rota inexistente sob /api/auth não é mais pública — o 404 só aparece com login. */
    private String login() throws Exception {
        return "Bearer " + TestUsers.registerAndLogin(mockMvc, objectMapper, pendingRegistrationRepository,
                "Atleta", "atleta.erro404@shottrack.com", "senha12345");
    }

    @Test
    void shouldAnswer404ForUnknownRoute(CapturedOutput output) throws Exception {
        mockMvc.perform(get("/api/nao-existe").header("Authorization", login()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("ENDPOINT_NOT_FOUND"))
                .andExpect(jsonPath("$.error.message").value("Rota não encontrada"));
        assertNoErrorLog(output);
    }

    @Test
    void shouldTranslateTheUnknownRouteMessageToEnglish() throws Exception {
        mockMvc.perform(get("/api/nao-existe").header("Authorization", login()).header("Accept-Language", "en"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.message").value("Route not found"));
    }

    @Test
    void shouldProtectUnknownRoutesUnderApiAuth() throws Exception {
        mockMvc.perform(get("/api/auth/nao-existe"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
    }

    @Test
    void shouldAnswer405WithAllowHeaderForWrongMethod(CapturedOutput output) throws Exception {
        mockMvc.perform(get("/api/auth/login"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(header().string("Allow", "POST"))
                .andExpect(jsonPath("$.error.code").value("METHOD_NOT_ALLOWED"));
        assertNoErrorLog(output);
    }

    @Test
    void shouldAnswer415ForTextPlainBody(CapturedOutput output) throws Exception {
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.TEXT_PLAIN).content("oi"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.error.code").value("UNSUPPORTED_MEDIA_TYPE"));
        assertNoErrorLog(output);
    }

    @Test
    void shouldAnswer400ForMalformedJson(CapturedOutput output) throws Exception {
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("{\"email\": x@y.com"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
        assertNoErrorLog(output);
        assertThat(output.getAll()).doesNotContain("x@y.com");
    }

    @Test
    void shouldAnswer400ForMissingRequestParameter(CapturedOutput output) {
        // nenhuma rota atual tem @RequestParam obrigatório: exercita o handler direto
        var response = handler.handleMissingParameter(new MissingServletRequestParameterException("page", "int"));

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody().error().code()).isEqualTo("MISSING_PARAMETER");
        assertNoErrorLog(output);
    }

    private static void assertNoErrorLog(CapturedOutput output) {
        assertThat(output.getAll()).doesNotContain(" ERROR ").doesNotContain("Erro inesperado");
    }
}
