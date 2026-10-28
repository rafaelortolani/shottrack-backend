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

/** S14: com 2 proxies confiáveis, o cliente é a penúltima entrada de X-Forwarded-For. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@TestPropertySource(properties = {
        "app.rate-limit.trust-forwarded-header=true",
        "app.rate-limit.trusted-proxy-hops=2",
        "app.rate-limit.registration.per-email=100",
        "app.rate-limit.registration.per-ip=2"
})
class RateLimitTrustedProxyHopsTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private JavaMailSenderImpl mailSender;

    @Test
    void shouldUseTheEntryTwoPositionsFromTheEnd() throws Exception {
        register("hop1@shottrack.com", "6.6.6.6, 198.51.100.70, 10.0.0.1").andExpect(status().isAccepted());
        register("hop2@shottrack.com", "8.8.8.8, 198.51.100.70, 10.0.0.2").andExpect(status().isAccepted());

        register("hop3@shottrack.com", "9.9.9.9, 198.51.100.70, 10.0.0.3").andExpect(status().isTooManyRequests());
    }

    @Test
    void shouldFallBackToTheConnectionIpWhenTheHeaderHasFewerEntriesThanProxies() throws Exception {
        // 1 entrada só, mas esperamos 2 proxies: o header não é confiável → vale o IP da conexão
        for (int i = 0; i < 2; i++) {
            mockMvc.perform(post("/api/users/registration")
                            .with(request -> {
                                request.setRemoteAddr("10.77.0.1");
                                return request;
                            })
                            .header("X-Forwarded-For", "203.0.113." + i)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new RegistrationRequest("curto" + i + "@shottrack.com"))))
                    .andExpect(status().isAccepted());
        }

        mockMvc.perform(post("/api/users/registration")
                        .with(request -> {
                            request.setRemoteAddr("10.77.0.1");
                            return request;
                        })
                        .header("X-Forwarded-For", "203.0.113.99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RegistrationRequest("curto-novo@shottrack.com"))))
                .andExpect(status().isTooManyRequests());
    }

    private ResultActions register(String email, String forwardedFor) throws Exception {
        return mockMvc.perform(post("/api/users/registration")
                .header("X-Forwarded-For", forwardedFor)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new RegistrationRequest(email))));
    }
}
