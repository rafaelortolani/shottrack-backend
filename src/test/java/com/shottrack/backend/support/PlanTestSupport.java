package com.shottrack.backend.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shottrack.backend.application.user.gateway.repository.PendingRegistrationRepository;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** ADR-0027: cria um administrador e altera limites de plano pela API (como o painel faz), para os testes de limites. */
public final class PlanTestSupport {

    private PlanTestSupport() {
    }

    /** Cadastra um atleta, promove a ADMIN direto no banco (ADR-0021) e devolve o token. */
    public static String adminToken(MockMvc mockMvc, ObjectMapper objectMapper, PendingRegistrationRepository pendingRegistrationRepository,
                                    JdbcTemplate jdbc) throws Exception {
        String email = "admin.plano." + UUID.randomUUID() + "@shottrack.com";
        String token = TestUsers.registerAndLogin(mockMvc, objectMapper, pendingRegistrationRepository, "Admin Plano", email, "senha12345");
        jdbc.update("UPDATE users SET role = 'ADMIN' WHERE email = ?", email);
        return token;
    }

    public static void setLimits(MockMvc mockMvc, ObjectMapper objectMapper, String adminToken, String planCode,
                                 Map<String, Object> limits) throws Exception {
        mockMvc.perform(patch("/api/admin/plans/" + planCode + "/limits").header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(Map.of("limits", limits))))
                .andExpect(status().isOk());
    }
}
