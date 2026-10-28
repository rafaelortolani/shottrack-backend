package com.shottrack.backend.application.dashboard;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shottrack.backend.application.dashboard.model.EvolutionPeriod;
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
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static com.shottrack.backend.support.DashboardTestSupport.configureResultType;
import static com.shottrack.backend.support.DashboardTestSupport.modalityId;
import static com.shottrack.backend.support.DashboardTestSupport.registerSeries;
import static com.shottrack.backend.support.DashboardTestSupport.registerSeriesWithResult;
import static com.shottrack.backend.support.DashboardTestSupport.resultTypeId;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * UC46: parâmetro comparePrevious — período imediatamente anterior, de mesma
 * duração, mesmo modo e mesmas regras. O comportamento sem o parâmetro está em
 * EvolutionTest (inalterado).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class EvolutionComparePreviousTest {

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
    void shouldReturnPreviousPointsForEachPeriodInMediaMode() throws Exception {
        String token = registerAndLogin("atleta.compararperiodos@shottrack.com");
        UUID ipscId = modalityId(mockMvc, objectMapper, token, "IPSC");
        UUID tempoId = resultTypeId(mockMvc, objectMapper, token, "Tempo");

        for (String period : new String[]{"7d", "30d", "3m", "1a"}) {
            EvolutionPeriod evolutionPeriod = EvolutionPeriod.fromCode(period);
            LocalDate today = LocalDate.now(ZoneOffset.UTC);
            LocalDate firstDay = evolutionPeriod.firstDay(today);
            long days = ChronoUnit.DAYS.between(firstDay, today) + 1;
            LocalDate previousFirstDay = firstDay.minusDays(days);

            // um ponto por janela (primeiro e último dia da anterior) + um fora das duas janelas
            UUID inCurrent = trainingWithTempo(token, tempoId, today, "10.0");
            UUID previousFirst = trainingWithTempo(token, tempoId, previousFirstDay, "12.0");
            UUID previousLast = trainingWithTempo(token, tempoId, firstDay.minusDays(1), "14.0");
            UUID previousSameDay = trainingWithTempo(token, tempoId, firstDay.minusDays(1), "16.0");
            UUID tooOld = trainingWithTempo(token, tempoId, previousFirstDay.minusDays(1), "99.0");

            evolution(token, ipscId, tempoId, period, "MEDIA", true)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.points[*].date").value(contains(today.toString())))
                    .andExpect(jsonPath("$.data.points[*].value").value(contains(10.0)))
                    .andExpect(jsonPath("$.data.previousPoints[*].date")
                            .value(contains(previousFirstDay.toString(), firstDay.minusDays(1).toString())))
                    // dois registros no último dia anterior: média de 14 e 16
                    .andExpect(jsonPath("$.data.previousPoints[*].value").value(contains(12.0, 15.0)));

            // limpa pra próxima iteração não somar dados do período anterior
            for (UUID id : new UUID[]{inCurrent, previousFirst, previousLast, previousSameDay, tooOld}) {
                SeriesTestSupport.backdateTraining(entityManager, id, LocalDate.of(2000, 1, 1));
            }
        }
    }

    @Test
    void shouldReturnBestOfDayInMelhorModeRespectingOrientation() throws Exception {
        String token = registerAndLogin("atleta.compararmelhor@shottrack.com");
        UUID ipscId = modalityId(mockMvc, objectMapper, token, "IPSC");
        UUID tempoId = resultTypeId(mockMvc, objectMapper, token, "Tempo");
        UUID pontuacaoId = resultTypeId(mockMvc, objectMapper, token, "Pontuação");
        LocalDate previousDay = LocalDate.now(ZoneOffset.UTC).minusDays(10);

        UUID training = SeriesTestSupport.openTraining(mockMvc, objectMapper, token, "IPSC");
        configureResultType(mockMvc, objectMapper, token, ipscId, tempoId);
        configureResultType(mockMvc, objectMapper, token, ipscId, pontuacaoId);
        for (String value : new String[]{"12.0", "9.5", "11.0"}) {
            registerSeriesWithResult(mockMvc, objectMapper, token, training, tempoId, value);
        }
        for (String value : new String[]{"80", "95", "90"}) {
            registerSeriesWithResult(mockMvc, objectMapper, token, training, pontuacaoId, value);
        }
        SeriesTestSupport.backdateTraining(entityManager, training, previousDay);

        // Tempo é MENOR_MELHOR
        evolution(token, ipscId, tempoId, "7d", "MELHOR", true)
                .andExpect(jsonPath("$.data.points").value(empty()))
                .andExpect(jsonPath("$.data.previousPoints.length()").value(1))
                .andExpect(jsonPath("$.data.previousPoints[0].date").value(previousDay.toString()))
                .andExpect(jsonPath("$.data.previousPoints[0].value").value(9.5));
        // Pontuação é MAIOR_MELHOR
        evolution(token, ipscId, pontuacaoId, "7d", "MELHOR", true)
                .andExpect(jsonPath("$.data.previousPoints[0].value").value(95));
        // e a média usa o mesmo dia
        evolution(token, ipscId, pontuacaoId, "7d", "MEDIA", true)
                .andExpect(jsonPath("$.data.previousPoints[0].value").value(88.33333333333333));
    }

    @Test
    void shouldReturnEmptyPreviousPointsWhenPreviousPeriodHasNoData() throws Exception {
        String token = registerAndLogin("atleta.compararvazio@shottrack.com");
        UUID ipscId = modalityId(mockMvc, objectMapper, token, "IPSC");
        UUID tempoId = resultTypeId(mockMvc, objectMapper, token, "Tempo");
        trainingWithTempo(token, tempoId, LocalDate.now(ZoneOffset.UTC).minusDays(1), "10.0");

        evolution(token, ipscId, tempoId, "7d", "MEDIA", true)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.points.length()").value(1))
                .andExpect(jsonPath("$.data.previousPoints").isArray())
                .andExpect(jsonPath("$.data.previousPoints").value(empty()));
    }

    @Test
    void shouldApplySameRulesToPreviousPeriod() throws Exception {
        String token = registerAndLogin("atleta.compararregras@shottrack.com");
        UUID ipscId = modalityId(mockMvc, objectMapper, token, "IPSC");
        UUID tempoId = resultTypeId(mockMvc, objectMapper, token, "Tempo");
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        LocalDate zeroDay = today.minusDays(8);
        LocalDate emptyDay = today.minusDays(9);

        // "0" é valor válido
        trainingWithTempo(token, tempoId, zeroDay, "0");
        // treino com série mas sem valor do tipo: o dia não gera ponto (nem zero)
        UUID withoutValue = SeriesTestSupport.openTraining(mockMvc, objectMapper, token, "IPSC");
        configureResultType(mockMvc, objectMapper, token, ipscId, tempoId);
        registerSeries(mockMvc, objectMapper, token, withoutValue, 5);
        SeriesTestSupport.backdateTraining(entityManager, withoutValue, emptyDay);
        // outra modalidade não entra
        UUID trap = SeriesTestSupport.openTraining(mockMvc, objectMapper, token, "Trap");
        SeriesTestSupport.backdateTraining(entityManager, trap, zeroDay);

        evolution(token, ipscId, tempoId, "7d", "MEDIA", true)
                .andExpect(jsonPath("$.data.previousPoints.length()").value(1))
                .andExpect(jsonPath("$.data.previousPoints[0].date").value(zeroDay.toString()))
                .andExpect(jsonPath("$.data.previousPoints[0].value").value(0));
    }

    @Test
    void shouldKeepResponseUnchangedWithoutTheParameterOrWithFalse() throws Exception {
        String token = registerAndLogin("atleta.compararsemparametro@shottrack.com");
        UUID ipscId = modalityId(mockMvc, objectMapper, token, "IPSC");
        UUID tempoId = resultTypeId(mockMvc, objectMapper, token, "Tempo");
        LocalDate yesterday = LocalDate.now(ZoneOffset.UTC).minusDays(1);
        trainingWithTempo(token, tempoId, yesterday, "10.0");
        trainingWithTempo(token, tempoId, LocalDate.now(ZoneOffset.UTC).minusDays(10), "20.0");

        // sem o parâmetro: data continua sendo a lista de pontos
        mockMvc.perform(get("/api/dashboard/evolution")
                        .header("Authorization", "Bearer " + token)
                        .param("modalityId", ipscId.toString())
                        .param("resultTypeId", tempoId.toString())
                        .param("period", "7d")
                        .param("mode", "MEDIA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].date").value(yesterday.toString()))
                .andExpect(jsonPath("$.data[0].value").value(10.0));

        evolution(token, ipscId, tempoId, "7d", "MEDIA", false)
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(1));
    }

    @Test
    void shouldRejectInvalidComparePreviousValue() throws Exception {
        String token = registerAndLogin("atleta.compararinvalido@shottrack.com");

        mockMvc.perform(get("/api/dashboard/evolution")
                        .header("Authorization", "Bearer " + token)
                        .param("modalityId", UUID.randomUUID().toString())
                        .param("resultTypeId", UUID.randomUUID().toString())
                        .param("period", "7d")
                        .param("mode", "MEDIA")
                        .param("comparePrevious", "talvez"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    private ResultActions evolution(String token, UUID modalityId, UUID resultTypeId, String period, String mode,
                                    boolean comparePrevious) throws Exception {
        return mockMvc.perform(get("/api/dashboard/evolution")
                .header("Authorization", "Bearer " + token)
                .param("modalityId", modalityId.toString())
                .param("resultTypeId", resultTypeId.toString())
                .param("period", period)
                .param("mode", mode)
                .param("comparePrevious", String.valueOf(comparePrevious)));
    }

    /** Treino de IPSC com um valor de Tempo, iniciado no dia informado (tipo configurado na modalidade). */
    private UUID trainingWithTempo(String token, UUID tempoId, LocalDate day, String value) throws Exception {
        UUID training = SeriesTestSupport.openTraining(mockMvc, objectMapper, token, "IPSC");
        configureResultType(mockMvc, objectMapper, token, modalityId(mockMvc, objectMapper, token, "IPSC"), tempoId);
        registerSeriesWithResult(mockMvc, objectMapper, token, training, tempoId, value);
        SeriesTestSupport.backdateTraining(entityManager, training, day);
        return training;
    }

    private String registerAndLogin(String email) throws Exception {
        return TestUsers.registerAndLogin(mockMvc, objectMapper, pendingRegistrationRepository, "Atleta Teste", email, PASSWORD);
    }
}
