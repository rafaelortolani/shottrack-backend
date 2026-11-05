package com.shottrack.backend.application.weapon;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shottrack.backend.application.user.gateway.repository.PendingRegistrationRepository;
import com.shottrack.backend.support.TestUsers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.StreamSupport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** ADR-0004 (Revisão 3), V57/V58: integridade do catálogo de armas e ordem alfabética das listas do UC09. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class WeaponCatalogContentTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PendingRegistrationRepository pendingRegistrationRepository;

    @Autowired
    private JdbcTemplate jdbc;

    private String accessToken;

    @BeforeEach
    void registerAndLoginUser() throws Exception {
        accessToken = TestUsers.registerAndLogin(mockMvc, objectMapper, pendingRegistrationRepository,
                "Atleta Conteúdo", "atleta.conteudo.catalogo@shottrack.com", "senha12345");
    }

    @Test
    void shouldNotHaveDuplicatedTypesBrandsOrCalibersIgnoringCase() {
        for (String table : List.of("weapon_types", "weapon_brands", "weapon_calibers")) {
            assertThat(jdbc.queryForList("SELECT lower(name) FROM " + table + " GROUP BY lower(name) HAVING count(*) > 1", String.class))
                    .as("duplicados em " + table).isEmpty();
        }
    }

    @Test
    void shouldNotHaveTheSameModelTwiceInABrandIgnoringCase() {
        assertThat(jdbc.queryForList("""
                SELECT brand_id || ':' || lower(name) FROM weapon_models
                GROUP BY brand_id, lower(name) HAVING count(*) > 1""", String.class)).isEmpty();
    }

    @Test
    void shouldGiveEveryModelATypeAndAtLeastOneCaliber() {
        assertThat(jdbc.queryForList("SELECT name FROM weapon_models WHERE weapon_type_id IS NULL", String.class))
                .as("modelos sem tipo").isEmpty();
        assertThat(jdbc.queryForList("""
                SELECT m.name FROM weapon_models m
                WHERE NOT EXISTS (SELECT 1 FROM weapon_model_calibers x WHERE x.weapon_model_id = m.id)""", String.class))
                .as("modelos sem calibre").isEmpty();
    }

    @Test
    void shouldHaveTheRifleTypeAndTheNewModels() {
        assertThat(types()).contains("Rifle", "Fuzil", "Espingarda", "Pistola", "Revólver");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM weapon_models", Integer.class)).isGreaterThanOrEqualTo(152);
    }

    @Test
    void shouldGiveTheExpectedTypeAndCalibersToSampleModels() {
        assertSample("Taurus", "G3", "Pistola", "9mm", ".38 TPC");
        assertSample("Taurus", "Judge", "Revólver", ".45 Colt", ".410");
        assertSample("Boito", "B-300", "Espingarda", "28", "32", ".410");
        assertSample("Taurus", "T4 Executive Grade", "Fuzil", "5.56mm NATO", ".223 Remington", ".300 BLK");
        assertSample("CBC", "Ranger", "Rifle", ".308 Winchester");
        assertSample("Taurus", "RM66", "Revólver", ".357 Magnum", ".38 Special");
    }

    @Test
    void shouldListTypesBrandsAndCalibersInAlphabeticalOrder() throws Exception {
        assertThat(names(json("/api/weapon-catalog/types"))).isSortedAccordingTo(String.CASE_INSENSITIVE_ORDER);
        assertThat(names(json("/api/weapon-catalog/brands"))).isSortedAccordingTo(String.CASE_INSENSITIVE_ORDER);
        assertThat(names(json("/api/weapon-catalog/calibers"))).isSortedAccordingTo(String.CASE_INSENSITIVE_ORDER);
    }

    @Test
    void shouldListModelsAndModelCalibersOfEveryBrandInAlphabeticalOrder() throws Exception {
        for (JsonNode brand : json("/api/weapon-catalog/brands")) {
            JsonNode models = json("/api/weapon-catalog/brands/" + brand.get("id").asText() + "/models");
            assertThat(names(models)).as("modelos de " + brand.get("name").asText()).isSortedAccordingTo(String.CASE_INSENSITIVE_ORDER);
            for (JsonNode model : models) {
                assertThat(names(json("/api/weapon-catalog/models/" + model.get("id").asText() + "/calibers")))
                        .as("calibres de " + model.get("name").asText()).isSortedAccordingTo(String.CASE_INSENSITIVE_ORDER);
            }
        }
    }

    private void assertSample(String brand, String model, String type, String... calibers) {
        assertThat(jdbc.queryForList("""
                SELECT t.name FROM weapon_models m JOIN weapon_brands b ON b.id = m.brand_id
                JOIN weapon_types t ON t.id = m.weapon_type_id WHERE b.name = ? AND m.name = ?""", String.class, brand, model))
                .as("tipo de " + brand + " " + model).containsExactly(type);
        assertThat(jdbc.queryForList("""
                SELECT c.name FROM weapon_models m JOIN weapon_brands b ON b.id = m.brand_id
                JOIN weapon_model_calibers x ON x.weapon_model_id = m.id JOIN weapon_calibers c ON c.id = x.weapon_caliber_id
                WHERE b.name = ? AND m.name = ?""", String.class, brand, model))
                .as("calibres de " + brand + " " + model).contains(calibers);
    }

    private List<String> types() {
        return jdbc.queryForList("SELECT name FROM weapon_types", String.class);
    }

    private JsonNode json(String path) throws Exception {
        String body = mockMvc.perform(get(path).header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("data");
    }

    private static List<String> names(JsonNode array) {
        List<String> names = new ArrayList<>();
        StreamSupport.stream(array.spliterator(), false).forEach(n -> names.add(n.get("name").asText()));
        return names;
    }
}
