package com.shottrack.backend.common.ratelimit;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * ADR-0018: com app.rate-limit.trust-forwarded-header=true (atrás do proxy),
 * o IP do limite é o primeiro de X-Forwarded-For — clientes diferentes que
 * chegam pelo mesmo IP do proxy não dividem o mesmo bucket.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@TestPropertySource(properties = {
        "app.rate-limit.trust-forwarded-header=true",
        "app.rate-limit.registration.per-email=100",
        "app.rate-limit.registration.per-ip=2"
})
class RateLimitForwardedHeaderTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private JavaMailSenderImpl mailSender;

    @Test
    void shouldUseFirstForwardedAddressAsClientIp() throws Exception {
        register("fwd1@shottrack.com", "198.51.100.1, 10.0.0.1").andExpect(status().isAccepted());
        register("fwd2@shottrack.com", "198.51.100.1").andExpect(status().isAccepted());

        register("fwd3@shottrack.com", "198.51.100.1, 10.0.0.9").andExpect(status().isTooManyRequests());
    }

    @Test
    void shouldKeepDifferentForwardedClientsBehindTheSameProxyIndependent() throws Exception {
        register("proxy1@shottrack.com", "198.51.100.10").andExpect(status().isAccepted());
        register("proxy2@shottrack.com", "198.51.100.10").andExpect(status().isAccepted());
        register("proxy3@shottrack.com", "198.51.100.10").andExpect(status().isTooManyRequests());

        register("proxy4@shottrack.com", "198.51.100.11").andExpect(status().isAccepted());
    }

    @Test
    void shouldFallBackToConnectionIpWhenForwardedHeaderIsMissing() throws Exception {
        for (int i = 0; i < 2; i++) {
            mockMvc.perform(post("/api/users/registration")
                            .with(request -> {
                                request.setRemoteAddr("10.9.9.9");
                                return request;
                            })
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new RegistrationRequest("semheader" + i + "@shottrack.com"))))
                    .andExpect(status().isAccepted());
        }

        mockMvc.perform(post("/api/users/registration")
                        .with(request -> {
                            request.setRemoteAddr("10.9.9.9");
                            return request;
                        })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RegistrationRequest("semheader-novo@shottrack.com"))))
                .andExpect(status().isTooManyRequests());
    }

    private ResultActions register(String email, String forwardedFor) throws Exception {
        return mockMvc.perform(post("/api/users/registration")
                .header("X-Forwarded-For", forwardedFor)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new RegistrationRequest(email))));
    }
}
