package com.shottrack.backend.application.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shottrack.backend.application.auth.dto.LoginRequest;
import com.shottrack.backend.application.auth.dto.LogoutRequest;
import com.shottrack.backend.application.auth.dto.RefreshRequest;
import com.shottrack.backend.application.auth.gateway.repository.RefreshTokenRepository;
import com.shottrack.backend.application.auth.model.RefreshToken;
import com.shottrack.backend.application.user.gateway.repository.PendingRegistrationRepository;
import com.shottrack.backend.application.user.gateway.repository.UserRepository;
import com.shottrack.backend.support.TestUsers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class LogoutTest {

    private static final String EMAIL = "atleta.logout@shottrack.com";
    private static final String PASSWORD = "senha12345";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PendingRegistrationRepository pendingRegistrationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    private String refreshToken;

    @BeforeEach
    void registerAndLogin() throws Exception {
        TestUsers.register(mockMvc, objectMapper, pendingRegistrationRepository, "Atleta Teste", EMAIL, PASSWORD);
        String body = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(EMAIL, PASSWORD))))
                .andReturn().getResponse().getContentAsString();
        refreshToken = objectMapper.readTree(body).get("data").get("refreshToken").asText();
    }

    @Test
    void shouldRevokeRefreshTokenOnLogout() throws Exception {
        logout(refreshToken).andExpect(status().isOk());

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RefreshRequest(refreshToken))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("INVALID_REFRESH_TOKEN"));
    }

    @Test
    void shouldWorkWithoutAccessTokenOrWithAnExpiredOne() throws Exception {
        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", "Bearer token-de-acesso-expirado-ou-invalido")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LogoutRequest(refreshToken))))
                .andExpect(status().isOk());
    }

    @Test
    void shouldBeIdempotentForAlreadyRevokedToken() throws Exception {
        logout(refreshToken).andExpect(status().isOk());
        logout(refreshToken).andExpect(status().isOk());
    }

    @Test
    void shouldNotRevokeOtherSessionsWhenLoggingOutAlreadyRevokedToken() throws Exception {
        String body = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(EMAIL, PASSWORD))))
                .andReturn().getResponse().getContentAsString();
        String otherSession = objectMapper.readTree(body).get("data").get("refreshToken").asText();

        logout(refreshToken).andExpect(status().isOk());
        logout(refreshToken).andExpect(status().isOk());

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RefreshRequest(otherSession))))
                .andExpect(status().isOk());
    }

    @Test
    void shouldSucceedForUnknownToken() throws Exception {
        logout("refresh-token-que-nunca-existiu").andExpect(status().isOk());
    }

    @Test
    void shouldSucceedForExpiredToken() throws Exception {
        UUID userId = userRepository.findByEmail(EMAIL).orElseThrow().getId();
        refreshTokenRepository.save(RefreshToken.builder()
                .token("refresh-expirado")
                .userId(userId)
                .expiresAt(Instant.now().minus(Duration.ofDays(1)))
                .build());

        logout("refresh-expirado").andExpect(status().isOk());
    }

    @Test
    void shouldRejectMissingRefreshToken() throws Exception {
        mockMvc.perform(post("/api/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    void shouldRejectBlankRefreshToken() throws Exception {
        logout("")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    private ResultActions logout(String token) throws Exception {
        return mockMvc.perform(post("/api/auth/logout")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new LogoutRequest(token))));
    }
}
