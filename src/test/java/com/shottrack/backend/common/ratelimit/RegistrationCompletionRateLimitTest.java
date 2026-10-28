package com.shottrack.backend.common.ratelimit;

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

/** S13 (ADR-0018): conclusão de cadastro limitada por IP. Classe própria: o helper de cadastro dos outros testes usa essa rota. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@TestPropertySource(properties = "app.rate-limit.registration-completion.attempts=3")
class RegistrationCompletionRateLimitTest {

    @Autowired
    private MockMvc mockMvc;

    private ResultActions complete(String ip) throws Exception {
        return mockMvc.perform(post("/api/users/registration/completion")
                .with(request -> {
                    request.setRemoteAddr(ip);
                    return request;
                })
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\"inexistente\",\"name\":\"Atleta\",\"password\":\"senha12345\",\"acceptedTerms\":true}"));
    }

    @Test
    void shouldLimitRegistrationCompletionPerIp() throws Exception {
        for (int i = 0; i < 3; i++) {
            complete("10.31.0.1").andExpect(status().isBadRequest());
        }

        complete("10.31.0.1")
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andExpect(jsonPath("$.error.code").value("TOO_MANY_REQUESTS"));
    }

    @Test
    void shouldKeepRegistrationCompletionLimitIndependentBetweenIps() throws Exception {
        for (int i = 0; i < 3; i++) {
            complete("10.31.0.2").andExpect(status().isBadRequest());
        }
        complete("10.31.0.2").andExpect(status().isTooManyRequests());

        complete("10.31.0.3").andExpect(status().isBadRequest());
    }
}
