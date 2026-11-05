package com.shottrack.backend.application.plan;

import com.fasterxml.jackson.databind.JsonNode;
import com.shottrack.backend.support.PlanApiTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** UC68: criar plano. */
class PlanCreateTest extends PlanApiTest {

    private static final String PATH = "/api/admin/plans";

    private Map<String, Object> plan(String code, String name) {
        Map<String, Object> body = new HashMap<>();
        body.put("code", code);
        body.put("name", name);
        return body;
    }

    private long planCount() {
        return jdbc.queryForObject("SELECT count(*) FROM plans", Long.class);
    }

    @Test
    void shouldCreateAnActiveNonDefaultPlanWithoutStoredLimits() throws Exception {
        JsonNode created = data(call(HttpMethod.POST, PATH, admin(), plan("PAID", "Pago")).andExpect(status().isCreated()));

        assertThat(created.get("code").asText()).isEqualTo("PAID");
        assertThat(created.get("active").asBoolean()).isTrue();
        assertThat(created.get("default").asBoolean()).isFalse();
        assertThat(created.get("athleteCount").asLong()).isZero();
        assertThat(PlanListTest.limit(created, "PHOTOS_PER_SERIES").get("value").asLong()).as("usa o padrão do enum").isEqualTo(5);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM plan_limits WHERE plan_id = ?", Long.class, planId("PAID"))).isZero();
        assertThat(auditCount("PLAN_CREATED")).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT target_plan_code FROM plan_audit WHERE action = 'PLAN_CREATED'", String.class)).isEqualTo("PAID");
    }

    @Test
    void shouldCopyTheLimitsOfAnotherPlan() throws Exception {
        String admin = admin();
        call(HttpMethod.PATCH, "/api/admin/plans/FREE/limits", admin, Map.of("limits", Map.of("PHOTOS_PER_SERIES", 9)));
        Map<String, Object> body = plan("PAID", "Pago");
        body.put("copyFromCode", "FREE");

        JsonNode created = data(call(HttpMethod.POST, PATH, admin, body).andExpect(status().isCreated()));

        assertThat(PlanListTest.limit(created, "PHOTOS_PER_SERIES").get("value").asLong()).isEqualTo(9);
        assertThat(PlanListTest.limit(created, "UPLOADS_PER_HOUR").get("value").asLong()).isEqualTo(30);
    }

    @Test
    void shouldTrimTheNameAndAcceptTheBoundaries() throws Exception {
        String admin = admin();

        JsonNode trimmed = data(call(HttpMethod.POST, PATH, admin, plan("AB", "  Beta  ")).andExpect(status().isCreated()));
        assertThat(trimmed.get("name").asText()).isEqualTo("Beta");
        call(HttpMethod.POST, PATH, admin, plan("A" + "B".repeat(28) + "9", "N".repeat(60))).andExpect(status().isCreated());
    }

    @Test
    void shouldRefuseACodeOutOfTheFormat() throws Exception {
        String admin = admin();
        for (String code : new String[]{"pago", "1PAGO", "P", "A" + "B".repeat(30), "PA GO", "PAGO-1", "PAGÓ", "", " "}) {
            call(HttpMethod.POST, PATH, admin, plan(code, "Plano " + code.length()))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error.code").value("PLAN_CODE_INVALID"));
        }
        call(HttpMethod.POST, PATH, admin, plan(null, "Sem código")).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("PLAN_CODE_INVALID"));
        assertThat(planCount()).isEqualTo(1);
    }

    @Test
    void shouldRefuseADuplicatedCodeIgnoringCase() throws Exception {
        String admin = admin();
        createPlan(admin, "PAID", "Pago");

        call(HttpMethod.POST, PATH, admin, plan("PAID", "Outro nome"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.error.code").value("PLAN_CODE_ALREADY_EXISTS"));
        // código só aceita maiúsculas (formato), então a variante de caixa diferente é recusada pelo formato — e o banco também recusa
        call(HttpMethod.POST, PATH, admin, plan("Paid", "Outro nome 2")).andExpect(status().isBadRequest());
        assertThat(planCount()).isEqualTo(2);
        // o banco também recusa a variante de caixa (último passo: a violação aborta a transação do teste)
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> jdbc.update("INSERT INTO plans (code, name) VALUES ('paid', 'Variante')"))
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
    }

    @Test
    void shouldRefuseADuplicatedNameIgnoringCase() throws Exception {
        String admin = admin();

        call(HttpMethod.POST, PATH, admin, plan("OTHER", "gratuito"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.error.code").value("PLAN_NAME_ALREADY_EXISTS"));
    }

    @Test
    void shouldRefuseAnInvalidName() throws Exception {
        String admin = admin();
        for (String name : new String[]{"A", "   ", "", "N".repeat(61)}) {
            call(HttpMethod.POST, PATH, admin, plan("PLANO", name))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error.code").value("PLAN_NAME_INVALID"));
        }
        call(HttpMethod.POST, PATH, admin, plan("PLANO", null)).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("PLAN_NAME_INVALID"));
    }

    @Test
    void shouldRefuseACopyFromAPlanThatDoesNotExist() throws Exception {
        Map<String, Object> body = plan("PAID", "Pago");
        body.put("copyFromCode", "NOPE");

        call(HttpMethod.POST, PATH, admin(), body)
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.error.code").value("PLAN_NOT_FOUND"));
        assertThat(planCount()).isEqualTo(1);
        assertThat(auditCount()).isZero();
    }

    @Test
    void shouldRefuseThe21stPlan() throws Exception {
        String admin = admin();
        for (int i = (int) planCount(); i < 20; i++) {
            createPlan(admin, "PLANO_" + i, "Plano numero " + i);
        }
        assertThat(planCount()).isEqualTo(20);

        call(HttpMethod.POST, PATH, admin, plan("PLANO_21", "Plano vinte e um"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.error.code").value("PLAN_COUNT_LIMIT_REACHED"));
        assertThat(planCount()).isEqualTo(20);
    }

    @Test
    void shouldWriteNothingOnError() throws Exception {
        call(HttpMethod.POST, PATH, admin(), plan("OTHER", "gratuito")).andExpect(status().isConflict());

        assertThat(planCount()).isEqualTo(1);
        assertThat(auditCount()).isZero();
    }

    @Test
    void shouldRefuseARequestWithoutToken() throws Exception {
        call(HttpMethod.POST, PATH, null, plan("PAID", "Pago")).andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRefuseAnAthleteWhoIsNotAdmin() throws Exception {
        call(HttpMethod.POST, PATH, athlete(), plan("PAID", "Pago")).andExpect(status().isForbidden());
        assertThat(planCount()).isEqualTo(1);
    }

    @Test
    void shouldNotExposeAnyRouteToDeleteAPlan() throws Exception {
        String admin = admin();
        call(HttpMethod.DELETE, "/api/admin/plans/FREE", admin, null).andExpect(status().is4xxClientError());
        assertThat(planCount()).isEqualTo(1);
    }
}
