package com.shottrack.backend.application.habituality;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shottrack.backend.application.user.gateway.repository.PendingRegistrationRepository;
import com.shottrack.backend.support.TestUsers;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Base dos testes de habitualidade (UC54–UC58, ADR-0023): um administrador e um atleta
 * prontos, mais helpers para montar regras, versões, locais e visitas. O tempo "passa"
 * mexendo em valid_from / created_at direto no banco (o projeto usa o relógio real, UTC).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
abstract class HabitualityIntegrationTest {

    protected static final String PASSWORD = "senha12345";
    protected static final String ADMIN_EMAIL = "admin.habitualidade@shottrack.com";

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected PendingRegistrationRepository pendingRegistrationRepository;

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    @Autowired
    protected EntityManager entityManager;

    protected String adminToken;
    protected String athleteToken;

    @BeforeEach
    void registerAdminAndAthlete() throws Exception {
        adminToken = registerAndLogin(ADMIN_EMAIL);
        entityManager.flush();
        jdbcTemplate.update("UPDATE users SET role = 'ADMIN' WHERE email = ?", ADMIN_EMAIL);
        entityManager.clear();
        athleteToken = registerAndLogin("atleta.habitualidade@shottrack.com");
    }

    protected String registerAndLogin(String email) throws Exception {
        return TestUsers.registerAndLogin(mockMvc, objectMapper, pendingRegistrationRepository, "Atleta", email, PASSWORD);
    }

    protected static LocalDate today() {
        return LocalDate.now(ZoneOffset.UTC);
    }

    protected void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }

    // ---- requisições ----

    protected ResultActions send(String method, String path, String token, Object body) throws Exception {
        var builder = switch (method) {
            case "GET" -> get(path);
            case "POST" -> post(path);
            case "PUT" -> put(path);
            case "PATCH" -> patch(path);
            default -> delete(path);
        };
        if (token != null) {
            builder.header("Authorization", "Bearer " + token);
        }
        if (body != null) {
            builder.contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(body));
        }
        return mockMvc.perform(builder);
    }

    protected JsonNode data(ResultActions result) throws Exception {
        return objectMapper.readTree(result.andReturn().getResponse().getContentAsString()).get("data");
    }

    protected UUID idOf(ResultActions result) throws Exception {
        return UUID.fromString(data(result.andExpect(status().is2xxSuccessful())).get("id").asText());
    }

    // ---- admin: regras e versões ----

    protected Map<String, Object> ruleBody(String name, boolean active) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("name", name);
        body.put("legalReference", "Norma de teste");
        body.put("active", active);
        return body;
    }

    protected UUID createRule(String name) throws Exception {
        return idOf(send("POST", "/api/admin/habituality/rules", adminToken, ruleBody(name, true)));
    }

    protected static Map<String, Object> req(String activityType, int minQuantity) {
        return req(activityType, minQuantity, false, false, null);
    }

    protected static Map<String, Object> req(String activityType, int minQuantity, boolean onlyInClub,
                                             boolean distinctEvents, String minScope) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("activityType", activityType);
        body.put("minQuantity", minQuantity);
        body.put("onlyInClub", onlyInClub);
        body.put("distinctEvents", distinctEvents);
        body.put("minScope", minScope);
        return body;
    }

    @SafeVarargs
    protected static Map<String, Object> level(String name, int position, Map<String, Object>... requirements) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("name", name);
        body.put("position", position);
        body.put("requirements", List.of(requirements));
        return body;
    }

    @SafeVarargs
    protected static Map<String, Object> version(LocalDate validFrom, int windowMonths, Map<String, Object>... levels) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("validFrom", validFrom.toString());
        body.put("windowMonths", windowMonths);
        body.put("levels", List.of(levels));
        return body;
    }

    protected ResultActions postVersion(UUID ruleId, Map<String, Object> body) throws Exception {
        return send("POST", "/api/admin/habituality/rules/" + ruleId + "/versions", adminToken, body);
    }

    protected UUID createVersion(UUID ruleId, Map<String, Object> body) throws Exception {
        return idOf(postVersion(ruleId, body));
    }

    /** Regra ativa com uma única versão já em vigor (1º de janeiro de 2020) e janela de 12 meses. */
    @SafeVarargs
    protected final UUID ruleWithLevel(String ruleName, String levelName, Map<String, Object>... requirements) throws Exception {
        UUID ruleId = createRule(ruleName);
        createVersion(ruleId, version(LocalDate.of(2020, 1, 1), 12, level(levelName, 1, requirements)));
        return ruleId;
    }

    protected void setValidFrom(UUID versionId, LocalDate validFrom) {
        flushAndClear();
        jdbcTemplate.update("UPDATE habituality_rule_versions SET valid_from = ? WHERE id = ?",
                java.sql.Date.valueOf(validFrom), versionId);
        entityManager.clear();
    }

    // ---- atleta ----

    protected ResultActions track(String token, UUID ruleId, String levelName) throws Exception {
        return send("PUT", "/api/habituality/tracking", token, Map.of("ruleId", ruleId, "levelName", levelName));
    }

    protected ResultActions progress(String token) throws Exception {
        return send("GET", "/api/habituality/progress", token, null);
    }

    protected UUID location(String token, boolean club) throws Exception {
        return idOf(send("POST", "/api/training-locations", token,
                Map.of("name", "Local " + UUID.randomUUID(), "city", "São Paulo", "state", "SP", "club", club)));
    }

    /** Visita encerrada, com início (UTC, meio-dia) em {@code day}. */
    protected UUID closedVisit(String token, UUID locationId, String visitType, String scope, LocalDate day) throws Exception {
        UUID visitId = openVisit(token, locationId, visitType, scope);
        send("POST", "/api/visits/" + visitId + "/closure", token, null).andExpect(status().isOk());
        backdateVisit(visitId, day);
        return visitId;
    }

    protected UUID openVisit(String token, UUID locationId, String visitType, String scope) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("trainingLocationId", locationId);
        body.put("visitType", visitType);
        body.put("competitionScope", scope);
        return idOf(send("POST", "/api/visits", token, body));
    }

    protected void backdateVisit(UUID visitId, LocalDate day) {
        flushAndClear();
        entityManager.createNativeQuery("UPDATE visits SET started_at = :createdAt WHERE id = :id")
                .setParameter("createdAt", day.atTime(LocalTime.NOON).toInstant(ZoneOffset.UTC))
                .setParameter("id", visitId)
                .executeUpdate();
        entityManager.clear();
    }
}
