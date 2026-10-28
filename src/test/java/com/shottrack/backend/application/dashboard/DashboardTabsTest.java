package com.shottrack.backend.application.dashboard;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shottrack.backend.application.dashboard.gateway.repository.DashboardSummaryRepository;
import com.shottrack.backend.application.dashboard.usecase.DashboardRecalculationService;
import com.shottrack.backend.application.user.gateway.repository.PendingRegistrationRepository;
import com.shottrack.backend.support.SeriesTestSupport;
import com.shottrack.backend.support.TestUsers;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;

import static com.shottrack.backend.support.DashboardTestSupport.configureResultType;
import static com.shottrack.backend.support.DashboardTestSupport.modalityId;
import static com.shottrack.backend.support.DashboardTestSupport.registerSeries;
import static com.shottrack.backend.support.DashboardTestSupport.registerSeriesWithResult;
import static com.shottrack.backend.support.DashboardTestSupport.resultTypeId;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * UC42 v7/ADR-0022: filtro por modalidade, indicadores do mês com período
 * anterior, últimos 3 treinos, recordes filtrados, evolução sugerida e aba
 * Modalidades. Os casos de calendário (mês curto, janeiro) estão em
 * MonthWindowsTest; aqui as datas são relativas a hoje (UTC).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class DashboardTabsTest {

    private static final String PASSWORD = "senha12345";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PendingRegistrationRepository pendingRegistrationRepository;

    @Autowired
    private DashboardSummaryRepository dashboardSummaryRepository;

    @Autowired
    private DashboardRecalculationService dashboardRecalculationService;

    @Autowired
    private EntityManager entityManager;

    @Test
    void shouldReturnIndicatorsWithPreviousPeriodWithoutFilter() throws Exception {
        String token = registerAndLogin("atleta.tabsindicadores@shottrack.com");

        UUID ipsc1 = training(token, "IPSC");
        registerSeries(mockMvc, objectMapper, token, ipsc1, 10);
        UUID ipsc2 = training(token, "IPSC");
        registerSeries(mockMvc, objectMapper, token, ipsc2, 20);
        UUID trap = training(token, "Trap");
        registerSeries(mockMvc, objectMapper, token, trap, 15);

        // mês anterior, dia 1 (sempre dentro da janela anterior)
        UUID previous = training(token, "IPSC");
        registerSeries(mockMvc, objectMapper, token, previous, 5);
        backdate(previous, previousMonthFirstDay());

        dashboard(token, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.trainingsThisMonth.value").value(3))
                .andExpect(jsonPath("$.data.trainingsThisMonth.previous").value(1))
                .andExpect(jsonPath("$.data.shotsThisMonth.value").value(45))
                .andExpect(jsonPath("$.data.shotsThisMonth.previous").value(5))
                .andExpect(jsonPath("$.data.modalitiesPracticed.value").value(2))
                .andExpect(jsonPath("$.data.modalitiesPracticed.previous").value(1));
    }

    @Test
    void shouldFilterIndicatorsByModalityButNotModalitiesPracticed() throws Exception {
        String token = registerAndLogin("atleta.tabsfiltro@shottrack.com");

        registerSeries(mockMvc, objectMapper, token, training(token, "IPSC"), 10);
        registerSeries(mockMvc, objectMapper, token, training(token, "Trap"), 15);
        UUID previousTrap = training(token, "Trap");
        registerSeries(mockMvc, objectMapper, token, previousTrap, 7);
        backdate(previousTrap, previousMonthFirstDay());
        UUID ipscId = modalityId(mockMvc, objectMapper, token, "IPSC");

        dashboard(token, ipscId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.trainingsThisMonth.value").value(1))
                .andExpect(jsonPath("$.data.trainingsThisMonth.previous").value(0))
                .andExpect(jsonPath("$.data.shotsThisMonth.value").value(10))
                .andExpect(jsonPath("$.data.shotsThisMonth.previous").value(0))
                // ignora o filtro: IPSC e Trap este mês; só Trap no anterior
                .andExpect(jsonPath("$.data.modalitiesPracticed.value").value(2))
                .andExpect(jsonPath("$.data.modalitiesPracticed.previous").value(1));
    }

    @Test
    void shouldLimitPreviousPeriodToSameDayOfMonthAndExcludeOlderMonths() throws Exception {
        String token = registerAndLogin("atleta.tabsanterior@shottrack.com");
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        LocalDate previousFirst = previousMonthFirstDay();
        int lastIncludedDay = Math.min(today.getDayOfMonth(), previousFirst.lengthOfMonth());

        UUID lastIncluded = training(token, "IPSC");
        backdate(lastIncluded, previousFirst.plusDays(lastIncludedDay - 1));
        UUID twoMonthsAgo = training(token, "IPSC");
        backdate(twoMonthsAgo, previousFirst.minusDays(1));
        if (lastIncludedDay < previousFirst.lengthOfMonth()) {
            // dia seguinte ao "mesmo dia do mês": fora da janela anterior
            backdate(training(token, "IPSC"), previousFirst.plusDays(lastIncludedDay));
        }

        dashboard(token, null)
                .andExpect(jsonPath("$.data.trainingsThisMonth.value").value(0))
                .andExpect(jsonPath("$.data.trainingsThisMonth.previous").value(1));
    }

    @Test
    void shouldReturnZeroPreviousWhenThereIsNoDataInPreviousPeriod() throws Exception {
        String token = registerAndLogin("atleta.tabssemanterior@shottrack.com");
        registerSeries(mockMvc, objectMapper, token, training(token, "IPSC"), 12);

        dashboard(token, null)
                .andExpect(jsonPath("$.data.trainingsThisMonth.value").value(1))
                .andExpect(jsonPath("$.data.trainingsThisMonth.previous").value(0))
                .andExpect(jsonPath("$.data.shotsThisMonth.value").value(12))
                .andExpect(jsonPath("$.data.shotsThisMonth.previous").value(0))
                .andExpect(jsonPath("$.data.modalitiesPracticed.previous").value(0));
    }

    @Test
    void shouldCountPreviousMonthTrainingsThatLeftCurrentMonthOut() throws Exception {
        String token = registerAndLogin("atleta.tabsviradadomes@shottrack.com");
        UUID previous = training(token, "IPSC");
        backdate(previous, previousMonthFirstDay());

        dashboard(token, null)
                .andExpect(jsonPath("$.data.trainingsThisMonth.value").value(0))
                .andExpect(jsonPath("$.data.trainingsThisMonth.previous").value(1))
                .andExpect(jsonPath("$.data.modalitiesPracticed.value").value(0))
                .andExpect(jsonPath("$.data.modalitiesPracticed.previous").value(1));
    }

    @Test
    void shouldListOnlyThreeLatestTrainingsFilteredByModality() throws Exception {
        String token = registerAndLogin("atleta.tabsultimos@shottrack.com");
        UUID oldestIpsc = training(token, "IPSC");
        UUID ipsc2 = training(token, "IPSC");
        training(token, "Trap");
        UUID ipsc3 = training(token, "IPSC");
        UUID ipsc4 = training(token, "IPSC");
        UUID ipscId = modalityId(mockMvc, objectMapper, token, "IPSC");

        dashboard(token, null)
                .andExpect(jsonPath("$.data.recentTrainings.length()").value(3))
                .andExpect(jsonPath("$.data.recentTrainings[0].trainingId").value(ipsc4.toString()));

        dashboard(token, ipscId)
                .andExpect(jsonPath("$.data.recentTrainings.length()").value(3))
                .andExpect(jsonPath("$.data.recentTrainings[*].trainingId",
                        contains(ipsc4.toString(), ipsc3.toString(), ipsc2.toString())))
                .andExpect(jsonPath("$.data.recentTrainings[?(@.trainingId == '" + oldestIpsc + "')]").isEmpty())
                .andExpect(jsonPath("$.data.recentTrainings[?(@.modalityName == 'Trap')]").isEmpty());
    }

    @Test
    void shouldFilterRecordsByModalityOnFallbackAndOnPrecalculatedSummary() throws Exception {
        String token = registerAndLogin("atleta.tabsrecordes@shottrack.com");
        UUID userId = currentUserId(token);
        UUID tempo = resultTypeId(mockMvc, objectMapper, token, "Tempo");
        UUID pontuacao = resultTypeId(mockMvc, objectMapper, token, "Pontuação");
        registerSeriesWithResult(mockMvc, objectMapper, token, training(token, "IPSC"), tempo, "9.8");
        registerSeriesWithResult(mockMvc, objectMapper, token, training(token, "Precisão"), pontuacao, "98");
        UUID ipscId = modalityId(mockMvc, objectMapper, token, "IPSC");

        // fallback: ainda não existe resumo
        assertThat(dashboardSummaryRepository.findByUserId(userId)).isEmpty();
        assertRecordsFilteredToTempo(token, ipscId);
        assertThat(dashboardSummaryRepository.findByUserId(userId)).isEmpty();

        // resumo pré-calculado
        dashboardRecalculationService.recalculate(userId);
        entityManager.flush();
        entityManager.clear();
        assertThat(dashboardSummaryRepository.findByUserId(userId)).isPresent();
        assertRecordsFilteredToTempo(token, ipscId);
    }

    private void assertRecordsFilteredToTempo(String token, UUID ipscId) throws Exception {
        dashboard(token, null)
                .andExpect(jsonPath("$.data.records.length()").value(2));
        dashboard(token, ipscId)
                .andExpect(jsonPath("$.data.records.length()").value(1))
                .andExpect(jsonPath("$.data.records[0].resultTypeName").value("Tempo"))
                .andExpect(jsonPath("$.data.records[0].value").value(9.8));
    }

    @Test
    void shouldSuggestMostRecordedResultTypeForCurrentFilter() throws Exception {
        String token = registerAndLogin("atleta.tabssugestao@shottrack.com");
        UUID tempo = resultTypeId(mockMvc, objectMapper, token, "Tempo");
        UUID pontuacao = resultTypeId(mockMvc, objectMapper, token, "Pontuação");
        UUID ipscId = modalityId(mockMvc, objectMapper, token, "IPSC");
        UUID precisaoId = modalityId(mockMvc, objectMapper, token, "Precisão");

        UUID ipsc = training(token, "IPSC");
        configureResultType(mockMvc, objectMapper, token, ipscId, tempo);
        configureResultType(mockMvc, objectMapper, token, ipscId, pontuacao);
        registerSeriesWithResult(mockMvc, objectMapper, token, ipsc, tempo, "10.0");
        registerSeriesWithResult(mockMvc, objectMapper, token, ipsc, tempo, "9.0");
        registerSeriesWithResult(mockMvc, objectMapper, token, ipsc, pontuacao, "90");
        UUID precisao = training(token, "Precisão");
        configureResultType(mockMvc, objectMapper, token, precisaoId, pontuacao);
        for (String value : new String[]{"90", "91", "92"}) {
            registerSeriesWithResult(mockMvc, objectMapper, token, precisao, pontuacao, value);
        }

        // com filtro IPSC: Tempo (2 registros) vence Pontuação (1)
        dashboard(token, ipscId)
                .andExpect(jsonPath("$.data.suggestedEvolution.modalityId").value(ipscId.toString()))
                .andExpect(jsonPath("$.data.suggestedEvolution.resultTypeId").value(tempo.toString()));
        // com filtro Precisão: Pontuação
        dashboard(token, precisaoId)
                .andExpect(jsonPath("$.data.suggestedEvolution.modalityId").value(precisaoId.toString()))
                .andExpect(jsonPath("$.data.suggestedEvolution.resultTypeId").value(pontuacao.toString()));
        // sem filtro: o par mais registrado de todos (Pontuação em Precisão, 3 registros)
        dashboard(token, null)
                .andExpect(jsonPath("$.data.suggestedEvolution.modalityId").value(precisaoId.toString()))
                .andExpect(jsonPath("$.data.suggestedEvolution.resultTypeId").value(pontuacao.toString()));
    }

    @Test
    void shouldReturnNullSuggestedEvolutionWithoutDataInFilter() throws Exception {
        String token = registerAndLogin("atleta.tabssugestaonula@shottrack.com");
        UUID tempo = resultTypeId(mockMvc, objectMapper, token, "Tempo");
        UUID ipscId = modalityId(mockMvc, objectMapper, token, "IPSC");
        UUID trapId = modalityId(mockMvc, objectMapper, token, "Trap");

        // atleta sem nenhum dado
        dashboard(token, null).andExpect(jsonPath("$.data.suggestedEvolution").value(nullValue()));

        UUID ipsc = training(token, "IPSC");
        configureResultType(mockMvc, objectMapper, token, ipscId, tempo);
        registerSeriesWithResult(mockMvc, objectMapper, token, ipsc, tempo, "10.0");
        training(token, "Trap");

        // modalidade praticada mas sem resultado elegível
        dashboard(token, trapId).andExpect(jsonPath("$.data.suggestedEvolution").value(nullValue()));
        dashboard(token, ipscId).andExpect(jsonPath("$.data.suggestedEvolution.resultTypeId").value(tempo.toString()));
    }

    @Test
    void shouldNotSuggestResultTypeNoLongerConfiguredInModality() throws Exception {
        String token = registerAndLogin("atleta.tabssugestaosemconfig@shottrack.com");
        UUID tempo = resultTypeId(mockMvc, objectMapper, token, "Tempo");
        UUID ipscId = modalityId(mockMvc, objectMapper, token, "IPSC");
        UUID ipsc = training(token, "IPSC");
        registerSeriesWithResult(mockMvc, objectMapper, token, ipsc, tempo, "10.0");
        dashboard(token, null).andExpect(jsonPath("$.data.suggestedEvolution.resultTypeId").value(tempo.toString()));

        // o atleta deixa de usar Tempo em IPSC (UC30): o registro continua nos recordes,
        // mas o UC46 rejeitaria o tipo, então a evolução não o sugere mais
        mockMvc.perform(delete("/api/practiced-modalities/" + ipscId + "/result-types/" + tempo)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().is2xxSuccessful());

        dashboard(token, null)
                .andExpect(jsonPath("$.data.records.length()").value(1))
                .andExpect(jsonPath("$.data.suggestedEvolution").value(nullValue()));
    }

    @Test
    void shouldRejectModalityNotPracticed() throws Exception {
        String token = registerAndLogin("atleta.tabsnaopraticada@shottrack.com");
        training(token, "IPSC");
        UUID trapId = modalityId(mockMvc, objectMapper, token, "Trap");

        dashboard(token, trapId)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("MODALITY_NOT_PRACTICED"));
        dashboard(token, UUID.randomUUID())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("MODALITY_NOT_PRACTICED"));
    }

    @Test
    void shouldRejectInvalidModalityId() throws Exception {
        String token = registerAndLogin("atleta.tabsidinvalido@shottrack.com");

        mockMvc.perform(get("/api/dashboard").param("modalityId", "nao-e-uuid")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    void shouldRejectRequestWithoutToken() throws Exception {
        mockMvc.perform(get("/api/dashboard").param("modalityId", UUID.randomUUID().toString()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
    }

    @Test
    void shouldIsolateDataBetweenAthletes() throws Exception {
        String tokenA = registerAndLogin("atleta.tabsisolamentoa@shottrack.com");
        String tokenB = registerAndLogin("atleta.tabsisolamentob@shottrack.com");
        UUID tempo = resultTypeId(mockMvc, objectMapper, tokenB, "Tempo");
        training(tokenA, "IPSC");
        UUID trainingB = training(tokenB, "IPSC");
        UUID trapTrainingB = training(tokenB, "Trap");
        registerSeries(mockMvc, objectMapper, tokenB, trapTrainingB, 99);
        registerSeriesWithResult(mockMvc, objectMapper, tokenB, trainingB, tempo, "5.0");
        UUID trapId = modalityId(mockMvc, objectMapper, tokenA, "Trap");

        dashboard(tokenA, null)
                .andExpect(jsonPath("$.data.trainingsThisMonth.value").value(1))
                .andExpect(jsonPath("$.data.shotsThisMonth.value").value(0))
                .andExpect(jsonPath("$.data.modalitiesPracticed.value").value(1))
                .andExpect(jsonPath("$.data.records.length()").value(0))
                .andExpect(jsonPath("$.data.recentTrainings.length()").value(1))
                .andExpect(jsonPath("$.data.suggestedEvolution").value(nullValue()));
        // Trap só é praticada pelo B
        dashboard(tokenA, trapId).andExpect(status().isNotFound());
        mockMvc.perform(get("/api/dashboard/modalities").header("Authorization", "Bearer " + tokenA))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].modalityName").value("IPSC"));
    }

    @Test
    void shouldFilterOnFallbackWithoutPersistingSummary() throws Exception {
        String token = registerAndLogin("atleta.tabsfallbackfiltro@shottrack.com");
        UUID userId = currentUserId(token);
        UUID tempo = resultTypeId(mockMvc, objectMapper, token, "Tempo");
        UUID ipscId = modalityId(mockMvc, objectMapper, token, "IPSC");
        UUID ipsc = training(token, "IPSC");
        configureResultType(mockMvc, objectMapper, token, ipscId, tempo);
        registerSeriesWithResult(mockMvc, objectMapper, token, ipsc, tempo, "8.5");
        training(token, "Trap");

        dashboard(token, ipscId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.trainingsThisMonth.value").value(1))
                .andExpect(jsonPath("$.data.records.length()").value(1))
                .andExpect(jsonPath("$.data.records[0].value").value(8.5))
                .andExpect(jsonPath("$.data.suggestedEvolution.resultTypeId").value(tempo.toString()))
                .andExpect(jsonPath("$.data.recentTrainings.length()").value(1));

        assertThat(dashboardSummaryRepository.findByUserId(userId)).isEmpty();
    }

    @Test
    void shouldListModalitiesIgnoringAnyFilter() throws Exception {
        String token = registerAndLogin("atleta.tabsmodalidades@shottrack.com");
        UUID tempo = resultTypeId(mockMvc, objectMapper, token, "Tempo");
        UUID ipscId = modalityId(mockMvc, objectMapper, token, "IPSC");
        registerSeriesWithResult(mockMvc, objectMapper, token, training(token, "IPSC"), tempo, "9.8");
        training(token, "Trap");

        mockMvc.perform(get("/api/dashboard/modalities").param("modalityId", ipscId.toString())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].modalityId").value(ipscId.toString()))
                .andExpect(jsonPath("$.data[0].modalityName").value("IPSC"))
                .andExpect(jsonPath("$.data[0].trainingCount").value(1))
                .andExpect(jsonPath("$.data[0].best.resultTypeName").value("Tempo"))
                .andExpect(jsonPath("$.data[0].best.value").value(9.8))
                .andExpect(jsonPath("$.data[1].modalityName").value("Trap"));
    }

    @Test
    void shouldListNoModalitiesWhenAthleteNeverTrained() throws Exception {
        String token = registerAndLogin("atleta.tabsmodalidadesvazio@shottrack.com");

        mockMvc.perform(get("/api/dashboard/modalities").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    private ResultActions dashboard(String token, UUID modalityId) throws Exception {
        var request = get("/api/dashboard").header("Authorization", "Bearer " + token);
        if (modalityId != null) {
            request.param("modalityId", modalityId.toString());
        }
        return mockMvc.perform(request);
    }

    private UUID training(String token, String modalityName) throws Exception {
        return SeriesTestSupport.openTraining(mockMvc, objectMapper, token, modalityName);
    }

    private void backdate(UUID trainingId, LocalDate day) {
        SeriesTestSupport.backdateTraining(entityManager, trainingId, day);
    }

    private LocalDate previousMonthFirstDay() {
        return LocalDate.now(ZoneOffset.UTC).withDayOfMonth(1).minusMonths(1);
    }

    private UUID currentUserId(String token) throws Exception {
        var result = mockMvc.perform(get("/api/users/me").header("Authorization", "Bearer " + token)).andReturn();
        return UUID.fromString(objectMapper.readTree(result.getResponse().getContentAsString()).get("data").get("id").asText());
    }

    private String registerAndLogin(String email) throws Exception {
        return TestUsers.registerAndLogin(mockMvc, objectMapper, pendingRegistrationRepository, "Atleta Teste", email, PASSWORD);
    }
}
