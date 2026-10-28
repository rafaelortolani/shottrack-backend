package com.shottrack.backend.support;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Lote 0: a trava recusa os bancos/vhost de desenvolvimento e E2E e aceita o de testes. */
class TestDatabaseGuardTest {

    private static MockEnvironment env(String url, String vhost) {
        MockEnvironment environment = new MockEnvironment().withProperty("spring.datasource.url", url);
        if (vhost != null) {
            environment.withProperty("spring.rabbitmq.virtual-host", vhost);
        }
        return environment;
    }

    @Test
    void shouldRejectTheDevelopmentDatabase() {
        assertThatThrownBy(() -> TestDatabaseGuard.check(env("jdbc:postgresql://localhost:5432/shottrack", "shottrack_unit_test")))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("'shottrack'");
    }

    @Test
    void shouldRejectTheFrontendE2eDatabase() {
        assertThatThrownBy(() -> TestDatabaseGuard.check(env("jdbc:postgresql://localhost:5432/shottrack_test?ssl=false", "shottrack_unit_test")))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("'shottrack_test'");
    }

    @Test
    void shouldRejectTheDefaultRabbitVhost() {
        assertThatThrownBy(() -> TestDatabaseGuard.check(env("jdbc:postgresql://localhost:5432/shottrack_unit_test", null)))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("vhost");
        assertThatThrownBy(() -> TestDatabaseGuard.check(env("jdbc:postgresql://localhost:5432/shottrack_unit_test", "/")))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void shouldAcceptTheDedicatedTestDatabaseAndVhost() {
        assertThatCode(() -> TestDatabaseGuard.check(env("jdbc:postgresql://localhost:5432/shottrack_unit_test", "shottrack_unit_test")))
                .doesNotThrowAnyException();
    }
}
