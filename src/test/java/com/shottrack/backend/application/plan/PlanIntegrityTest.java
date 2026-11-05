package com.shottrack.backend.application.plan;

import com.shottrack.backend.application.plan.usecase.PlanIntegrityVerifier;
import com.shottrack.backend.support.PlanApiTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** ADR-0027 / UC23: o banco nasce com o plano Gratuito, todo atleta tem plano e sempre existe exatamente um padrão ativo. */
class PlanIntegrityTest extends PlanApiTest {

    @Autowired
    private PlanIntegrityVerifier verifier;

    @Test
    void shouldSeedTheActiveDefaultFreePlanWithTheInitialLimits() {
        assertThat(jdbc.queryForMap("SELECT code, name, active, is_default FROM plans WHERE code = 'FREE'"))
                .containsEntry("name", "Gratuito").containsEntry("active", true).containsEntry("is_default", true);
        assertThat(jdbc.queryForMap("SELECT key, value FROM (SELECT limit_key AS key, value FROM plan_limits) l WHERE key = 'PHOTOS_PER_SERIES'")).containsEntry("value", 5L);
        assertThat(jdbc.queryForList("SELECT limit_key FROM plan_limits WHERE plan_id = (SELECT id FROM plans WHERE code = 'FREE')", String.class))
                .containsExactlyInAnyOrder("PHOTOS_PER_SERIES", "PHOTOS_PER_ATHLETE", "STORAGE_BYTES_PER_ATHLETE", "UPLOADS_PER_HOUR");
        assertThat(jdbc.queryForObject("SELECT value FROM plan_limits WHERE limit_key = 'STORAGE_BYTES_PER_ATHLETE'", Long.class)).isEqualTo(536_870_912L);
    }

    @Test
    void shouldNeverLeaveAnAthleteWithoutAPlan() {
        assertThat(jdbc.queryForObject("SELECT count(*) FROM users WHERE plan_id IS NULL", Long.class)).isZero();
        assertThatThrownBy(() -> jdbc.update("UPDATE users SET plan_id = NULL WHERE id = (SELECT id FROM users LIMIT 1)"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void shouldGiveANewAthleteTheDefaultPlan() throws Exception {
        String email = athleteEmail("plano.padrao");
        athlete(email);

        assertThat(jdbc.queryForObject("SELECT p.code FROM users u JOIN plans p ON p.id = u.plan_id WHERE u.email = ?", String.class, email)).isEqualTo("FREE");
    }

    @Test
    void shouldPassWhenThereIsExactlyOneActiveDefault() {
        assertThatCode(verifier::verify).doesNotThrowAnyException();
        assertThat(verifier.defaultPlanId()).isEqualTo(planId("FREE"));
    }

    @Test
    void shouldFailClearlyWhenThereIsNoDefaultPlan() {
        jdbc.update("UPDATE plans SET is_default = FALSE");

        assertThatThrownBy(verifier::verify).isInstanceOf(IllegalStateException.class).hasMessageContaining("exatamente 1 plano padrão");
    }

    @Test
    void shouldRefuseASecondDefaultPlanInTheDatabase() {
        assertThatThrownBy(() -> jdbc.update("INSERT INTO plans (code, name, active, is_default) VALUES ('OUTRO', 'Outro', TRUE, TRUE)"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void shouldRefuseAnInactiveDefaultPlanInTheDatabase() {
        assertThatThrownBy(() -> jdbc.update("UPDATE plans SET active = FALSE WHERE code = 'FREE'"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
