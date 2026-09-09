package com.shottrack.backend.application.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shottrack.backend.application.auth.dto.LoginRequest;
import com.shottrack.backend.application.auth.dto.RefreshRequest;
import com.shottrack.backend.application.auth.gateway.repository.RefreshTokenRepository;
import com.shottrack.backend.application.auth.model.RefreshToken;
import com.shottrack.backend.application.user.dto.UserRegisterRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class RefreshTokenTest {

    private static final String EMAIL = "atleta.refresh@shottrack.com";
    private static final String PASSWORD = "senha12345";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    private UUID userId;

    @BeforeEach
    void registerUser() throws Exception {
        var request = new UserRegisterRequest("Atleta Refresh", EMAIL, PASSWORD);
        MvcResult result = mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andReturn();
        userId = UUID.fromString(objectMapper.readTree(result.getResponse().getContentAsString())
                .get("data").get("id").asText());
    }

    private String login() throws Exception {
        var request = new LoginRequest(EMAIL, PASSWORD);
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .get("data").get("refreshToken").asText();
    }

    private String refresh(String refreshToken) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RefreshRequest(refreshToken))))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .get("data").get("refreshToken").asText();
    }

    @Test
    void shouldRotateTokensWhenUsingValidRefreshToken() throws Exception {
        String firstToken = login();

        String secondToken = refresh(firstToken);

        assertThat(secondToken).isNotEqualTo(firstToken);
        assertThat(refreshTokenRepository.findByToken(firstToken))
                .as("token antigo precisa ficar marcado como usado/revogado após a rotação")
                .hasValueSatisfying(saved -> assertThat(saved.isRevoked()).isTrue());
        assertThat(refreshTokenRepository.findByToken(secondToken))
                .hasValueSatisfying(saved -> assertThat(saved.isRevoked()).isFalse());
    }

    @Test
    void shouldRevokeAllTokensWhenReuseIsDetected() throws Exception {
        String firstToken = login();
        String secondToken = refresh(firstToken);

        // reuso do primeiro token, já revogado pela rotação acima → sinal de roubo
        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RefreshRequest(firstToken))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("INVALID_REFRESH_TOKEN"));

        assertThat(refreshTokenRepository.findByToken(secondToken))
                .as("todos os tokens do usuário devem ser revogados quando um reuso é detectado")
                .hasValueSatisfying(saved -> assertThat(saved.isRevoked()).isTrue());

        // token2 era legítimo, mas a sessão inteira foi derrubada pela detecção de reuso
        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RefreshRequest(secondToken))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("INVALID_REFRESH_TOKEN"));
    }

    @Test
    void shouldRejectNonExistentRefreshToken() throws Exception {
        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RefreshRequest("token-que-nao-existe"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("INVALID_REFRESH_TOKEN"));
    }

    @Test
    void shouldRejectExpiredRefreshToken() throws Exception {
        RefreshToken expired = new RefreshToken("token-expirado-123", userId, Instant.now().minusSeconds(60));
        refreshTokenRepository.save(expired);

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RefreshRequest("token-expirado-123"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("INVALID_REFRESH_TOKEN"));
    }

    @Test
    void shouldRejectInvalidBody() throws Exception {
        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RefreshRequest(""))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }
}
