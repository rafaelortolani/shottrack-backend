package com.shottrack.backend.common.ratelimit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shottrack.backend.application.user.gateway.repository.PendingRegistrationRepository;
import com.shottrack.backend.support.TestUsers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * S13 (ADR-0018): limite em refresh, confirmação de redefinição de senha, troca de senha, troca de
 * e-mail e confirmação do código. As rotas autenticadas limitam por usuário; as públicas, por IP.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@TestPropertySource(properties = {
        "app.rate-limit.refresh.attempts=3",
        "app.rate-limit.password-reset-confirm.attempts=3",
        "app.rate-limit.password-change.attempts=2",
        "app.rate-limit.email-change.attempts=2",
        "app.rate-limit.email-change-confirmation.attempts=2"
})
class SensitiveRoutesRateLimitTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PendingRegistrationRepository pendingRegistrationRepository;

    @MockBean
    private JavaMailSenderImpl mailSender;

    private ResultActions call(HttpMethod method, String path, String token, String ip, String body) throws Exception {
        var builder = request(method, path)
                .with(r -> {
                    r.setRemoteAddr(ip);
                    return r;
                })
                .contentType(MediaType.APPLICATION_JSON)
                .content(body);
        if (token != null) {
            builder.header("Authorization", "Bearer " + token);
        }
        return mockMvc.perform(builder);
    }

    private void assertLimited(ResultActions result) throws Exception {
        result.andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andExpect(jsonPath("$.error.code").value("TOO_MANY_REQUESTS"));
    }

    private String athlete(String email) throws Exception {
        return TestUsers.registerAndLogin(mockMvc, objectMapper, pendingRegistrationRepository, "Atleta", email, "senha12345");
    }

    @Test
    void shouldLimitRefreshPerIp() throws Exception {
        for (int i = 0; i < 3; i++) {
            call(HttpMethod.POST, "/api/auth/refresh", null, "10.30.0.1", "{\"refreshToken\":\"x\"}")
                    .andExpect(status().isUnauthorized());
        }
        assertLimited(call(HttpMethod.POST, "/api/auth/refresh", null, "10.30.0.1", "{\"refreshToken\":\"x\"}"));

        call(HttpMethod.POST, "/api/auth/refresh", null, "10.30.0.2", "{\"refreshToken\":\"x\"}")
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldLimitPasswordResetConfirmationPerIp() throws Exception {
        String body = "{\"token\":\"inexistente\",\"newPassword\":\"novaSenha123\"}";
        for (int i = 0; i < 3; i++) {
            call(HttpMethod.POST, "/api/auth/password-reset/confirm", null, "10.30.0.3", body)
                    .andExpect(status().isBadRequest());
        }
        assertLimited(call(HttpMethod.POST, "/api/auth/password-reset/confirm", null, "10.30.0.3", body));
    }

    @Test
    void shouldLimitPasswordChangePerUserSoTheCurrentPasswordCannotBeGuessed() throws Exception {
        String token = athlete("limite.trocasenha@shottrack.com");
        String wrong = "{\"currentPassword\":\"errada123\",\"newPassword\":\"novaSenha123\"}";

        call(HttpMethod.PATCH, "/api/users/me/password", token, "10.30.0.4", wrong).andExpect(status().isUnauthorized());
        call(HttpMethod.PATCH, "/api/users/me/password", token, "10.30.0.4", wrong).andExpect(status().isUnauthorized());

        // mesmo trocando de IP, o limite é do usuário
        assertLimited(call(HttpMethod.PATCH, "/api/users/me/password", token, "10.30.0.99", wrong));
    }

    @Test
    void shouldKeepPasswordChangeLimitIndependentBetweenUsers() throws Exception {
        String first = athlete("senha.a@shottrack.com");
        String second = athlete("senha.b@shottrack.com");
        String wrong = "{\"currentPassword\":\"errada123\",\"newPassword\":\"novaSenha123\"}";

        call(HttpMethod.PATCH, "/api/users/me/password", first, "10.30.0.5", wrong);
        call(HttpMethod.PATCH, "/api/users/me/password", first, "10.30.0.5", wrong);
        assertLimited(call(HttpMethod.PATCH, "/api/users/me/password", first, "10.30.0.5", wrong));

        call(HttpMethod.PATCH, "/api/users/me/password", second, "10.30.0.5", wrong).andExpect(status().isUnauthorized());
    }

    @Test
    void shouldLimitEmailChangeRequestsPerUser() throws Exception {
        String token = athlete("limite.trocaemail@shottrack.com");

        for (int i = 0; i < 2; i++) {
            call(HttpMethod.POST, "/api/users/me/email", token, "10.30.0.6",
                    "{\"email\":\"destino" + i + "@example.invalid\"}").andExpect(status().isAccepted());
        }
        assertLimited(call(HttpMethod.POST, "/api/users/me/email", token, "10.30.0.6",
                "{\"email\":\"destino-extra@example.invalid\"}"));
    }

    @Test
    void shouldLimitEmailChangeCodeAttemptsPerUser() throws Exception {
        String token = athlete("limite.codigo@shottrack.com");

        call(HttpMethod.POST, "/api/users/me/email/confirmation", token, "10.30.0.7", "{\"code\":\"000001\"}")
                .andExpect(status().isBadRequest());
        call(HttpMethod.POST, "/api/users/me/email/confirmation", token, "10.30.0.7", "{\"code\":\"000002\"}")
                .andExpect(status().isBadRequest());

        assertLimited(call(HttpMethod.POST, "/api/users/me/email/confirmation", token, "10.30.0.8", "{\"code\":\"000003\"}"));
    }
}
