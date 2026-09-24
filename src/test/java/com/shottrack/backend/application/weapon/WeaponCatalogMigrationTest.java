package com.shottrack.backend.application.weapon;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ADR-0004 (Revisão 2): a reestruturação do catálogo (V42 estrutura, V43
 * seed) precisa ajustar armas cadastradas antes dela sem invalidar nenhuma.
 * O banco dos testes já está migrado, então este teste recria o cenário
 * num schema isolado: migra até a V41, insere armas legadas incoerentes,
 * aplica V42+V43 e confere o resultado. O schema é apagado no fim — nada
 * fica no banco.
 */
@SpringBootTest
class WeaponCatalogMigrationTest {

    private static final String SCHEMA = "weapon_catalog_migration_test";

    @Autowired
    private DataSource dataSource;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    @AfterEach
    void dropSchema() {
        jdbcTemplate.execute("DROP SCHEMA IF EXISTS " + SCHEMA + " CASCADE");
    }

    @Test
    void shouldFixLegacyWeaponsAndKeepTheirModelCaliberCombinationsValid() {
        migrateTo("41");

        UUID userId = insertUser();
        // tipo errado (856 é revólver) e calibre que o modelo não usa de verdade
        UUID wrongTypeWeapon = insertWeapon(userId, "Pistola", "Taurus", "856", ".22 LR");
        // cadastro já coerente — não pode mudar
        UUID coherentWeapon = insertWeapon(userId, "Pistola", "Glock", "G17", "9mm");
        // calibre com nome antigo (V7) — renomeado, e a arma continua nele
        UUID renamedCaliberWeapon = insertWeapon(userId, "Revólver", "Taurus", "856", ".38 SPL");

        migrateTo("43");

        // (a) tipo da arma passa a ser o do modelo
        assertThat(weaponTypeName(wrongTypeWeapon)).isEqualTo("Revólver");
        assertThat(weaponTypeName(coherentWeapon)).isEqualTo("Pistola");
        assertThat(weaponCaliberName(renamedCaliberWeapon)).isEqualTo(".38 Special");

        // (b) combinação legada continua válida, junto com as da referência
        assertThat(allowedCalibers("Taurus", "856")).containsExactlyInAnyOrder(".22 LR", ".38 Special");
        assertThat(allowedCalibers("Glock", "G17")).containsExactly("9mm");

        // sobras da V7 fora da referência, sem uso, saem do catálogo
        assertThat(count("weapon_brands WHERE name = 'Rossi'")).isZero();
        assertThat(count("weapon_types WHERE name = 'Outro'")).isZero();
        assertThat(count("weapon_calibers WHERE name IN ('.38 SPL', '.223/5.56', '12 Gauge')")).isZero();

        // todo modelo tem tipo (a coluna vira NOT NULL no fim da V43)
        assertThat(count("weapon_models WHERE weapon_type_id IS NULL")).isZero();
    }

    private void migrateTo(String version) {
        Flyway.configure()
                .dataSource(dataSource)
                .schemas(SCHEMA)
                .defaultSchema(SCHEMA)
                .target(version)
                .load()
                .migrate();
    }

    private UUID insertUser() {
        return jdbcTemplate.queryForObject(
                "INSERT INTO " + SCHEMA + ".users (name, email, password_hash) VALUES ('Atleta Legado', 'atleta.legado@shottrack.com', 'hash') RETURNING id",
                UUID.class);
    }

    private UUID insertWeapon(UUID userId, String typeName, String brandName, String modelName, String caliberName) {
        return jdbcTemplate.queryForObject("""
                INSERT INTO %1$s.weapons (user_id, type_id, brand_id, model_id, caliber_id)
                SELECT ?, t.id, b.id, m.id, c.id
                FROM %1$s.weapon_types t, %1$s.weapon_brands b, %1$s.weapon_models m, %1$s.weapon_calibers c
                WHERE t.name = ? AND b.name = ? AND m.brand_id = b.id AND m.name = ? AND c.name = ?
                RETURNING id
                """.formatted(SCHEMA), UUID.class, userId, typeName, brandName, modelName, caliberName);
    }

    private String weaponTypeName(UUID weaponId) {
        return jdbcTemplate.queryForObject("""
                SELECT t.name FROM %1$s.weapons w JOIN %1$s.weapon_types t ON t.id = w.type_id WHERE w.id = ?
                """.formatted(SCHEMA), String.class, weaponId);
    }

    private String weaponCaliberName(UUID weaponId) {
        return jdbcTemplate.queryForObject("""
                SELECT c.name FROM %1$s.weapons w JOIN %1$s.weapon_calibers c ON c.id = w.caliber_id WHERE w.id = ?
                """.formatted(SCHEMA), String.class, weaponId);
    }

    private List<String> allowedCalibers(String brandName, String modelName) {
        return jdbcTemplate.queryForList("""
                SELECT c.name
                FROM %1$s.weapon_model_calibers mc
                JOIN %1$s.weapon_models m ON m.id = mc.weapon_model_id
                JOIN %1$s.weapon_brands b ON b.id = m.brand_id
                JOIN %1$s.weapon_calibers c ON c.id = mc.weapon_caliber_id
                WHERE b.name = ? AND m.name = ?
                """.formatted(SCHEMA), String.class, brandName, modelName);
    }

    private int count(String fromWhere) {
        return jdbcTemplate.queryForObject("SELECT count(*) FROM " + SCHEMA + "." + fromWhere, Integer.class);
    }
}
