package com.shottrack.backend.common.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shottrack.backend.application.user.gateway.repository.PendingRegistrationRepository;
import com.shottrack.backend.support.TestUsers;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AdminAuthorizationTest {

    private static final String EMAIL = "atleta.admin@shottrack.com";
    private static final String PING = "/api/admin/test/ping";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PendingRegistrationRepository pendingRegistrationRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EntityManager entityManager;

    private String accessToken;

    @BeforeEach
    void registerAndLoginUser() throws Exception {
        accessToken = TestUsers.registerAndLogin(mockMvc, objectMapper, pendingRegistrationRepository,
                "Atleta Admin", EMAIL, "senha12345");
    }

    /** Promoção/rebaixamento são feitos só no banco (ADR-0021). */
    private void setRole(String role) {
        entityManager.flush();
        jdbcTemplate.update("UPDATE users SET role = ? WHERE email = ?", role, EMAIL);
        entityManager.clear();
    }

    @Test
    void shouldReturnUserRoleOnProfile() throws Exception {
        mockMvc.perform(get("/api/users/me").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.role").value("USER"));
    }

    @Test
    void shouldReturnAdminRoleOnProfile() throws Exception {
        setRole("ADMIN");

        mockMvc.perform(get("/api/users/me").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.role").value("ADMIN"));
    }

    @Test
    void shouldIgnoreRoleSentOnProfileUpdate() throws Exception {
        mockMvc.perform(patch("/api/users/me")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Novo Nome\",\"experienceLevel\":\"ADVANCED\",\"role\":\"ADMIN\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Novo Nome"))
                .andExpect(jsonPath("$.data.role").value("USER"));

        entityManager.flush();
        entityManager.clear();
        mockMvc.perform(get("/api/users/me").header("Authorization", "Bearer " + accessToken))
                .andExpect(jsonPath("$.data.role").value("USER"));
        mockMvc.perform(get(PING).header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldRejectAdminRouteWithoutToken() throws Exception {
        mockMvc.perform(get(PING))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
    }

    @Test
    void shouldRejectAdminRouteWithInvalidToken() throws Exception {
        mockMvc.perform(get(PING).header("Authorization", "Bearer token-invalido"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
    }

    @Test
    void shouldForbidAdminRouteForRegularUser() throws Exception {
        mockMvc.perform(get(PING).header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.error.message").value("Você não tem permissão para acessar este recurso"));
    }

    @Test
    void shouldTranslateForbiddenMessage() throws Exception {
        mockMvc.perform(get(PING)
                        .header("Authorization", "Bearer " + accessToken)
                        .header("Accept-Language", "en"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.message").value("You do not have permission to access this resource"));
    }

    @Test
    void shouldAllowAdminRouteForAdmin() throws Exception {
        setRole("ADMIN");

        mockMvc.perform(get(PING).header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value("pong"));
    }

    @Test
    void shouldDenyDemotedAdminOnNextRequest() throws Exception {
        setRole("ADMIN");
        mockMvc.perform(get(PING).header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());

        setRole("USER");

        mockMvc.perform(get(PING).header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
    }

    @Test
    void shouldGrantPromotedUserOnNextRequestWithSameToken() throws Exception {
        mockMvc.perform(get(PING).header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isForbidden());

        setRole("ADMIN");

        mockMvc.perform(get(PING).header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());
    }
}
