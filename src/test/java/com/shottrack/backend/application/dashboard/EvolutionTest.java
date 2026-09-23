package com.shottrack.backend.application.dashboard;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shottrack.backend.application.modality.dto.AddPracticedModalityRequest;
import com.shottrack.backend.application.series.dto.RegisterSeriesRequest;
import com.shottrack.backend.application.series.dto.RegisterSeriesResultRequest;
import com.shottrack.backend.application.series.gateway.repository.SeriesResultRepository;
import com.shottrack.backend.application.series.model.SeriesResult;
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
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * UC46/ADR-0016: GET /api/dashboard/evolution. O dia de cada ponto é o
 * dia do treino — como a auditoria sempre grava "agora", os testes movem
 * o início do treino pro passado direto no banco (backdateTraining).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class EvolutionTest {

    private static final String PASSWORD = "senha12345";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PendingRegistrationRepository pendingRegistrationRepository;

    @Autowired
    private SeriesResultRepository seriesResultRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void shouldAverageValuesOfEachDayInMediaMode() throws Exception {
        String token = registerAndLogin("atleta.evolucaomedia@shottrack.com");
        UUID tempoId = resultTypeIdByName(token, "Tempo");

        UUID older = SeriesTestSupport.openTraining(mockMvc, objectMapper, token, "IPSC");
        registerSeriesWithResult(token, older, tempoId, "9.0");
        backdateTraining(older, 5);
        // mesmo dia: dois treinos, três valores — média de todos (10 + 12 + 14) / 3
        UUID firstOfDay = SeriesTestSupport.openTraining(mockMvc, objectMapper, token, "IPSC");
        registerSeriesWithResult(token, firstOfDay, tempoId, "10.0");
        registerSeriesWithResult(token, firstOfDay, tempoId, "12.0");
        backdateTraining(firstOfDay, 2);
        UUID secondOfDay = SeriesTestSupport.openTraining(mockMvc, objectMapper, token, "IPSC");
        registerSeriesWithResult(token, secondOfDay, tempoId, "14.0");
        backdateTraining(secondOfDay, 2);

        evolution(token, modalityIdByName(token, "IPSC"), tempoId, "7d", "MEDIA")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].date").value(daysAgo(5).toString()))
                .andExpect(jsonPath("$.data[0].value").value(9.0))
                .andExpect(jsonPath("$.data[1].date").value(daysAgo(2).toString()))
                .andExpect(jsonPath("$.data[1].value").value(12.0));
    }

    @Test
    void shouldPickBestValueOfEachDayRespectingOrientationInMelhorMode() throws Exception {
        String token = registerAndLogin("atleta.evolucaomelhor@shottrack.com");
        UUID ipscId = modalityIdByName(token, "IPSC");
        UUID tempoId = resultTypeIdByName(token, "Tempo");
        UUID pontuacaoId = resultTypeIdByName(token, "Pontuação");

        UUID training = SeriesTestSupport.openTraining(mockMvc, objectMapper, token, "IPSC");
        registerSeriesWithResult(token, training, tempoId, "12.0");
        registerSeriesWithResult(token, training, tempoId, "10.0");
        registerSeriesWithResult(token, training, pontuacaoId, "80");
        registerSeriesWithResult(token, training, pontuacaoId, "95");
        backdateTraining(training, 1);
        // outra modalidade com Pontuação maior — não entra na evolução de IPSC
        UUID precisao = SeriesTestSupport.openTraining(mockMvc, objectMapper, token, "Precisão");
        registerSeriesWithResult(token, precisao, pontuacaoId, "99");
        backdateTraining(precisao, 1);

        // Tempo é MENOR_MELHOR
        evolution(token, ipscId, tempoId, "7d", "MELHOR")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].date").value(daysAgo(1).toString()))
                .andExpect(jsonPath("$.data[0].value").value(10.0));

        // Pontuação é MAIOR_MELHOR
        evolution(token, ipscId, pontuacaoId, "7d", "MELHOR")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].value").value(95));
    }

    @Test
    void shouldLimitPointsToEachPeriod() throws Exception {
        String token = registerAndLogin("atleta.evolucaoperiodos@shottrack.com");
        UUID ipscId = modalityIdByName(token, "IPSC");
        UUID tempoId = resultTypeIdByName(token, "Tempo");

        for (int daysAgo : new int[]{3, 20, 60, 200, 400}) {
            UUID training = SeriesTestSupport.openTraining(mockMvc, objectMapper, token, "IPSC");
            registerSeriesWithResult(token, training, tempoId, "10.0");
            backdateTraining(training, daysAgo);
        }

        evolution(token, ipscId, tempoId, "7d", "MEDIA")
                .andExpect(jsonPath("$.data[*].date").value(org.hamcrest.Matchers.contains(
                        daysAgo(3).toString())));
        evolution(token, ipscId, tempoId, "30d", "MEDIA")
                .andExpect(jsonPath("$.data[*].date").value(org.hamcrest.Matchers.contains(
                        daysAgo(20).toString(), daysAgo(3).toString())));
        evolution(token, ipscId, tempoId, "3m", "MEDIA")
                .andExpect(jsonPath("$.data[*].date").value(org.hamcrest.Matchers.contains(
                        daysAgo(60).toString(), daysAgo(20).toString(), daysAgo(3).toString())));
        // 400 dias atrás fica fora até do período de 1 ano
        evolution(token, ipscId, tempoId, "1a", "MEDIA")
                .andExpect(jsonPath("$.data[*].date").value(org.hamcrest.Matchers.contains(
                        daysAgo(200).toString(), daysAgo(60).toString(), daysAgo(20).toString(), daysAgo(3).toString())));
    }

    @Test
    void shouldReturnEmptyListWhenPeriodHasNoData() throws Exception {
        String token = registerAndLogin("atleta.evolucaovazia@shottrack.com");
        UUID tempoId = resultTypeIdByName(token, "Tempo");

        UUID training = SeriesTestSupport.openTraining(mockMvc, objectMapper, token, "IPSC");
        registerSeriesWithResult(token, training, tempoId, "10.0");
        backdateTraining(training, 20);

        evolution(token, modalityIdByName(token, "IPSC"), tempoId, "7d", "MEDIA")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    void shouldNotGeneratePointForDayWithoutRecord() throws Exception {
        String token = registerAndLogin("atleta.evolucaodiasemregistro@shottrack.com");
        UUID tempoId = resultTypeIdByName(token, "Tempo");

        UUID fourDaysAgo = SeriesTestSupport.openTraining(mockMvc, objectMapper, token, "IPSC");
        registerSeriesWithResult(token, fourDaysAgo, tempoId, "10.0");
        backdateTraining(fourDaysAgo, 4);
        // treino com série mas sem valor de Tempo — o dia não vira ponto (nem zero)
        UUID threeDaysAgo = SeriesTestSupport.openTraining(mockMvc, objectMapper, token, "IPSC");
        registerSeries(token, threeDaysAgo);
        backdateTraining(threeDaysAgo, 3);
        UUID oneDayAgo = SeriesTestSupport.openTraining(mockMvc, objectMapper, token, "IPSC");
        registerSeriesWithResult(token, oneDayAgo, tempoId, "8.0");
        backdateTraining(oneDayAgo, 1);

        evolution(token, modalityIdByName(token, "IPSC"), tempoId, "7d", "MEDIA")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[*].date").value(org.hamcrest.Matchers.contains(
                        daysAgo(4).toString(), daysAgo(1).toString())))
                .andExpect(jsonPath("$.data[*].value").value(org.hamcrest.Matchers.contains(10.0, 8.0)));
    }

    @Test
    void shouldIgnoreNonNumericValues() throws Exception {
        String token = registerAndLogin("atleta.evolucaonaonumerico@shottrack.com");
        UUID tempoId = resultTypeIdByName(token, "Tempo");

        UUID training = SeriesTestSupport.openTraining(mockMvc, objectMapper, token, "IPSC");
        registerSeriesWithResult(token, training, tempoId, "10.0");
        // a API rejeita valor não numérico pra Tempo — grava direto pra simular dado legado
        SeriesResult invalid = SeriesResult.builder().seriesId(registerSeries(token, training)).resultTypeId(tempoId).build();
        invalid.registerValue("não é número");
        seriesResultRepository.save(invalid);
        backdateTraining(training, 1);

        evolution(token, modalityIdByName(token, "IPSC"), tempoId, "7d", "MEDIA")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].value").value(10.0));
    }

    @Test
    void shouldRejectRequestWithoutToken() throws Exception {
        mockMvc.perform(get("/api/dashboard/evolution")
                        .param("modalityId", UUID.randomUUID().toString())
                        .param("resultTypeId", UUID.randomUUID().toString())
                        .param("period", "7d")
                        .param("mode", "MEDIA"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
    }

    @Test
    void shouldRejectInvalidPeriod() throws Exception {
        String token = registerAndLogin("atleta.evolucaoperiodoinvalido@shottrack.com");
        SeriesTestSupport.openTraining(mockMvc, objectMapper, token, "IPSC");

        evolution(token, modalityIdByName(token, "IPSC"), resultTypeIdByName(token, "Tempo"), "2w", "MEDIA")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    void shouldRejectModalityNotPracticed() throws Exception {
        String token = registerAndLogin("atleta.evolucaonaopraticada@shottrack.com");

        evolution(token, modalityIdByName(token, "IPSC"), resultTypeIdByName(token, "Tempo"), "7d", "MEDIA")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("MODALITY_NOT_PRACTICED"));
    }

    @Test
    void shouldRejectResultTypeNotConfiguredForModality() throws Exception {
        String token = registerAndLogin("atleta.evolucaotiponaoconfigurado@shottrack.com");
        UUID ipscId = modalityIdByName(token, "IPSC");
        mockMvc.perform(post("/api/practiced-modalities")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AddPracticedModalityRequest(ipscId))))
                .andExpect(status().is2xxSuccessful());

        // Agrupamento não está na sugestão padrão de IPSC (ADR-0011)
        evolution(token, ipscId, resultTypeIdByName(token, "Agrupamento"), "7d", "MEDIA")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("RESULT_TYPE_NOT_CONFIGURED_FOR_TRAINING"));
    }

    private ResultActions evolution(String token, UUID modalityId, UUID resultTypeId, String period, String mode) throws Exception {
        return mockMvc.perform(get("/api/dashboard/evolution")
                .header("Authorization", "Bearer " + token)
                .param("modalityId", modalityId.toString())
                .param("resultTypeId", resultTypeId.toString())
                .param("period", period)
                .param("mode", mode));
    }

    /**
     * Move o início do treino pro meio-dia (UTC) de N dias atrás. Flush
     * antes (o treino pode ainda não ter sido enviado ao banco) e clear
     * depois (senão o Hibernate devolve a entidade em cache, com a data
     * antiga).
     */
    private void backdateTraining(UUID trainingId, int daysAgo) {
        entityManager.flush();
        entityManager.createNativeQuery("UPDATE trainings SET created_at = :createdAt WHERE id = :id")
                .setParameter("createdAt", daysAgo(daysAgo).atTime(LocalTime.NOON).toInstant(ZoneOffset.UTC))
                .setParameter("id", trainingId)
                .executeUpdate();
        entityManager.clear();
    }

    private LocalDate daysAgo(int days) {
        return LocalDate.now(ZoneOffset.UTC).minusDays(days);
    }

    private UUID registerSeries(String token, UUID trainingId) throws Exception {
        var result = mockMvc.perform(post("/api/series")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new RegisterSeriesRequest(trainingId, null, null, null, null, null, null))))
                .andExpect(status().isCreated())
                .andReturn();
        return UUID.fromString(objectMapper.readTree(result.getResponse().getContentAsString()).get("data").get("id").asText());
    }

    private void registerSeriesWithResult(String token, UUID trainingId, UUID resultTypeId, String value) throws Exception {
        mockMvc.perform(post("/api/series/" + registerSeries(token, trainingId) + "/results")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RegisterSeriesResultRequest(resultTypeId, value))))
                .andExpect(status().isOk());
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
