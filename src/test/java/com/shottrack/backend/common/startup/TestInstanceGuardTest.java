package com.shottrack.backend.common.startup;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Rodada 2: com APP_TEST_INSTANCE=true a subida falha se apontar para o banco de dev/E2E ou o vhost padrão. */
class TestInstanceGuardTest {

    private static MockEnvironment env(String testInstance, String url, String vhost) {
        MockEnvironment environment = new MockEnvironment().withProperty("spring.datasource.url", url);
        if (testInstance != null) {
            environment.withProperty("app.test-instance", testInstance);
        }
        if (vhost != null) {
            environment.withProperty("spring.rabbitmq.virtual-host", vhost);
        }
        return environment;
    }

    @Test
    void shouldRejectTheDevelopmentDatabaseOfATestInstance() {
        assertThatThrownBy(() -> TestInstanceGuard.check(env("true", "jdbc:postgresql://localhost:5432/shottrack", "shottrack_unit_test")))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("'shottrack'");
    }

    @Test
    void shouldRejectTheFrontendE2eDatabaseOfATestInstance() {
        assertThatThrownBy(() -> TestInstanceGuard.check(env("true", "jdbc:postgresql://localhost:5432/shottrack_test?ssl=false", "shottrack_unit_test")))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("'shottrack_test'");
    }

    @Test
    void shouldRejectTheDefaultRabbitVhostOfATestInstance() {
        assertThatThrownBy(() -> TestInstanceGuard.check(env("true", "jdbc:postgresql://localhost:5432/shottrack_unit_test", null)))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("vhost");
    }

    @Test
    void shouldAcceptTheDedicatedTestDatabaseAndVhost() {
        assertThatCode(() -> TestInstanceGuard.check(env("true", "jdbc:postgresql://localhost:5432/shottrack_unit_test", "shottrack_unit_test")))
                .doesNotThrowAnyException();
    }

    @Test
    void shouldDoNothingWhenTheInstanceIsNotMarkedAsTest() {
        assertThatCode(() -> TestInstanceGuard.check(env(null, "jdbc:postgresql://localhost:5432/shottrack", null)))
                .doesNotThrowAnyException();
        assertThatCode(() -> TestInstanceGuard.check(env("false", "jdbc:postgresql://localhost:5432/shottrack", null)))
                .doesNotThrowAnyException();
    }
}
