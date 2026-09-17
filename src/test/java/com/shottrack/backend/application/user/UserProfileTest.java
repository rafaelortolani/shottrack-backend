package com.shottrack.backend.application.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shottrack.backend.application.user.gateway.repository.PendingRegistrationRepository;
import com.shottrack.backend.common.security.JwtTokenProvider;
import com.shottrack.backend.support.TestUsers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class UserProfileTest {

    private static final String NAME = "Atleta Perfil";
    private static final String EMAIL = "atleta.perfil@shottrack.com";
    private static final String PASSWORD = "senha12345";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private PendingRegistrationRepository pendingRegistrationRepository;

    private String accessToken;

    @BeforeEach
    void registerAndLoginUser() throws Exception {
        accessToken = TestUsers.registerAndLogin(mockMvc, objectMapper, pendingRegistrationRepository, NAME, EMAIL, PASSWORD);
    }

    @Test
    void shouldReturnAuthenticatedUserProfile() throws Exception {
        mockMvc.perform(get("/api/users/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value(NAME))
                .andExpect(jsonPath("$.data.email").value(EMAIL))
                .andExpect(jsonPath("$.data.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.data.passwordHash").doesNotExist());
    }

    @Test
    void shouldRejectRequestWithoutToken() throws Exception {
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
    }

    @Test
    void shouldRejectInvalidToken() throws Exception {
        mockMvc.perform(get("/api/users/me")
                        .header("Authorization", "Bearer token-invalido"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
    }

    /**
     * Token assinado corretamente (passa no JwtAuthenticationFilter), mas pra
     * um id de usuário que nunca existiu — não tem endpoint de exclusão de
     * usuário hoje, então isso só é alcançável forjando o token diretamente
     * com o JwtTokenProvider, como aqui.
     */
    @Test
    void shouldRejectTokenForNonExistentUser() throws Exception {
        String tokenForUnknownUser = jwtTokenProvider.generateAccessToken(UUID.randomUUID());

        mockMvc.perform(get("/api/users/me")
                        .header("Authorization", "Bearer " + tokenForUnknownUser))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
    }
}
