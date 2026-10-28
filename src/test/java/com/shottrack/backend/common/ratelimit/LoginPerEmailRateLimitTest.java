package com.shottrack.backend.common.ratelimit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shottrack.backend.application.auth.dto.LoginRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * S14 (a): além de IP + e-mail, o login tem um teto por e-mail (minúsculas), qualquer que seja o
 * IP — barra quem espalha as tentativas por muitos IPs. Aqui o limite por IP + e-mail fica alto
 * para isolar o teto por e-mail.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@TestPropertySource(properties = {
        "app.rate-limit.login.attempts=1000",
        "app.rate-limit.login.per-email=4"
})
class LoginPerEmailRateLimitTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private ResultActions login(String email, String ip) throws Exception {
        return mockMvc.perform(post("/api/auth/login")
                .with(request -> {
                    request.setRemoteAddr(ip);
                    return request;
                })
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new LoginRequest(email, "senha-errada"))));
    }

    @Test
    void shouldLimitLoginPerEmailAcrossManyIps() throws Exception {
        for (int i = 1; i <= 4; i++) {
            login("alvo.espalhado@shottrack.com", "10.40.0." + i).andExpect(status().isUnauthorized());
        }

        login("alvo.espalhado@shottrack.com", "10.40.0.200")
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andExpect(jsonPath("$.error.code").value("TOO_MANY_REQUESTS"));
    }

    @Test
    void shouldCountEmailCaseInsensitivelyAcrossIps() throws Exception {
        login("Alvo.Maiusculas@shottrack.com", "10.41.0.1").andExpect(status().isUnauthorized());
        login("ALVO.MAIUSCULAS@shottrack.com", "10.41.0.2").andExpect(status().isUnauthorized());
        login("alvo.maiusculas@shottrack.com", "10.41.0.3").andExpect(status().isUnauthorized());
        login("alvo.MAIUSCULAS@shottrack.com", "10.41.0.4").andExpect(status().isUnauthorized());

        login("aLvo.maiusculas@shottrack.com", "10.41.0.5").andExpect(status().isTooManyRequests());
    }

    @Test
    void shouldKeepTheEmailLimitIndependentBetweenEmails() throws Exception {
        for (int i = 1; i <= 4; i++) {
            login("independente.a@shottrack.com", "10.42.0." + i).andExpect(status().isUnauthorized());
        }
        login("independente.a@shottrack.com", "10.42.0.9").andExpect(status().isTooManyRequests());

        login("independente.b@shottrack.com", "10.42.0.9").andExpect(status().isUnauthorized());
    }
}
