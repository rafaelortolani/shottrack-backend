package com.shottrack.backend.common.ratelimit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shottrack.backend.application.auth.dto.LoginRequest;
import com.shottrack.backend.application.auth.dto.PasswordResetRequest;
import com.shottrack.backend.application.user.dto.RegistrationRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * ADR-0018: os buckets vivem enquanto o contexto Spring viver, então cada
 * teste usa IP e e-mails próprios (remoteAddr distinto) pra não interferir nos outros.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@TestPropertySource(properties = {
        "app.rate-limit.login.attempts=3",
        "app.rate-limit.registration.per-email=2",
        "app.rate-limit.registration.per-ip=4",
        "app.rate-limit.password-reset-request.per-email=2",
        "app.rate-limit.password-reset-request.per-ip=4"
})
class RateLimitTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private JavaMailSenderImpl mailSender;

    @Test
    void shouldLimitLoginAttemptsPerIpAndEmail() throws Exception {
        for (int i = 0; i < 3; i++) {
            login("limite.login@shottrack.com", "10.1.0.1").andExpect(status().isUnauthorized());
        }

        login("limite.login@shottrack.com", "10.1.0.1")
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andExpect(jsonPath("$.error.code").value("TOO_MANY_REQUESTS"))
                .andExpect(jsonPath("$.error.message").value("Muitas tentativas. Aguarde um pouco e tente novamente"));
    }

    @Test
    void shouldReturnRetryAfterInSecondsWithinTheWindow() throws Exception {
        for (int i = 0; i < 3; i++) {
            login("retry.after@shottrack.com", "10.1.0.2");
        }

        String retryAfter = login("retry.after@shottrack.com", "10.1.0.2")
                .andExpect(status().isTooManyRequests())
                .andReturn().getResponse().getHeader("Retry-After");

        assertThat(Long.parseLong(retryAfter)).isBetween(1L, 15 * 60L);
    }

    @Test
    void shouldTranslateTooManyRequestsToEnglish() throws Exception {
        for (int i = 0; i < 3; i++) {
            login("english.limit@shottrack.com", "10.1.0.3");
        }

        mockMvc.perform(post("/api/auth/login")
                        .with(remoteAddr("10.1.0.3"))
                        .header("Accept-Language", "en")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("english.limit@shottrack.com", "x"))))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.error.message").value("Too many attempts. Please wait a moment and try again"));
    }

    @Test
    void shouldKeepLoginKeysPerIpAndEmailIndependent() throws Exception {
        for (int i = 0; i < 3; i++) {
            login("chave.a@shottrack.com", "10.1.0.4");
        }
        login("chave.a@shottrack.com", "10.1.0.4").andExpect(status().isTooManyRequests());

        login("chave.b@shottrack.com", "10.1.0.4").andExpect(status().isUnauthorized());
        login("chave.a@shottrack.com", "10.1.0.5").andExpect(status().isUnauthorized());
    }

    @Test
    void shouldNormalizeEmailBeforeBuildingLoginKey() throws Exception {
        login("Normaliza@Shottrack.com", "10.1.0.6");
        login("  normaliza@shottrack.com ", "10.1.0.6");
        login("NORMALIZA@shottrack.com", "10.1.0.6");

        login("normaliza@shottrack.com", "10.1.0.6").andExpect(status().isTooManyRequests());
    }

    @Test
    void shouldLimitRegistrationPerEmailAcrossIps() throws Exception {
        register("limite.cadastro@shottrack.com", "10.2.0.1").andExpect(status().isAccepted());
        register("limite.cadastro@shottrack.com", "10.2.0.2").andExpect(status().isAccepted());

        register("limite.cadastro@shottrack.com", "10.2.0.3")
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andExpect(jsonPath("$.error.code").value("TOO_MANY_REQUESTS"));

        register("outro.cadastro@shottrack.com", "10.2.0.3").andExpect(status().isAccepted());
    }

    @Test
    void shouldLimitRegistrationPerIpAcrossEmails() throws Exception {
        for (int i = 0; i < 4; i++) {
            register("ip.cadastro" + i + "@shottrack.com", "10.2.1.1").andExpect(status().isAccepted());
        }

        register("ip.cadastro-novo@shottrack.com", "10.2.1.1")
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"));

        register("ip.cadastro-novo@shottrack.com", "10.2.1.2").andExpect(status().isAccepted());
    }

    @Test
    void shouldLimitPasswordResetRequestsPerEmailAcrossIps() throws Exception {
        resetRequest("limite.reset@shottrack.com", "10.3.0.1").andExpect(status().isAccepted());
        resetRequest("limite.reset@shottrack.com", "10.3.0.2").andExpect(status().isAccepted());

        resetRequest("limite.reset@shottrack.com", "10.3.0.3")
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andExpect(jsonPath("$.error.code").value("TOO_MANY_REQUESTS"));
    }

    @Test
    void shouldLimitPasswordResetRequestsPerIpAcrossEmails() throws Exception {
        for (int i = 0; i < 4; i++) {
            resetRequest("ip.reset" + i + "@shottrack.com", "10.3.1.1").andExpect(status().isAccepted());
        }

        resetRequest("ip.reset-novo@shottrack.com", "10.3.1.1")
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"));
    }

    @Test
    void shouldApplyOnlyIpLimitWhenBodyHasNoReadableEmail() throws Exception {
        for (int i = 0; i < 4; i++) {
            rawPost("/api/users/registration", "{}", "10.4.0.1").andExpect(status().isBadRequest());
        }

        rawPost("/api/users/registration", "{}", "10.4.0.1").andExpect(status().isTooManyRequests());
        rawPost("/api/users/registration", "{}", "10.4.0.2").andExpect(status().isBadRequest());
    }

    @Test
    void shouldStillDeliverTheBodyToTheControllerAfterReadingTheEmail() throws Exception {
        register("corpo.intacto@shottrack.com", "10.4.1.1")
                .andExpect(status().isAccepted());
    }

    @Test
    void shouldNotLimitRoutesWithoutARule() throws Exception {
        for (int i = 0; i < 10; i++) {
            rawPost("/api/auth/logout", "{\"refreshToken\":\"x\"}", "10.4.2.1").andExpect(status().isOk());
        }
    }

    @Test
    void shouldIgnoreForwardedHeaderByDefault() throws Exception {
        for (int i = 0; i < 4; i++) {
            mockMvc.perform(post("/api/users/registration")
                            .with(remoteAddr("10.5.0.1"))
                            .header("X-Forwarded-For", "203.0.113." + i)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new RegistrationRequest("xff" + i + "@shottrack.com"))))
                    .andExpect(status().isAccepted());
        }

        mockMvc.perform(post("/api/users/registration")
                        .with(remoteAddr("10.5.0.1"))
                        .header("X-Forwarded-For", "203.0.113.99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RegistrationRequest("xff-novo@shottrack.com"))))
                .andExpect(status().isTooManyRequests());
    }

    private ResultActions login(String email, String ip) throws Exception {
        return mockMvc.perform(post("/api/auth/login")
                .with(remoteAddr(ip))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new LoginRequest(email, "senha-errada"))));
    }

    private ResultActions register(String email, String ip) throws Exception {
        return mockMvc.perform(post("/api/users/registration")
                .with(remoteAddr(ip))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new RegistrationRequest(email))));
    }

    private ResultActions resetRequest(String email, String ip) throws Exception {
        return mockMvc.perform(post("/api/auth/password-reset/request")
                .with(remoteAddr(ip))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new PasswordResetRequest(email))));
    }

    private ResultActions rawPost(String path, String body, String ip) throws Exception {
        return mockMvc.perform(post(path)
                .with(remoteAddr(ip))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private static RequestPostProcessor remoteAddr(String ip) {
        return request -> {
            request.setRemoteAddr(ip);
            return request;
        };
    }
}
