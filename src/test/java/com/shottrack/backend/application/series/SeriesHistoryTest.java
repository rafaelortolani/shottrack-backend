package com.shottrack.backend.application.series;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shottrack.backend.application.series.dto.RegisterSeriesRequest;
import com.shottrack.backend.application.series.dto.RegisterSeriesResultRequest;
import com.shottrack.backend.application.user.gateway.repository.PendingRegistrationRepository;
import com.shottrack.backend.support.SeriesTestSupport;
import com.shottrack.backend.support.TestUsers;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * UC47: GET /api/series. Os testes de filtro usam o mesmo cenário
 * (Scenario) e identificam cada série pelo alvo (s1..s5):
 * - IPSC, 10 dias atrás: s1 (arma A, 10m), s2 (arma B, 25m)
 * - Precisão, 3 dias atrás: s3 (arma A, 25m), s4 (sem arma, sem distância)
 * - IPSC, 1 dia atrás: s5 (arma B, 50m)
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SeriesHistoryTest {

    private static final String PASSWORD = "senha12345";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PendingRegistrationRepository pendingRegistrationRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void shouldReturnAllSeriesFromMostRecentToOldestWhenNoFilterIsGiven() throws Exception {
        Scenario scenario = createScenario("atleta.historicosemfiltro@shottrack.com");

        history(scenario.token())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[*].target").value(contains("s5", "s4", "s3", "s2", "s1")));
    }

    @Test
    void shouldReturnEachSeriesWithFullContext() throws Exception {
        String token = registerAndLogin("atleta.historicocontexto@shottrack.com");
        UUID weaponId = SeriesTestSupport.registerWeapon(mockMvc, objectMapper, token);
        UUID ammunitionId = SeriesTestSupport.registerAmmunition(mockMvc, objectMapper, token);
        UUID tempoId = resultTypeIdByName(token, "Tempo");
        UUID trainingId = SeriesTestSupport.openTraining(mockMvc, objectMapper, token, "IPSC");
        UUID seriesId = registerSeries(token, new RegisterSeriesRequest(
                trainingId, weaponId, ammunitionId, new BigDecimal("15"), "IPSC Target", 12, "vento forte"));
        registerResult(token, seriesId, tempoId, "8.5");
        SeriesTestSupport.backdateTraining(entityManager, trainingId, daysAgo(2));

        history(token)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].id").value(seriesId.toString()))
                .andExpect(jsonPath("$.data[0].trainingId").value(trainingId.toString()))
                .andExpect(jsonPath("$.data[0].date").value(daysAgo(2).toString()))
                .andExpect(jsonPath("$.data[0].trainingLocation.name").value("Clube de Tiro Central"))
                .andExpect(jsonPath("$.data[0].modality.name").value("IPSC"))
                .andExpect(jsonPath("$.data[0].weapon.id").value(weaponId.toString()))
                .andExpect(jsonPath("$.data[0].weapon.model.name").value("G17"))
                .andExpect(jsonPath("$.data[0].ammunition.id").value(ammunitionId.toString()))
                .andExpect(jsonPath("$.data[0].ammunition.nickname").value("Munição de teste"))
                .andExpect(jsonPath("$.data[0].distanceMeters").value(15))
                .andExpect(jsonPath("$.data[0].target").value("IPSC Target"))
                .andExpect(jsonPath("$.data[0].shotCount").value(12))
                .andExpect(jsonPath("$.data[0].notes").value("vento forte"))
                .andExpect(jsonPath("$.data[0].results.length()").value(1))
                .andExpect(jsonPath("$.data[0].results[0].resultTypeName").value("Tempo"))
                .andExpect(jsonPath("$.data[0].results[0].value").value("8.5"));
    }

    @Test
    void shouldFilterByPeriod() throws Exception {
        Scenario scenario = createScenario("atleta.historicoperiodo@shottrack.com");

        history(scenario.token(), "startDate", daysAgo(5).toString(), "endDate", daysAgo(2).toString())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[*].target").value(contains("s4", "s3")));
        // pontas inclusivas, e cada ponta funciona sozinha
        history(scenario.token(), "startDate", daysAgo(3).toString())
                .andExpect(jsonPath("$.data[*].target").value(contains("s5", "s4", "s3")));
        history(scenario.token(), "endDate", daysAgo(3).toString())
                .andExpect(jsonPath("$.data[*].target").value(contains("s4", "s3", "s2", "s1")));
    }

    @Test
    void shouldFilterByModality() throws Exception {
        Scenario scenario = createScenario("atleta.historicomodalidade@shottrack.com");

        history(scenario.token(), "modalityId", modalityIdByName(scenario.token(), "IPSC").toString())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[*].target").value(contains("s5", "s2", "s1")));
    }

    @Test
    void shouldFilterByWeapon() throws Exception {
        Scenario scenario = createScenario("atleta.historicoarma@shottrack.com");

        history(scenario.token(), "weaponId", scenario.weaponA().toString())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[*].target").value(contains("s3", "s1")));
    }

    @Test
    void shouldFilterByDistance() throws Exception {
        Scenario scenario = createScenario("atleta.historicodistancia@shottrack.com");

        // série sem distância (s4) nunca entra quando há filtro de distância
        history(scenario.token(), "minDistanceMeters", "25")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[*].target").value(contains("s5", "s3", "s2")));
        history(scenario.token(), "maxDistanceMeters", "25")
                .andExpect(jsonPath("$.data[*].target").value(contains("s3", "s2", "s1")));
        history(scenario.token(), "minDistanceMeters", "20", "maxDistanceMeters", "30")
                .andExpect(jsonPath("$.data[*].target").value(contains("s3", "s2")));
    }

    @Test
    void shouldCombineFilters() throws Exception {
        Scenario scenario = createScenario("atleta.historicocombinado@shottrack.com");
        String ipscId = modalityIdByName(scenario.token(), "IPSC").toString();

        history(scenario.token(), "modalityId", ipscId, "weaponId", scenario.weaponB().toString())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[*].target").value(contains("s5", "s2")));
        history(scenario.token(), "modalityId", ipscId, "weaponId", scenario.weaponB().toString(),
                "maxDistanceMeters", "30")
                .andExpect(jsonPath("$.data[*].target").value(contains("s2")));
        history(scenario.token(), "weaponId", scenario.weaponA().toString(),
                "startDate", daysAgo(5).toString(), "minDistanceMeters", "20")
                .andExpect(jsonPath("$.data[*].target").value(contains("s3")));
    }

    @Test
    void shouldReturnEmptyListWhenNoSeriesMatchesFilters() throws Exception {
        Scenario scenario = createScenario("atleta.historicovazio@shottrack.com");

        history(scenario.token(), "modalityId", modalityIdByName(scenario.token(), "IPSC").toString(),
                "weaponId", scenario.weaponA().toString(), "minDistanceMeters", "50")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    void shouldReturnEmptyListWhenAthleteHasNoSeries() throws Exception {
        String token = registerAndLogin("atleta.historicosemseries@shottrack.com");

        history(token)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    void shouldNeverReturnSeriesOfAnotherAthlete() throws Exception {
        Scenario scenario = createScenario("atleta.historicoisolamento@shottrack.com");
        String otherToken = registerAndLogin("atleta.historicoisolamento.outro@shottrack.com");
        UUID otherTraining = SeriesTestSupport.openTraining(mockMvc, objectMapper, otherToken, "IPSC");
        registerSeries(otherToken, new RegisterSeriesRequest(otherTraining, null, null, null, "outro", null, null));

        history(scenario.token())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[*].target").value(contains("s5", "s4", "s3", "s2", "s1")));
        history(otherToken)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[*].target").value(contains("outro")));
    }

    @Test
    void shouldRejectRequestWithoutToken() throws Exception {
        mockMvc.perform(get("/api/series"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
    }

    @Test
    void shouldRejectStartDateAfterEndDate() throws Exception {
        String token = registerAndLogin("atleta.historicoperiodoinvalido@shottrack.com");

        history(token, "startDate", daysAgo(1).toString(), "endDate", daysAgo(5).toString())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    void shouldRejectWeaponThatDoesNotExist() throws Exception {
        String token = registerAndLogin("atleta.historicoarmainexistente@shottrack.com");

        history(token, "weaponId", UUID.randomUUID().toString())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("WEAPON_NOT_FOUND"));
    }

    @Test
    void shouldRejectWeaponOfAnotherAthlete() throws Exception {
        String token = registerAndLogin("atleta.historicoarmaalheia@shottrack.com");
        String otherToken = registerAndLogin("atleta.historicoarmaalheia.dono@shottrack.com");
        UUID otherWeapon = SeriesTestSupport.registerWeapon(mockMvc, objectMapper, otherToken);

        history(token, "weaponId", otherWeapon.toString())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("WEAPON_NOT_FOUND"));
    }

    @Test
    void shouldRejectModalityNotPracticed() throws Exception {
        String token = registerAndLogin("atleta.historiconaopraticada@shottrack.com");

        history(token, "modalityId", modalityIdByName(token, "IPSC").toString())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("MODALITY_NOT_PRACTICED"));
    }

    private Scenario createScenario(String email) throws Exception {
        String token = registerAndLogin(email);
        UUID weaponA = SeriesTestSupport.registerWeapon(mockMvc, objectMapper, token, "Arma A");
        UUID weaponB = SeriesTestSupport.registerWeapon(mockMvc, objectMapper, token, "Arma B");

        UUID tenDaysAgo = SeriesTestSupport.openTraining(mockMvc, objectMapper, token, "IPSC");
        registerSeries(token, new RegisterSeriesRequest(tenDaysAgo, weaponA, null, new BigDecimal("10"), "s1", null, null));
        registerSeries(token, new RegisterSeriesRequest(tenDaysAgo, weaponB, null, new BigDecimal("25"), "s2", null, null));
        SeriesTestSupport.backdateTraining(entityManager, tenDaysAgo, daysAgo(10));

        UUID threeDaysAgo = SeriesTestSupport.openTraining(mockMvc, objectMapper, token, "Precisão");
        registerSeries(token, new RegisterSeriesRequest(threeDaysAgo, weaponA, null, new BigDecimal("25"), "s3", null, null));
        registerSeries(token, new RegisterSeriesRequest(threeDaysAgo, null, null, null, "s4", null, null));
        SeriesTestSupport.backdateTraining(entityManager, threeDaysAgo, daysAgo(3));

        UUID oneDayAgo = SeriesTestSupport.openTraining(mockMvc, objectMapper, token, "IPSC");
        registerSeries(token, new RegisterSeriesRequest(oneDayAgo, weaponB, null, new BigDecimal("50"), "s5", null, null));
        SeriesTestSupport.backdateTraining(entityManager, oneDayAgo, daysAgo(1));

        return new Scenario(token, weaponA, weaponB);
    }

    private record Scenario(String token, UUID weaponA, UUID weaponB) {
    }

    /**
     * params: pares nome/valor de query param.
     */
    private ResultActions history(String token, String... params) throws Exception {
        MockHttpServletRequestBuilder request = get("/api/series").header("Authorization", "Bearer " + token);
        for (int i = 0; i < params.length; i += 2) {
            request.param(params[i], params[i + 1]);
        }
        return mockMvc.perform(request);
    }

    private UUID registerSeries(String token, RegisterSeriesRequest request) throws Exception {
        var result = mockMvc.perform(post("/api/series")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();
        return UUID.fromString(objectMapper.readTree(result.getResponse().getContentAsString()).get("data").get("id").asText());
    }

    private void registerResult(String token, UUID seriesId, UUID resultTypeId, String value) throws Exception {
        mockMvc.perform(post("/api/series/" + seriesId + "/results")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RegisterSeriesResultRequest(resultTypeId, value))))
                .andExpect(status().isOk());
    }

    private LocalDate daysAgo(int days) {
        return LocalDate.now(ZoneOffset.UTC).minusDays(days);
    }

    private UUID modalityIdByName(String token, String name) throws Exception {
        return idByName(token, "/api/modality-catalog", name);
    }

    private UUID resultTypeIdByName(String token, String name) throws Exception {
        return idByName(token, "/api/result-type-catalog", name);
    }

    private UUID idByName(String token, String path, String name) throws Exception {
        var result = mockMvc.perform(get(path).header("Authorization", "Bearer " + token)).andReturn();

        JsonNode items = objectMapper.readTree(result.getResponse().getContentAsString()).get("data");
        for (JsonNode item : items) {
            if (item.get("name").asText().equals(name)) {
                return UUID.fromString(item.get("id").asText());
            }
        }
        throw new AssertionError("Item não encontrado no catálogo (" + path + "): " + name);
    }

    private String registerAndLogin(String email) throws Exception {
        return TestUsers.registerAndLogin(mockMvc, objectMapper, pendingRegistrationRepository, "Atleta Teste", email, PASSWORD);
    }
}
