package com.shottrack.backend.application.weapon;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shottrack.backend.application.user.gateway.repository.PendingRegistrationRepository;
import com.shottrack.backend.support.SqlRecorder;
import com.shottrack.backend.support.TestUsers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * UC09: a lista de modelos de uma marca traz o tipo de cada modelo com UMA consulta de tipos, seja a marca de 1 ou de 62 modelos.
 * Conta os comandos SQL de verdade (inspetor de comandos do Hibernate, ligado só neste teste).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@TestPropertySource(properties = "spring.jpa.properties.hibernate.session_factory.statement_inspector=com.shottrack.backend.support.SqlRecorder")
class WeaponCatalogQueriesTest {

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
                "Atleta Consultas", "atleta.consultas.catalogo@shottrack.com", "senha12345");
    }

    @Test
    void shouldLoadTheTypesWithASingleQueryRegardlessOfTheNumberOfModels() throws Exception {
        long[] taurus = listModelsAndCount("Taurus");
        long[] glock = listModelsAndCount("Glock");

        assertThat(taurus[2]).as("modelos da Taurus").isGreaterThan(50);
        assertThat(taurus[0]).as("consultas em weapon_types (Taurus)").isEqualTo(1);
        assertThat(glock[0]).as("consultas em weapon_types (Glock)").isEqualTo(1);
        assertThat(taurus[1]).as("total de comandos SQL não cresce com o número de modelos").isEqualTo(glock[1]);
    }

    /** Devolve {comandos em weapon_types, comandos no total, modelos devolvidos}. */
    private long[] listModelsAndCount(String brand) throws Exception {
        String brandId = jdbc.queryForObject("SELECT id::text FROM weapon_brands WHERE name = ?", String.class, brand);
        SqlRecorder.clear();
        String body = mockMvc.perform(get("/api/weapon-catalog/brands/" + brandId + "/models").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        JsonNode models = objectMapper.readTree(body).get("data");
        return new long[]{SqlRecorder.countContaining("weapon_types"), SqlRecorder.statements().size(), models.size()};
    }
}
