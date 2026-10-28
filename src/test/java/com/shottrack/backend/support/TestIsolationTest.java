package com.shottrack.backend.support;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

/** Lote 0 (Q06): o contexto de teste real está ligado ao banco e ao vhost dedicados. */
@SpringBootTest
class TestIsolationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Value("${spring.rabbitmq.virtual-host}")
    private String rabbitVhost;

    @Test
    void shouldRunAgainstTheDedicatedTestDatabase() {
        String database = jdbcTemplate.queryForObject("SELECT current_database()", String.class);

        assertThat(database).isNotIn("shottrack", "shottrack_test").endsWith("_test");
    }

    @Test
    void shouldRunAgainstTheDedicatedRabbitVhost() {
        assertThat(rabbitVhost).isNotEqualTo("/");
    }
}
