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
 * ADR-0018 / S14: com app.rate-limit.trust-forwarded-header=true (atrás do proxy),
 * o IP do limite é o ÚLTIMO de X-Forwarded-For (o que o proxy acrescentou) — as
 * entradas anteriores são escritas pelo cliente e não valem. Clientes diferentes
 * que chegam pelo mesmo IP do proxy não dividem o mesmo bucket.
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
    void shouldUseLastForwardedAddressAsClientIp() throws Exception {
        register("fwd1@shottrack.com", "10.0.0.1, 198.51.100.1").andExpect(status().isAccepted());
        register("fwd2@shottrack.com", "198.51.100.1").andExpect(status().isAccepted());

        register("fwd3@shottrack.com", "10.0.0.9, 198.51.100.1").andExpect(status().isTooManyRequests());
    }

    @Test
    void shouldNotLetTheClientEscapeTheLimitByForgingTheFirstEntries() throws Exception {
        register("forjado1@shottrack.com", "1.1.1.1, 198.51.100.50").andExpect(status().isAccepted());
        register("forjado2@shottrack.com", "2.2.2.2, 198.51.100.50").andExpect(status().isAccepted());

        register("forjado3@shottrack.com", "3.3.3.3, 198.51.100.50").andExpect(status().isTooManyRequests());
        register("forjado4@shottrack.com", "4.4.4.4, 5.5.5.5, 198.51.100.50").andExpect(status().isTooManyRequests());
    }

    @Test
    void shouldReadForwardedAddressesSplitAcrossHeaderLines() throws Exception {
        mockMvc.perform(post("/api/users/registration")
                .header("X-Forwarded-For", "9.9.9.9")
                .header("X-Forwarded-For", "198.51.100.60")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new RegistrationRequest("duaslinhas1@shottrack.com"))))
                .andExpect(status().isAccepted());
        register("duaslinhas2@shottrack.com", "198.51.100.60").andExpect(status().isAccepted());

        register("duaslinhas3@shottrack.com", "7.7.7.7, 198.51.100.60").andExpect(status().isTooManyRequests());
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
