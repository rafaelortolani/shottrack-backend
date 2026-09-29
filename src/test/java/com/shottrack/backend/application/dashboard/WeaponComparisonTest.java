package com.shottrack.backend.application.dashboard;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shottrack.backend.application.modality.dto.AddPracticedModalityRequest;
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

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * UC48: GET /api/dashboard/weapon-comparison.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class WeaponComparisonTest {

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
    void shouldCompareTwoWeaponsWithData() throws Exception {
        String token = registerAndLogin("atleta.comparacaoduas@shottrack.com");
        UUID weaponA = SeriesTestSupport.registerWeapon(mockMvc, objectMapper, token);
        UUID weaponB = SeriesTestSupport.registerWeapon(mockMvc, objectMapper, token);
        UUID pontuacaoId = resultTypeIdByName(token, "Pontuação");

        UUID ipsc = SeriesTestSupport.openTraining(mockMvc, objectMapper, token, "IPSC");
        registerSeriesWithResult(token, ipsc, weaponA, pontuacaoId, "80");
        registerSeriesWithResult(token, ipsc, weaponA, pontuacaoId, "90");
        // conta como série da arma, mas não entra em média/melhor
        registerSeries(token, ipsc, weaponA);
        registerSeriesWithResult(token, ipsc, weaponB, pontuacaoId, "70");
        // sem arma — não pertence a nenhuma das comparadas
        registerSeriesWithResult(token, ipsc, null, pontuacaoId, "100");
        // outra modalidade — fora da comparação de IPSC
        UUID precisao = SeriesTestSupport.openTraining(mockMvc, objectMapper, token, "Precisão");
        registerSeriesWithResult(token, precisao, weaponA, pontuacaoId, "99");

        compare(token, modalityIdByName(token, "IPSC"), pontuacaoId, weaponA, weaponB)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].weapon.id").value(weaponA.toString()))
                .andExpect(jsonPath("$.data[0].seriesCount").value(3))
                .andExpect(jsonPath("$.data[0].average").value(85))
                .andExpect(jsonPath("$.data[0].best").value(90))
                .andExpect(jsonPath("$.data[1].weapon.id").value(weaponB.toString()))
                .andExpect(jsonPath("$.data[1].seriesCount").value(1))
                .andExpect(jsonPath("$.data[1].average").value(70))
                .andExpect(jsonPath("$.data[1].best").value(70));
    }

    @Test
    void shouldCompareThreeOrMoreWeaponsRespectingOrientation() throws Exception {
        String token = registerAndLogin("atleta.comparacaotres@shottrack.com");
        UUID weaponA = SeriesTestSupport.registerWeapon(mockMvc, objectMapper, token);
        UUID weaponB = SeriesTestSupport.registerWeapon(mockMvc, objectMapper, token);
        UUID weaponC = SeriesTestSupport.registerWeapon(mockMvc, objectMapper, token);
        UUID tempoId = resultTypeIdByName(token, "Tempo");

        UUID training = SeriesTestSupport.openTraining(mockMvc, objectMapper, token, "IPSC");
        registerSeriesWithResult(token, training, weaponA, tempoId, "10.0");
        registerSeriesWithResult(token, training, weaponA, tempoId, "12.0");
        registerSeriesWithResult(token, training, weaponB, tempoId, "9.5");
        registerSeriesWithResult(token, training, weaponC, tempoId, "14.0");
        registerSeriesWithResult(token, training, weaponC, tempoId, "16.0");

        // Tempo é MENOR_MELHOR — melhor é o menor valor
        compare(token, modalityIdByName(token, "IPSC"), tempoId, weaponA, weaponB, weaponC)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(3))
                .andExpect(jsonPath("$.data[0].weapon.id").value(weaponA.toString()))
                .andExpect(jsonPath("$.data[0].seriesCount").value(2))
                .andExpect(jsonPath("$.data[0].average").value(11.0))
                .andExpect(jsonPath("$.data[0].best").value(10.0))
                .andExpect(jsonPath("$.data[1].weapon.id").value(weaponB.toString()))
                .andExpect(jsonPath("$.data[1].seriesCount").value(1))
                .andExpect(jsonPath("$.data[1].average").value(9.5))
                .andExpect(jsonPath("$.data[1].best").value(9.5))
                .andExpect(jsonPath("$.data[2].weapon.id").value(weaponC.toString()))
                .andExpect(jsonPath("$.data[2].seriesCount").value(2))
                .andExpect(jsonPath("$.data[2].average").value(15.0))
                .andExpect(jsonPath("$.data[2].best").value(14.0));
    }

    @Test
    void shouldShowWeaponWithoutDataWithZeroCountWithoutBreakingComparison() throws Exception {
        String token = registerAndLogin("atleta.comparacaosemdado@shottrack.com");
        UUID withData = SeriesTestSupport.registerWeapon(mockMvc, objectMapper, token);
        UUID withoutSeries = SeriesTestSupport.registerWeapon(mockMvc, objectMapper, token);
        UUID withoutValue = SeriesTestSupport.registerWeapon(mockMvc, objectMapper, token);
        UUID pontuacaoId = resultTypeIdByName(token, "Pontuação");

        UUID training = SeriesTestSupport.openTraining(mockMvc, objectMapper, token, "IPSC");
        registerSeriesWithResult(token, training, withData, pontuacaoId, "88");
        registerSeries(token, training, withoutValue);

        compare(token, modalityIdByName(token, "IPSC"), pontuacaoId, withData, withoutSeries, withoutValue)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(3))
                .andExpect(jsonPath("$.data[0].seriesCount").value(1))
                .andExpect(jsonPath("$.data[0].average").value(88))
                .andExpect(jsonPath("$.data[0].best").value(88))
                .andExpect(jsonPath("$.data[1].weapon.id").value(withoutSeries.toString()))
                .andExpect(jsonPath("$.data[1].seriesCount").value(0))
                .andExpect(jsonPath("$.data[1].average").doesNotExist())
                .andExpect(jsonPath("$.data[1].best").doesNotExist())
                // tem série nessa modalidade, mas nenhum valor do tipo escolhido
                .andExpect(jsonPath("$.data[2].weapon.id").value(withoutValue.toString()))
                .andExpect(jsonPath("$.data[2].seriesCount").value(1))
                .andExpect(jsonPath("$.data[2].average").doesNotExist())
                .andExpect(jsonPath("$.data[2].best").doesNotExist());
    }

    @Test
    void shouldLimitComparisonToPeriod() throws Exception {
        String token = registerAndLogin("atleta.comparacaoperiodo@shottrack.com");
        UUID weaponA = SeriesTestSupport.registerWeapon(mockMvc, objectMapper, token);
        UUID weaponB = SeriesTestSupport.registerWeapon(mockMvc, objectMapper, token);
        UUID ipscId = modalityIdByName(token, "IPSC");
        UUID pontuacaoId = resultTypeIdByName(token, "Pontuação");

        UUID older = SeriesTestSupport.openTraining(mockMvc, objectMapper, token, "IPSC");
        registerSeriesWithResult(token, older, weaponA, pontuacaoId, "80");
        SeriesTestSupport.backdateTraining(entityManager, older, daysAgo(10));
        UUID recent = SeriesTestSupport.openTraining(mockMvc, objectMapper, token, "IPSC");
        registerSeriesWithResult(token, recent, weaponA, pontuacaoId, "90");
        SeriesTestSupport.backdateTraining(entityManager, recent, daysAgo(1));

        compare(token, ipscId, pontuacaoId, new UUID[]{weaponA, weaponB}, "startDate", daysAgo(5).toString())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].seriesCount").value(1))
                .andExpect(jsonPath("$.data[0].average").value(90));
        compare(token, ipscId, pontuacaoId, new UUID[]{weaponA, weaponB}, "endDate", daysAgo(5).toString())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].seriesCount").value(1))
                .andExpect(jsonPath("$.data[0].average").value(80));
        compare(token, ipscId, pontuacaoId, new UUID[]{weaponA, weaponB},
                "startDate", daysAgo(10).toString(), "endDate", daysAgo(1).toString())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].seriesCount").value(2))
                .andExpect(jsonPath("$.data[0].average").value(85));
    }

    @Test
    void shouldRejectRequestWithoutToken() throws Exception {
        mockMvc.perform(get("/api/dashboard/weapon-comparison")
                        .param("modalityId", UUID.randomUUID().toString())
                        .param("resultTypeId", UUID.randomUUID().toString())
                        .param("weaponIds", UUID.randomUUID().toString(), UUID.randomUUID().toString()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
    }

    @Test
    void shouldRejectLessThanTwoWeapons() throws Exception {
        String token = registerAndLogin("atleta.comparacaoumaarma@shottrack.com");
        UUID weapon = SeriesTestSupport.registerWeapon(mockMvc, objectMapper, token);
        UUID ipscId = practiceModality(token, "IPSC");
        UUID pontuacaoId = resultTypeIdByName(token, "Pontuação");

        compare(token, ipscId, pontuacaoId, weapon)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
        // a mesma arma repetida não vira comparação
        compare(token, ipscId, pontuacaoId, weapon, weapon)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
        compare(token, ipscId, pontuacaoId)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    void shouldRejectStartDateAfterEndDate() throws Exception {
        String token = registerAndLogin("atleta.comparacaoperiodoinvalido@shottrack.com");
        UUID weaponA = SeriesTestSupport.registerWeapon(mockMvc, objectMapper, token);
        UUID weaponB = SeriesTestSupport.registerWeapon(mockMvc, objectMapper, token);

        compare(token, practiceModality(token, "IPSC"), resultTypeIdByName(token, "Pontuação"),
                new UUID[]{weaponA, weaponB}, "startDate", daysAgo(1).toString(), "endDate", daysAgo(5).toString())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    void shouldRejectModalityNotPracticed() throws Exception {
        String token = registerAndLogin("atleta.comparacaonaopraticada@shottrack.com");
        UUID weaponA = SeriesTestSupport.registerWeapon(mockMvc, objectMapper, token);
        UUID weaponB = SeriesTestSupport.registerWeapon(mockMvc, objectMapper, token);

        compare(token, modalityIdByName(token, "IPSC"), resultTypeIdByName(token, "Pontuação"), weaponA, weaponB)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("MODALITY_NOT_PRACTICED"));
    }

    @Test
    void shouldRejectResultTypeNotConfiguredForModality() throws Exception {
        String token = registerAndLogin("atleta.comparacaotiponaoconfigurado@shottrack.com");
        UUID weaponA = SeriesTestSupport.registerWeapon(mockMvc, objectMapper, token);
        UUID weaponB = SeriesTestSupport.registerWeapon(mockMvc, objectMapper, token);

        // Agrupamento não está na sugestão padrão de IPSC (ADR-0011)
        compare(token, practiceModality(token, "IPSC"), resultTypeIdByName(token, "Agrupamento"), weaponA, weaponB)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("RESULT_TYPE_NOT_CONFIGURED_FOR_TRAINING"));
    }

    @Test
    void shouldRejectWeaponThatDoesNotExist() throws Exception {
        String token = registerAndLogin("atleta.comparacaoarmainexistente@shottrack.com");
        UUID weapon = SeriesTestSupport.registerWeapon(mockMvc, objectMapper, token);

        compare(token, practiceModality(token, "IPSC"), resultTypeIdByName(token, "Pontuação"), weapon, UUID.randomUUID())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("WEAPON_NOT_FOUND"));
    }

    @Test
    void shouldRejectWeaponOfAnotherAthlete() throws Exception {
        String token = registerAndLogin("atleta.comparacaoarmaalheia@shottrack.com");
        UUID weapon = SeriesTestSupport.registerWeapon(mockMvc, objectMapper, token);
        String otherToken = registerAndLogin("atleta.comparacaoarmaalheia.dono@shottrack.com");
        UUID otherWeapon = SeriesTestSupport.registerWeapon(mockMvc, objectMapper, otherToken);

        compare(token, practiceModality(token, "IPSC"), resultTypeIdByName(token, "Pontuação"), weapon, otherWeapon)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("WEAPON_NOT_FOUND"));
    }

    private ResultActions compare(String token, UUID modalityId, UUID resultTypeId, UUID... weaponIds) throws Exception {
        return compare(token, modalityId, resultTypeId, weaponIds, new String[0]);
    }

    /**
     * params: pares nome/valor de query param extras (período).
     */
    private ResultActions compare(String token, UUID modalityId, UUID resultTypeId, UUID[] weaponIds,
                                  String... params) throws Exception {
        MockHttpServletRequestBuilder request = get("/api/dashboard/weapon-comparison")
                .header("Authorization", "Bearer " + token)
                .param("modalityId", modalityId.toString())
                .param("resultTypeId", resultTypeId.toString());
        for (UUID weaponId : weaponIds) {
            request.param("weaponIds", weaponId.toString());
        }
        for (int i = 0; i < params.length; i += 2) {
            request.param(params[i], params[i + 1]);
        }
        return mockMvc.perform(request);
    }

    /**
     * Pratica a modalidade sem abrir treino — aplica a sugestão padrão de
     * tipos de resultado (UC30), igual a abrir um treino faria.
     */
    private UUID practiceModality(String token, String name) throws Exception {
        UUID modalityId = modalityIdByName(token, name);
        mockMvc.perform(post("/api/practiced-modalities")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AddPracticedModalityRequest(modalityId))))
                .andExpect(status().is2xxSuccessful());
        return modalityId;
    }

    private UUID registerSeries(String token, UUID trainingId, UUID weaponId) throws Exception {
        var result = mockMvc.perform(post("/api/series")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new RegisterSeriesRequest(trainingId, weaponId, null, null, null, null, null))))
                .andExpect(status().isCreated())
                .andReturn();
        return UUID.fromString(objectMapper.readTree(result.getResponse().getContentAsString()).get("data").get("id").asText());
    }

    private void registerSeriesWithResult(String token, UUID trainingId, UUID weaponId, UUID resultTypeId,
                                          String value) throws Exception {
        mockMvc.perform(post("/api/series/" + registerSeries(token, trainingId, weaponId) + "/results")
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
