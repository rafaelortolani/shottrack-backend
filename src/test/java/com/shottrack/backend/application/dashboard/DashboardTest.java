package com.shottrack.backend.application.dashboard;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shottrack.backend.application.dashboard.gateway.repository.DashboardSummaryRepository;
import com.shottrack.backend.application.dashboard.model.DashboardSummary;
import com.shottrack.backend.application.dashboard.model.ModalityStats;
import com.shottrack.backend.application.modality.dto.AddModalityResultTypeRequest;
import com.shottrack.backend.application.modality.dto.AddPracticedModalityRequest;
import com.shottrack.backend.application.series.dto.RegisterSeriesRequest;
import com.shottrack.backend.application.series.dto.RegisterSeriesResultRequest;
import com.shottrack.backend.application.user.dto.UpdateProfileRequest;
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
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * UC42/ADR-0015: GET /api/dashboard. A lógica de recálculo em si (destaque
 * dinâmico, ignorar valor não numérico etc.) é coberta em
 * DashboardRecalculationServiceTest — aqui o foco é o contrato HTTP, os
 * dois caminhos de leitura (resumo pré-calculado vs. fallback) e as seções
 * calculadas na hora (onboarding, ação principal, últimos treinos, acervo).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class DashboardTest {

    private static final String PASSWORD = "senha12345";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PendingRegistrationRepository pendingRegistrationRepository;

    @Autowired
    private DashboardSummaryRepository dashboardSummaryRepository;

    @Test
    void shouldReadPrecalculatedSummaryWhenItAlreadyExists() throws Exception {
        String token = registerAndLogin("atleta.dashboardresumoexistente@shottrack.com");
        UUID userId = currentUserId(token);

        // valores propositalmente diferentes de qualquer coisa calculável a
        // partir dos dados reais do atleta (não há nenhum) — prova que a
        // leitura vem do resumo, não de um recálculo na hora
        DashboardSummary summary = DashboardSummary.builder().userId(userId).build();
        summary.replaceWith(7, 123, List.of("Trap", "Skeet"),
                List.of(
                        ModalityStats.builder().modalityName("Skeet").trainingCount(4).build(),
                        ModalityStats.builder().modalityName("Trap").trainingCount(3)
                                .bestResultTypeName("Acertos").bestValue(new BigDecimal("24")).build()),
                "Agrupamento", new BigDecimal("3.30"));
        dashboardSummaryRepository.save(summary);

        mockMvc.perform(get("/api/dashboard")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.trainingsThisMonth").value(7))
                .andExpect(jsonPath("$.data.shotsThisMonth").value(123))
                .andExpect(jsonPath("$.data.practicedModalities", containsInAnyOrder("Trap", "Skeet")))
                .andExpect(jsonPath("$.data.modalitySummaries.length()").value(2))
                .andExpect(jsonPath("$.data.modalitySummaries[?(@.modalityName == 'Skeet')].trainingCount").value(4))
                .andExpect(jsonPath("$.data.modalitySummaries[?(@.modalityName == 'Trap')].best.resultTypeName").value("Acertos"))
                .andExpect(jsonPath("$.data.modalitySummaries[?(@.modalityName == 'Trap')].best.value").value(24))
                .andExpect(jsonPath("$.data.highlight.resultTypeName").value("Agrupamento"))
                .andExpect(jsonPath("$.data.highlight.value").value(3.30))
                .andExpect(jsonPath("$.data.recentTrainings.length()").value(0));
    }

    @Test
    void shouldFallBackToOnTheFlyCalculationWhenNoSummaryExistsYet() throws Exception {
        String token = registerAndLogin("atleta.dashboardfallback@shottrack.com");
        UUID userId = currentUserId(token);

        UUID trainingId = SeriesTestSupport.openTraining(mockMvc, objectMapper, token, "IPSC");
        UUID modalityId = modalityIdByName(token, "IPSC");
        UUID tempoId = resultTypeIdByName(token, "Tempo");
        mockMvc.perform(post("/api/practiced-modalities/" + modalityId + "/result-types")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new AddModalityResultTypeRequest(tempoId))));

        UUID seriesId = registerSeriesWithShotCount(token, trainingId, 25);
        mockMvc.perform(post("/api/series/" + seriesId + "/results")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new RegisterSeriesResultRequest(tempoId, "9.8"))));

        assertThat(dashboardSummaryRepository.findByUserId(userId)).isEmpty();

        mockMvc.perform(get("/api/dashboard")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.trainingsThisMonth").value(1))
                .andExpect(jsonPath("$.data.shotsThisMonth").value(25))
                .andExpect(jsonPath("$.data.practicedModalities", contains("IPSC")))
                .andExpect(jsonPath("$.data.highlight.resultTypeName").value("Tempo"))
                .andExpect(jsonPath("$.data.highlight.value").value(9.8));

        // fallback só calcula, nunca grava
        assertThat(dashboardSummaryRepository.findByUserId(userId)).isEmpty();
    }

    @Test
    void shouldReturnAllOnboardingStepsAndEmptySectionsWhenNoDataYet() throws Exception {
        String token = registerAndLogin("atleta.dashboardsemdados@shottrack.com");

        mockMvc.perform(get("/api/dashboard")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.onboarding.pendingSteps",
                        contains("CREATE_PROFILE", "CONFIGURE_MODALITIES", "REGISTER_WEAPON")))
                .andExpect(jsonPath("$.data.mainAction.type").value("START_VISIT"))
                .andExpect(jsonPath("$.data.trainingsThisMonth").value(0))
                .andExpect(jsonPath("$.data.shotsThisMonth").value(0))
                .andExpect(jsonPath("$.data.practicedModalities.length()").value(0))
                .andExpect(jsonPath("$.data.recentTrainings.length()").value(0))
                .andExpect(jsonPath("$.data.modalitySummaries.length()").value(0))
                .andExpect(jsonPath("$.data.weaponCollection.weaponCount").value(0))
                .andExpect(jsonPath("$.data.weaponCollection.weaponNames.length()").value(0))
                .andExpect(jsonPath("$.data.highlight").isEmpty());
    }

    @Test
    void shouldListOnlyRemainingOnboardingStep() throws Exception {
        String token = registerAndLogin("atleta.dashboardonboardingparcial@shottrack.com");
        completeProfile(token);
        addPracticedModality(token, "IPSC");

        mockMvc.perform(get("/api/dashboard")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.onboarding.pendingSteps", contains("REGISTER_WEAPON")));
    }

    @Test
    void shouldOmitOnboardingWhenEverythingIsComplete() throws Exception {
        String token = registerAndLogin("atleta.dashboardonboardingcompleto@shottrack.com");
        completeProfile(token);
        addPracticedModality(token, "IPSC");
        SeriesTestSupport.registerWeapon(mockMvc, objectMapper, token);

        mockMvc.perform(get("/api/dashboard")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.onboarding").doesNotExist());
    }

    @Test
    void shouldPointMainActionToActiveVisit() throws Exception {
        String token = registerAndLogin("atleta.dashboardvisitaativa@shottrack.com");
        UUID visitId = SeriesTestSupport.openVisit(mockMvc, objectMapper, token);
        SeriesTestSupport.openTrainingInVisit(mockMvc, objectMapper, token, visitId, "IPSC");

        mockMvc.perform(get("/api/dashboard")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.mainAction.type").value("CONTINUE_VISIT"))
                .andExpect(jsonPath("$.data.mainAction.activeVisit.visitId").value(visitId.toString()))
                .andExpect(jsonPath("$.data.mainAction.activeVisit.trainingLocationName").value("Clube de Tiro Central"))
                .andExpect(jsonPath("$.data.mainAction.activeVisit.activeTrainingModalityNames", contains("IPSC")));
    }

    @Test
    void shouldInviteToStartVisitWhenNoneIsActive() throws Exception {
        String token = registerAndLogin("atleta.dashboardsemvisitaativa@shottrack.com");
        UUID visitId = SeriesTestSupport.openVisit(mockMvc, objectMapper, token);
        mockMvc.perform(post("/api/visits/" + visitId + "/closure")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/dashboard")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.mainAction.type").value("START_VISIT"))
                .andExpect(jsonPath("$.data.mainAction.activeVisit").isEmpty());
    }

    @Test
    void shouldListLastFiveTrainingsWithHighlightScopedToEachTraining() throws Exception {
        String token = registerAndLogin("atleta.dashboardultimostreinos@shottrack.com");
        UUID tempoId = resultTypeIdByName(token, "Tempo");

        // mais antigo — fica fora dos 5, mesmo tendo o melhor tempo geral
        UUID oldest = SeriesTestSupport.openTraining(mockMvc, objectMapper, token, "IPSC");
        registerSeriesWithResult(token, oldest, tempoId, "5.0");
        SeriesTestSupport.openTraining(mockMvc, objectMapper, token, "IPSC");
        SeriesTestSupport.openTraining(mockMvc, objectMapper, token, "IPSC");
        UUID withTwoResults = SeriesTestSupport.openTraining(mockMvc, objectMapper, token, "IPSC");
        registerSeriesWithResult(token, withTwoResults, tempoId, "12.0");
        registerSeriesWithResult(token, withTwoResults, tempoId, "10.0");
        UUID withoutResults = SeriesTestSupport.openTraining(mockMvc, objectMapper, token, "IPSC");
        UUID newest = SeriesTestSupport.openTraining(mockMvc, objectMapper, token, "IPSC");
        registerSeriesWithResult(token, newest, tempoId, "8.0");

        mockMvc.perform(get("/api/dashboard")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.recentTrainings.length()").value(5))
                .andExpect(jsonPath("$.data.recentTrainings[0].trainingId").value(newest.toString()))
                .andExpect(jsonPath("$.data.recentTrainings[0].trainingLocationName").value("Clube de Tiro Central"))
                .andExpect(jsonPath("$.data.recentTrainings[0].modalityName").value("IPSC"))
                .andExpect(jsonPath("$.data.recentTrainings[0].startedAt").isNotEmpty())
                .andExpect(jsonPath("$.data.recentTrainings[0].highlight.resultTypeName").value("Tempo"))
                .andExpect(jsonPath("$.data.recentTrainings[0].highlight.value").value(8.0))
                .andExpect(jsonPath("$.data.recentTrainings[1].trainingId").value(withoutResults.toString()))
                .andExpect(jsonPath("$.data.recentTrainings[1].highlight").doesNotExist())
                .andExpect(jsonPath("$.data.recentTrainings[2].trainingId").value(withTwoResults.toString()))
                .andExpect(jsonPath("$.data.recentTrainings[2].highlight.value").value(10.0))
                .andExpect(jsonPath("$.data.recentTrainings[?(@.trainingId == '" + oldest + "')]").isEmpty())
                // destaque geral continua olhando o histórico inteiro
                .andExpect(jsonPath("$.data.highlight.value").value(5.0));
    }

    @Test
    void shouldSummarizeModalitiesWithBestValuePerModality() throws Exception {
        String token = registerAndLogin("atleta.dashboardresumomodalidades@shottrack.com");
        UUID tempoId = resultTypeIdByName(token, "Tempo");
        UUID pontuacaoId = resultTypeIdByName(token, "Pontuação");

        // IPSC: Tempo é MENOR_MELHOR
        UUID firstIpsc = SeriesTestSupport.openTraining(mockMvc, objectMapper, token, "IPSC");
        registerSeriesWithResult(token, firstIpsc, tempoId, "11.0");
        UUID secondIpsc = SeriesTestSupport.openTraining(mockMvc, objectMapper, token, "IPSC");
        registerSeriesWithResult(token, secondIpsc, tempoId, "9.8");
        // Precisão: Pontuação é MAIOR_MELHOR
        UUID precisao = SeriesTestSupport.openTraining(mockMvc, objectMapper, token, "Precisão");
        registerSeriesWithResult(token, precisao, pontuacaoId, "95");
        registerSeriesWithResult(token, precisao, pontuacaoId, "98");
        // Trap: treino sem resultado nenhum
        SeriesTestSupport.openTraining(mockMvc, objectMapper, token, "Trap");

        mockMvc.perform(get("/api/dashboard")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.modalitySummaries.length()").value(3))
                .andExpect(jsonPath("$.data.modalitySummaries[0].modalityName").value("IPSC"))
                .andExpect(jsonPath("$.data.modalitySummaries[0].trainingCount").value(2))
                .andExpect(jsonPath("$.data.modalitySummaries[0].best.resultTypeName").value("Tempo"))
                .andExpect(jsonPath("$.data.modalitySummaries[0].best.value").value(9.8))
                .andExpect(jsonPath("$.data.modalitySummaries[1].modalityName").value("Precisão"))
                .andExpect(jsonPath("$.data.modalitySummaries[1].trainingCount").value(1))
                .andExpect(jsonPath("$.data.modalitySummaries[1].best.resultTypeName").value("Pontuação"))
                .andExpect(jsonPath("$.data.modalitySummaries[1].best.value").value(98))
                .andExpect(jsonPath("$.data.modalitySummaries[2].modalityName").value("Trap"))
                .andExpect(jsonPath("$.data.modalitySummaries[2].trainingCount").value(1))
                .andExpect(jsonPath("$.data.modalitySummaries[2].best").isEmpty());
    }

    @Test
    void shouldSummarizeWeaponCollection() throws Exception {
        String token = registerAndLogin("atleta.dashboardacervo@shottrack.com");
        SeriesTestSupport.registerWeapon(mockMvc, objectMapper, token);
        SeriesTestSupport.registerWeapon(mockMvc, objectMapper, token);
        SeriesTestSupport.registerWeapon(mockMvc, objectMapper, token);
        SeriesTestSupport.registerWeapon(mockMvc, objectMapper, token, "Minha Glock");

        mockMvc.perform(get("/api/dashboard")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.weaponCollection.weaponCount").value(4))
                .andExpect(jsonPath("$.data.weaponCollection.weaponNames",
                        contains("Minha Glock", "Glock G17", "Glock G17")));
    }

    @Test
    void shouldRejectRequestWithoutToken() throws Exception {
        mockMvc.perform(get("/api/dashboard"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
    }

    private void completeProfile(String token) throws Exception {
        mockMvc.perform(patch("/api/users/me")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateProfileRequest("Atleta Teste", "INTERMEDIATE"))))
                .andExpect(status().isOk());
    }

    private void addPracticedModality(String token, String modalityName) throws Exception {
        mockMvc.perform(post("/api/practiced-modalities")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AddPracticedModalityRequest(modalityIdByName(token, modalityName)))))
                .andExpect(status().is2xxSuccessful());
    }

    private void registerSeriesWithResult(String token, UUID trainingId, UUID resultTypeId, String value) throws Exception {
        var series = mockMvc.perform(post("/api/series")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new RegisterSeriesRequest(trainingId, null, null, null, null, null, null))))
                .andExpect(status().isCreated())
                .andReturn();

        mockMvc.perform(post("/api/series/" + idFromResponse(series) + "/results")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RegisterSeriesResultRequest(resultTypeId, value))))
                .andExpect(status().isOk());
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
