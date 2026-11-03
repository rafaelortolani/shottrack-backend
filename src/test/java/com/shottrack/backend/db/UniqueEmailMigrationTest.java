package com.shottrack.backend.db;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * S15: a V54 (índice único em lower(email)) ABORTA com mensagem clara se já houver e-mails duplicados por caixa, e
 * quando passa deixa os e-mails em minúsculas e o índice criado. Roda num schema próprio do banco de teste
 * (migrations até a V53, dados de teste, depois a V54) e o apaga no fim — não toca no schema do resto da suíte.
 */
class UniqueEmailMigrationTest {

    private static final String SCHEMA = "v54_migration_test";

    private DriverManagerDataSource dataSource;
    private JdbcTemplate jdbc;

    @BeforeEach
    void migrateUpToV53() {
        dataSource = new DriverManagerDataSource(
                System.getenv().getOrDefault("TEST_DB_URL", "jdbc:postgresql://localhost:5432/shottrack_unit_test"),
                "shottrack", "shottrack");
        jdbc = new JdbcTemplate(dataSource);
        jdbc.execute("DROP SCHEMA IF EXISTS " + SCHEMA + " CASCADE");
        flywayUpTo("53").migrate();
    }

    @AfterEach
    void dropSchema() {
        jdbc.execute("DROP SCHEMA IF EXISTS " + SCHEMA + " CASCADE");
    }

    private Flyway flywayUpTo(String version) {
        return Flyway.configure().dataSource(dataSource).schemas(SCHEMA).defaultSchema(SCHEMA)
                .locations("classpath:db/migration").target(version).load();
    }

    private void insertUser(String email) {
        jdbc.update("INSERT INTO " + SCHEMA + ".users (name, email, password_hash) VALUES ('Atleta', ?, 'hash')", email);
    }

    @Test
    void shouldAbortListingTheDuplicatedEmailsAndLeaveTheSchemaUntouched() {
        insertUser("Foo@Exemplo.com");
        insertUser("foo@exemplo.com ");
        insertUser("unico@exemplo.com");

        assertThatThrownBy(() -> flywayUpTo("54").migrate())
                .isInstanceOf(FlywayException.class)
                .hasMessageContaining("V54 abortada")
                .hasMessageContaining("foo@exemplo.com (2 contas)")
                .hasMessageNotContaining("unico@exemplo.com");

        assertThat(jdbc.queryForObject("SELECT count(*) FROM " + SCHEMA + ".users", Integer.class)).isEqualTo(3);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM pg_indexes WHERE schemaname = ? AND indexname = 'uq_users_email_lower'",
                Integer.class, SCHEMA)).isZero();
    }

    @Test
    void shouldLowercaseExistingEmailsAndCreateTheUniqueIndexWhenThereAreNoDuplicates() {
        insertUser("Maria@Exemplo.COM");
        insertUser("outro@exemplo.com");

        flywayUpTo("54").migrate();

        assertThat(jdbc.queryForList("SELECT email FROM " + SCHEMA + ".users ORDER BY email", String.class))
                .containsExactly("maria@exemplo.com", "outro@exemplo.com");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM pg_indexes WHERE schemaname = ? AND indexname = 'uq_users_email_lower'",
                Integer.class, SCHEMA)).isEqualTo(1);
        assertThatThrownBy(() -> insertUser("MARIA@exemplo.com")).hasMessageContaining("uq_users_email_lower");
    }
}
