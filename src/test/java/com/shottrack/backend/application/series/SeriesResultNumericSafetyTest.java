package com.shottrack.backend.application.series;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shottrack.backend.application.dashboard.usecase.DashboardRecalculationService;
import com.shottrack.backend.application.user.gateway.repository.PendingRegistrationRepository;
import com.shottrack.backend.support.DashboardTestSupport;
import com.shottrack.backend.support.SeriesTestSupport;
import com.shottrack.backend.support.TestUsers;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTimeout;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * S04: valores numéricos de resultado com expoente gigante, tamanho ou precisão fora da faixa são
 * recusados no cadastro (RESULT_VALUE_FORMAT_INVALID) e IGNORADOS nos cálculos quando já estão gravados.
 * O limite de tempo prova que nada tenta materializar bilhões de dígitos.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SeriesResultNumericSafetyTest {

    private static final Duration LIMIT = Duration.ofSeconds(20);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PendingRegistrationRepository pendingRegistrationRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private DashboardRecalculationService dashboardRecalculationService;

    private String token;
    private UUID trainingId;
    private UUID seriesId;
    private UUID ipsc;
    private UUID tempo;

    @BeforeEach
    void prepare() throws Exception {
        token = TestUsers.registerAndLogin(mockMvc, objectMapper, pendingRegistrationRepository,
                "Atleta", "atleta.numerico@shottrack.com", "senha12345");
        trainingId = SeriesTestSupport.openTraining(mockMvc, objectMapper, token, "IPSC");
        ipsc = DashboardTestSupport.modalityId(mockMvc, objectMapper, token, "IPSC");
        tempo = DashboardTestSupport.resultTypeId(mockMvc, objectMapper, token, "Tempo");
        DashboardTestSupport.configureResultType(mockMvc, objectMapper, token, ipsc, tempo);
        seriesId = DashboardTestSupport.registerSeries(mockMvc, objectMapper, token, trainingId, null);
    }

    private ResultActions register(UUID seriesId, UUID resultTypeId, String value) throws Exception {
        return mockMvc.perform(post("/api/series/" + seriesId + "/results")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("resultTypeId", resultTypeId, "value", value))));
    }

    private UUID configured(String typeName) throws Exception {
        UUID typeId = DashboardTestSupport.resultTypeId(mockMvc, objectMapper, token, typeName);
        DashboardTestSupport.configureResultType(mockMvc, objectMapper, token, ipsc, typeId);
        return typeId;
    }

    private void insertLegacyValue(UUID seriesId, UUID typeId, String value) {
        entityManager.flush();
        jdbcTemplate.update("INSERT INTO series_results (series_id, result_type_id, value) VALUES (?, ?, ?)",
                seriesId, typeId, value);
        entityManager.clear();
    }

    @Test
    void shouldRejectDangerousNumericValuesQuickly() throws Exception {
        assertTimeout(LIMIT, () -> {
            for (String value : new String[]{"1E+99999999", "1e999999999", "-1E+99999999", "1e3", "9".repeat(33),
                    "1".repeat(19), "0.0000001", "abc", "1,5"}) {
                register(seriesId, tempo, value)
                        .andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.error.code").value("RESULT_VALUE_FORMAT_INVALID"));
            }
        });
    }

    @Test
    void shouldAcceptNormalValuesIncludingZeroAndTheLimits() throws Exception {
        for (String value : new String[]{"9.8", "0", "-0.5", "123456789012345678", "0.000001", "12"}) {
            register(seriesId, tempo, value).andExpect(status().isOk()).andExpect(jsonPath("$.data.value").value(value));
        }
    }

    @Test
    void shouldRejectDangerousShotCountValuesAndKeepTheIntegerCeiling() throws Exception {
        UUID acertos = configured("Acertos");

        assertTimeout(LIMIT, () -> {
            for (String value : new String[]{"1E+99999999", "1e999999999", "-1E+99999999", "-1", "1.5", "1000001"}) {
                register(seriesId, acertos, value)
                        .andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.error.code").value("RESULT_VALUE_FORMAT_INVALID"));
            }
        });
        register(seriesId, acertos, "5.0").andExpect(status().isOk());
        register(seriesId, acertos, "1000000").andExpect(status().isOk());
        register(seriesId, acertos, "0").andExpect(status().isOk());
    }

    @Test
    void shouldIgnoreDangerousLegacyValuesInEvolutionDashboardAndRecalculation() throws Exception {
        UUID otherSeries = DashboardTestSupport.registerSeries(mockMvc, objectMapper, token, trainingId, null);
        UUID thirdSeries = DashboardTestSupport.registerSeries(mockMvc, objectMapper, token, trainingId, null);
        insertLegacyValue(seriesId, tempo, "1E+99999999");
        insertLegacyValue(otherSeries, tempo, "-1e999999999");
        register(thirdSeries, tempo, "10").andExpect(status().isOk());
        UUID userId = jdbcTemplate.queryForObject("SELECT id FROM users WHERE email = ?", UUID.class, "atleta.numerico@shottrack.com");

        assertTimeout(LIMIT, () -> {
            for (String mode : new String[]{"MEDIA", "MELHOR"}) {
                mockMvc.perform(get("/api/dashboard/evolution").header("Authorization", "Bearer " + token)
                                .param("modalityId", ipsc.toString()).param("resultTypeId", tempo.toString())
                                .param("period", "30d").param("mode", mode))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.length()").value(1))
                        .andExpect(jsonPath("$.data[0].value").value(10));
            }
            mockMvc.perform(get("/api/dashboard").header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.records.length()").value(1))
                    .andExpect(jsonPath("$.data.records[0].value").value(10));
            dashboardRecalculationService.recalculate(userId);
            mockMvc.perform(get("/api/dashboard").header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.records[0].value").value(10));
        });
    }

    @Test
    void shouldNotFailWhenALegacyShotCountValueIsDangerousAndAnotherOneIsRegistered() throws Exception {
        UUID acertos = configured("Acertos");
        UUID erros = configured("Erros");
        insertLegacyValue(seriesId, acertos, "1E+99999999");

        assertTimeout(LIMIT, () -> register(seriesId, erros, "3").andExpect(status().isOk()));
    }
}
