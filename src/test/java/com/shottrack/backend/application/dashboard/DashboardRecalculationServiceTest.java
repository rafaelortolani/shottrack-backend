package com.shottrack.backend.application.dashboard;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shottrack.backend.application.dashboard.gateway.repository.DashboardSummaryRepository;
import com.shottrack.backend.application.dashboard.model.DashboardSummary;
import com.shottrack.backend.application.dashboard.usecase.DashboardRecalculationService;
import com.shottrack.backend.application.modality.dto.AddModalityResultTypeRequest;
import com.shottrack.backend.application.series.dto.RegisterSeriesRequest;
import com.shottrack.backend.application.series.dto.RegisterSeriesResultRequest;
import com.shottrack.backend.application.series.gateway.repository.SeriesResultRepository;
import com.shottrack.backend.application.series.model.SeriesResult;
import com.shottrack.backend.application.user.gateway.repository.PendingRegistrationRepository;
import com.shottrack.backend.support.SeriesTestSupport;
import com.shottrack.backend.support.TestUsers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * ADR-0015: testa a lógica de recálculo (mesma usada pelo consumidor da
 * fila e pelo fallback do UC42) chamando DashboardRecalculationService
 * diretamente — sem precisar da fila de verdade rodando, como o próprio
 * ADR prevê.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class DashboardRecalculationServiceTest {

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
    private DashboardSummaryRepository dashboardSummaryRepository;

    @Autowired
    private DashboardRecalculationService dashboardRecalculationService;

    @Test
    void shouldRecalculateAndPersistSummaryWithRealData() throws Exception {
        String token = registerAndLogin("atleta.recalculodadosreais@shottrack.com");
        UUID userId = currentUserId(token);

        UUID trainingId = SeriesTestSupport.openTraining(mockMvc, objectMapper, token, "IPSC");
        UUID modalityId = modalityIdByName(token, "IPSC");
        UUID tempoId = configureResultType(token, modalityId, "Tempo");

        registerSeriesWithShotCountAndResult(token, trainingId, 20, tempoId, "12.5");
        registerSeriesWithShotCountAndResult(token, trainingId, 30, tempoId, "10.0");

        SeriesTestSupport.openTraining(mockMvc, objectMapper, token, "Trap");

        dashboardRecalculationService.recalculate(userId);

        DashboardSummary summary = dashboardSummaryRepository.findByUserId(userId).orElseThrow();
        assertThat(summary.getTrainingsThisMonth()).isEqualTo(2);
        assertThat(summary.getShotsThisMonth()).isEqualTo(50);
        assertThat(summary.getPracticedModalities()).containsExactlyInAnyOrder("IPSC", "Trap");
        assertThat(summary.getHighlightResultTypeName()).isEqualTo("Tempo");
        assertThat(summary.getHighlightValue()).isEqualByComparingTo(new BigDecimal("10.0"));
    }

    @Test
    void shouldRecalculateOnTopOfAnExistingSummary() throws Exception {
        String token = registerAndLogin("atleta.recalculosobreexistente@shottrack.com");
        UUID userId = currentUserId(token);

        UUID trainingId = SeriesTestSupport.openTraining(mockMvc, objectMapper, token, "IPSC");
        registerSeriesWithShotCount(token, trainingId, 10);
        dashboardRecalculationService.recalculate(userId);
        assertThat(dashboardSummaryRepository.findByUserId(userId).orElseThrow().getShotsThisMonth()).isEqualTo(10);

        registerSeriesWithShotCount(token, trainingId, 15);
        dashboardRecalculationService.recalculate(userId);

        // filtra pelo atleta: o banco de desenvolvimento pode ter resumos de outros atletas
        assertThat(dashboardSummaryRepository.findAll())
                .filteredOn(summary -> summary.getUserId().equals(userId))
                .hasSize(1);
        assertThat(dashboardSummaryRepository.findByUserId(userId).orElseThrow().getShotsThisMonth()).isEqualTo(25);
    }

    @Test
    void shouldPickHighlightTypeWithMostFilledRecordsRespectingOrientation() throws Exception {
        String token = registerAndLogin("atleta.destaquemaisregistrado@shottrack.com");
        UUID userId = currentUserId(token);

        UUID trainingId = SeriesTestSupport.openTraining(mockMvc, objectMapper, token, "IPSC");
        UUID modalityId = modalityIdByName(token, "IPSC");
        UUID tempoId = configureResultType(token, modalityId, "Tempo");
        UUID pontuacaoId = configureResultType(token, modalityId, "Pontuação");

        // Tempo (MENOR_MELHOR): 3 registros, melhor (menor) é 10.5
        registerSeriesWithResult(token, trainingId, tempoId, "12.0");
        registerSeriesWithResult(token, trainingId, tempoId, "10.5");
        registerSeriesWithResult(token, trainingId, tempoId, "15.0");

        // Pontuação (MAIOR_MELHOR): só 1 registro — menos que Tempo
        registerSeriesWithResult(token, trainingId, pontuacaoId, "90");

        dashboardRecalculationService.recalculate(userId);

        DashboardSummary summary = dashboardSummaryRepository.findByUserId(userId).orElseThrow();
        assertThat(summary.getHighlightResultTypeName()).isEqualTo("Tempo");
        assertThat(summary.getHighlightValue()).isEqualByComparingTo(new BigDecimal("10.5"));
    }

    @Test
    void shouldTieBreakHighlightByMostRecentlyUsedType() throws Exception {
        String token = registerAndLogin("atleta.destaquedesempate@shottrack.com");
        UUID userId = currentUserId(token);

        UUID trainingId = SeriesTestSupport.openTraining(mockMvc, objectMapper, token, "IPSC");
        UUID modalityId = modalityIdByName(token, "IPSC");
        UUID tempoId = configureResultType(token, modalityId, "Tempo");
        UUID pontuacaoId = configureResultType(token, modalityId, "Pontuação");

        // mesma contagem (2 cada) — Pontuação é a usada por último
        registerSeriesWithResult(token, trainingId, tempoId, "12.0");
        registerSeriesWithResult(token, trainingId, tempoId, "11.0");
        registerSeriesWithResult(token, trainingId, pontuacaoId, "80");
        registerSeriesWithResult(token, trainingId, pontuacaoId, "95");

        dashboardRecalculationService.recalculate(userId);

        DashboardSummary summary = dashboardSummaryRepository.findByUserId(userId).orElseThrow();
        assertThat(summary.getHighlightResultTypeName()).isEqualTo("Pontuação");
        assertThat(summary.getHighlightValue()).isEqualByComparingTo(new BigDecimal("95"));
    }

    @Test
    void shouldHaveNoHighlightWhenNoEligibleTypeHasData() throws Exception {
        String token = registerAndLogin("atleta.destaqueausente@shottrack.com");
        UUID userId = currentUserId(token);

        SeriesTestSupport.openTraining(mockMvc, objectMapper, token, "IPSC");

        dashboardRecalculationService.recalculate(userId);

        DashboardSummary summary = dashboardSummaryRepository.findByUserId(userId).orElseThrow();
        assertThat(summary.getHighlightResultTypeName()).isNull();
        assertThat(summary.getHighlightValue()).isNull();
    }

    @Test
    void shouldIgnoreNonNumericValueWhenCalculatingHighlight() throws Exception {
        String token = registerAndLogin("atleta.destaquevalornaonumerico@shottrack.com");
        UUID userId = currentUserId(token);

        UUID trainingId = SeriesTestSupport.openTraining(mockMvc, objectMapper, token, "IPSC");
        UUID modalityId = modalityIdByName(token, "IPSC");
        UUID tempoId = configureResultType(token, modalityId, "Tempo");

        UUID seriesIdInvalido = registerSeries(token, trainingId);
        SeriesResult resultadoInvalido = SeriesResult.builder().seriesId(seriesIdInvalido).resultTypeId(tempoId).build();
        resultadoInvalido.registerValue("não é número");
        seriesResultRepository.save(resultadoInvalido);

        dashboardRecalculationService.recalculate(userId);

        DashboardSummary summary = dashboardSummaryRepository.findByUserId(userId).orElseThrow();
        // valor inválido é ignorado — nenhum tipo elegível sobra com registro válido
        assertThat(summary.getHighlightResultTypeName()).isNull();

        UUID seriesIdValido = registerSeries(token, trainingId);
        registerValue(token, seriesIdValido, tempoId, "8.0");

        dashboardRecalculationService.recalculate(userId);

        DashboardSummary updated = dashboardSummaryRepository.findByUserId(userId).orElseThrow();
        assertThat(updated.getHighlightResultTypeName()).isEqualTo("Tempo");
        assertThat(updated.getHighlightValue()).isEqualByComparingTo(new BigDecimal("8.0"));
    }

    private void registerValue(String token, UUID seriesId, UUID resultTypeId, String value) throws Exception {
        mockMvc.perform(post("/api/series/" + seriesId + "/results")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new RegisterSeriesResultRequest(resultTypeId, value))));
    }

    private UUID registerSeriesWithResult(String token, UUID trainingId, UUID resultTypeId, String value) throws Exception {
        UUID seriesId = registerSeries(token, trainingId);
        registerValue(token, seriesId, resultTypeId, value);
        return seriesId;
    }

    private void registerSeriesWithShotCountAndResult(String token, UUID trainingId, int shotCount, UUID resultTypeId, String value) throws Exception {
        UUID seriesId = registerSeriesWithShotCount(token, trainingId, shotCount);
        registerValue(token, seriesId, resultTypeId, value);
    }

    private UUID configureResultType(String token, UUID modalityId, String resultTypeName) throws Exception {
        UUID resultTypeId = resultTypeIdByName(token, resultTypeName);

        mockMvc.perform(post("/api/practiced-modalities/" + modalityId + "/result-types")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new AddModalityResultTypeRequest(resultTypeId))));

        return resultTypeId;
    }

    private UUID registerSeries(String token, UUID trainingId) throws Exception {
        var result = mockMvc.perform(post("/api/series")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new RegisterSeriesRequest(trainingId, null, null, null, null, null, null))))
                .andReturn();
        return idFromResponse(result);
    }

    private UUID registerSeriesWithShotCount(String token, UUID trainingId, int shotCount) throws Exception {
        var result = mockMvc.perform(post("/api/series")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new RegisterSeriesRequest(trainingId, null, null, null, null, shotCount, null))))
                .andReturn();
        return idFromResponse(result);
    }

    private UUID currentUserId(String token) throws Exception {
        var result = mockMvc.perform(get("/api/users/me").header("Authorization", "Bearer " + token)).andReturn();
        return idFromResponse(result);
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

    private UUID idFromResponse(org.springframework.test.web.servlet.MvcResult result) throws Exception {
        return UUID.fromString(objectMapper.readTree(result.getResponse().getContentAsString()).get("data").get("id").asText());
    }

    private String registerAndLogin(String email) throws Exception {
        return TestUsers.registerAndLogin(mockMvc, objectMapper, pendingRegistrationRepository, "Atleta Teste", email, PASSWORD);
    }
}
