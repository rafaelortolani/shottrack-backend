package com.shottrack.backend.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shottrack.backend.application.user.gateway.repository.PendingRegistrationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;

/** Base dos testes de planos e limites (ADR-0027): atleta e administrador prontos e chamadas HTTP curtas. Cada teste é revertido. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Import(FlushAfterEachRequest.class)
public abstract class PlanApiTest {

    protected static final String PASSWORD = "senha12345";

    @Autowired
    protected MockMvc mockMvc;
    @Autowired
    protected ObjectMapper objectMapper;
    @Autowired
    protected PendingRegistrationRepository pendingRegistrationRepository;
    @Autowired
    protected JdbcTemplate jdbc;

    protected String athleteEmail(String prefix) {
        return prefix + "." + UUID.randomUUID() + "@shottrack.com";
    }

    protected String athlete(String email) throws Exception {
        return TestUsers.registerAndLogin(mockMvc, objectMapper, pendingRegistrationRepository, "Atleta Plano", email, PASSWORD);
    }

    protected String athlete() throws Exception {
        return athlete(athleteEmail("atleta.plano"));
    }

    protected String admin() throws Exception {
        return PlanTestSupport.adminToken(mockMvc, objectMapper, pendingRegistrationRepository, jdbc);
    }

    protected UUID userId(String email) {
        return jdbc.queryForObject("SELECT id FROM users WHERE email = ?", UUID.class, email);
    }

    protected ResultActions call(HttpMethod method, String path, String token, Object body) throws Exception {
        var builder = request(method, path);
        if (token != null) {
            builder.header("Authorization", "Bearer " + token);
        }
        if (body != null) {
            builder.contentType(MediaType.APPLICATION_JSON).content(body instanceof String s ? s : objectMapper.writeValueAsString(body));
        }
        return mockMvc.perform(builder);
    }

    protected JsonNode data(ResultActions result) throws Exception {
        return objectMapper.readTree(result.andReturn().getResponse().getContentAsString()).get("data");
    }

    protected long auditCount(String action) {
        return jdbc.queryForObject("SELECT count(*) FROM plan_audit WHERE action = ?", Long.class, action);
    }

    protected long auditCount() {
        return jdbc.queryForObject("SELECT count(*) FROM plan_audit", Long.class);
    }

    protected UUID planId(String code) {
        return jdbc.queryForObject("SELECT id FROM plans WHERE code = ?", UUID.class, code);
    }

    /** Cria um plano pela API (como o administrador faria) e devolve o código. */
    protected String createPlan(String adminToken, String code, String name) throws Exception {
        call(HttpMethod.POST, "/api/admin/plans", adminToken, java.util.Map.of("code", code, "name", name))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isCreated());
        return code;
    }
}
