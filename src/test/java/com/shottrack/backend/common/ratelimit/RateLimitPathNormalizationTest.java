package com.shottrack.backend.common.ratelimit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shottrack.backend.application.auth.dto.LoginRequest;
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
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * S01: o limite é decidido pelo caminho NORMALIZADO. Variações do mesmo endereço (%xx, barra final)
 * caem no MESMO bucket do caminho exato e nunca chegam ao controller (400 INVALID_REQUEST_PATH);
 * `//`, `;param` e maiúsculas já são barrados antes pelo Spring Security (400/401).
 * Cada teste usa IP e e-mail próprios porque os buckets vivem enquanto o contexto viver.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@TestPropertySource(properties = {
        "app.rate-limit.login.attempts=3",
        "app.rate-limit.registration.per-email=100",
        "app.rate-limit.registration.per-ip=3"
})
class RateLimitPathNormalizationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private JavaMailSenderImpl mailSender;

    private ResultActions login(String path, String email, String ip) throws Exception {
        return mockMvc.perform(post(java.net.URI.create(path))
                .with(request -> {
                    request.setRemoteAddr(ip);
                    return request;
                })
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new LoginRequest(email, "senha-errada"))));
    }

    @Test
    void shouldLimitTheExactPath() throws Exception {
        for (int i = 0; i < 3; i++) {
            login("/api/auth/login", "exato@shottrack.com", "10.20.0.1").andExpect(status().isUnauthorized());
        }
        login("/api/auth/login", "exato@shottrack.com", "10.20.0.1").andExpect(status().isTooManyRequests());
    }

    @Test
    void shouldLimitAPercentEncodedPathAndNeverReachTheController() throws Exception {
        for (int i = 0; i < 3; i++) {
            login("/api/auth/log%69n", "codificado@shottrack.com", "10.20.0.2")
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error.code").value("INVALID_REQUEST_PATH"));
        }

        login("/api/auth/log%69n", "codificado@shottrack.com", "10.20.0.2").andExpect(status().isTooManyRequests());
    }

    @Test
    void shouldShareTheBucketBetweenTheEncodedAndTheExactPath() throws Exception {
        login("/api/auth/log%69n", "misto@shottrack.com", "10.20.0.3").andExpect(status().isBadRequest());
        login("/api/auth/login", "misto@shottrack.com", "10.20.0.3").andExpect(status().isUnauthorized());
        login("/api/auth/%6Cogin", "misto@shottrack.com", "10.20.0.3").andExpect(status().isBadRequest());

        login("/api/auth/login", "misto@shottrack.com", "10.20.0.3").andExpect(status().isTooManyRequests());
    }

    @Test
    void shouldLimitTrailingSlashAsTheSameRule() throws Exception {
        login("/api/auth/login/", "barrafinal@shottrack.com", "10.20.0.4")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_REQUEST_PATH"));
        login("/api/auth/login/", "barrafinal@shottrack.com", "10.20.0.4").andExpect(status().isBadRequest());
        login("/api/auth/login", "barrafinal@shottrack.com", "10.20.0.4").andExpect(status().isUnauthorized());

        login("/api/auth/login/", "barrafinal@shottrack.com", "10.20.0.4").andExpect(status().isTooManyRequests());
    }

    @Test
    void shouldNeverLetUppercasePathsReachTheLogin() throws Exception {
        // o Spring Security não reconhece o caminho em maiúsculas como público: 401 antes do login (e do
        // filtro) — nunca INVALID_CREDENTIALS, que seria o login sendo processado sem limite
        for (String path : new String[]{"/API/AUTH/LOGIN", "/Api/Auth/Login"}) {
            for (int i = 0; i < 5; i++) {
                login(path, "maiusculas@shottrack.com", "10.20.0.8")
                        .andExpect(status().isUnauthorized())
                        .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
            }
        }
    }

    @Test
    void shouldRejectDoubleSlashAndMatrixParameterBeforeTheyReachTheLogin() throws Exception {
        for (String path : new String[]{"/api//auth/login", "/api/auth/login;jsessionid=abc", "/api/auth//login"}) {
            for (int i = 0; i < 5; i++) {
                // nunca 401 (login processado): o firewall/filtro barra todas, em qualquer quantidade
                login(path, "firewall@shottrack.com", "10.20.0.5")
                        .andExpect(result -> {
                            int code = result.getResponse().getStatus();
                            if (code != 400 && code != 429) {
                                throw new AssertionError(path + " devolveu " + code + " (esperado 400 ou 429)");
                            }
                        });
            }
        }
        // e o caminho exato continua com o orçamento inteiro dele
        login("/api/auth/login", "firewall@shottrack.com", "10.20.0.5").andExpect(status().isUnauthorized());
    }

    @Test
    void shouldLimitEncodedVariantsOfOtherRulesToo() throws Exception {
        for (int i = 0; i < 3; i++) {
            mockMvc.perform(post(java.net.URI.create("/api/users/regis%74ration"))
                            .with(request -> {
                                request.setRemoteAddr("10.20.0.6");
                                return request;
                            })
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new RegistrationRequest("cod" + i + "@shottrack.com"))))
                    .andExpect(status().isBadRequest());
        }

        mockMvc.perform(post("/api/users/registration")
                        .with(request -> {
                            request.setRemoteAddr("10.20.0.6");
                            return request;
                        })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RegistrationRequest("cod-final@shottrack.com"))))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    void shouldTranslateTheInvalidPathMessageToEnglish() throws Exception {
        mockMvc.perform(post(java.net.URI.create("/api/auth/log%69n"))
                        .with(request -> {
                            request.setRemoteAddr("10.20.0.7");
                            return request;
                        })
                        .header("Accept-Language", "en")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"ingles@shottrack.com\",\"password\":\"x\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.message").value("Invalid request path"));
    }

    @Test
    void shouldNotTouchRoutesWithoutARuleThatHaveEncodedCharacters() throws Exception {
        mockMvc.perform(post(java.net.URI.create("/api/auth/logo%75t"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"x\"}"))
                .andExpect(status().isOk());
    }
}
