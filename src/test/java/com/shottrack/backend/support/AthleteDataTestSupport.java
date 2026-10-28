package com.shottrack.backend.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shottrack.backend.application.accessory.dto.AccessoryRegisterRequest;
import com.shottrack.backend.application.accessory.dto.AssociateAccessoryWeaponRequest;
import com.shottrack.backend.application.auth.dto.LoginRequest;
import com.shottrack.backend.application.auth.dto.PasswordResetRequest;
import com.shottrack.backend.application.series.dto.MarkSeriesResultNotApplicableRequest;
import com.shottrack.backend.application.series.dto.RegisterSeriesRequest;
import com.shottrack.backend.application.series.dto.RegisterSeriesResultRequest;
import com.shottrack.backend.application.user.dto.ChangeEmailRequest;
import com.shottrack.backend.application.user.gateway.repository.PendingRegistrationRepository;
import com.shottrack.backend.application.user.gateway.repository.UserRepository;
import com.shottrack.backend.application.user.model.PendingRegistration;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * ADR-0019 (UC52/UC53): monta um atleta com dados em TODAS as tabelas que
 * pertencem a ele, e conta as linhas de cada tabela — o teste de exclusão usa
 * as contagens pra provar que nenhuma tabela ficou de fora.
 */
public final class AthleteDataTestSupport {

    public static final String PASSWORD = "senha12345";

    private AthleteDataTestSupport() {
    }

    public record Athlete(String email, UUID userId, String accessToken, String refreshToken) {
    }

    public static Athlete registerAthlete(MockMvc mockMvc, ObjectMapper objectMapper,
                                          PendingRegistrationRepository pendingRegistrationRepository,
                                          UserRepository userRepository, String email) throws Exception {
        TestUsers.register(mockMvc, objectMapper, pendingRegistrationRepository, "Atleta Teste", email, PASSWORD);

        var result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(email, PASSWORD))))
                .andReturn();
        JsonNode data = objectMapper.readTree(result.getResponse().getContentAsString()).get("data");

        UUID userId = userRepository.findByEmail(email).orElseThrow().getId();
        return new Athlete(email, userId, data.get("accessToken").asText(), data.get("refreshToken").asText());
    }

    /**
     * Cria dados em todas as tabelas do atleta, exceto dashboard_summary* (o
     * resumo é gravado pelo consumidor da fila — o teste chama o recálculo
     * direto) e cadastro pendente (inserido direto, pois o e-mail já existe).
     */
    public static void populate(MockMvc mockMvc, ObjectMapper objectMapper,
                                PendingRegistrationRepository pendingRegistrationRepository, Athlete athlete) throws Exception {
        String token = athlete.accessToken();

        UUID weaponId = SeriesTestSupport.registerWeapon(mockMvc, objectMapper, token, "Minha G17");
        UUID ammunitionId = SeriesTestSupport.registerAmmunition(mockMvc, objectMapper, token);

        UUID accessoryTypeId = idByName(mockMvc, objectMapper, token, "/api/accessory-catalog/types", "Luneta");
        var accessoryResult = mockMvc.perform(post("/api/accessories")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AccessoryRegisterRequest("Luneta 4x32", accessoryTypeId, "comprada usada"))))
                .andReturn();
        UUID accessoryId = idFrom(objectMapper, accessoryResult.getResponse().getContentAsString());
        mockMvc.perform(post("/api/accessories/" + accessoryId + "/weapons")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new AssociateAccessoryWeaponRequest(weaponId))));

        UUID trainingId = SeriesTestSupport.openTraining(mockMvc, objectMapper, token, "IPSC");
        var seriesResult = mockMvc.perform(post("/api/series")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RegisterSeriesRequest(trainingId, weaponId, ammunitionId,
                                new BigDecimal("25"), "Alvo IPSC", 10, "primeira série"))))
                .andReturn();
        UUID seriesId = idFrom(objectMapper, seriesResult.getResponse().getContentAsString());

        UUID tempoId = idByName(mockMvc, objectMapper, token, "/api/result-type-catalog", "Tempo");
        UUID pontuacaoId = idByName(mockMvc, objectMapper, token, "/api/result-type-catalog", "Pontuação");
        UUID fatorId = idByName(mockMvc, objectMapper, token, "/api/result-type-catalog", "Fator de desempenho");
        postJson(mockMvc, objectMapper, token, "/api/series/" + seriesId + "/results", new RegisterSeriesResultRequest(tempoId, "9.8"));
        postJson(mockMvc, objectMapper, token, "/api/series/" + seriesId + "/results", new RegisterSeriesResultRequest(pontuacaoId, "0"));
        postJson(mockMvc, objectMapper, token, "/api/series/" + seriesId + "/results/not-applicable",
                new MarkSeriesResultNotApplicableRequest(fatorId));

        // tokens: refresh token já existe (login); código de e-mail e token de redefinição:
        postJson(mockMvc, objectMapper, token, "/api/users/me/email", new ChangeEmailRequest("novo." + athlete.email()));
        mockMvc.perform(post("/api/auth/password-reset/request")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new PasswordResetRequest(athlete.email()))));

        pendingRegistrationRepository.save(PendingRegistration.builder()
                .email(athlete.email())
                .token(UUID.randomUUID().toString())
                .expiresAt(Instant.now().plus(Duration.ofDays(1)))
                .build());
    }

    /** Linhas que pertencem ao atleta em cada tabela (a ordem é a da exclusão). */
    public static Map<String, Long> countRows(JdbcTemplate jdbc, Athlete athlete) {
        UUID id = athlete.userId();
        String visits = "(SELECT id FROM visits WHERE user_id = ?)";
        String trainings = "(SELECT id FROM trainings WHERE visit_id IN " + visits + ")";
        String series = "(SELECT id FROM series WHERE training_id IN " + trainings + ")";
        String summaries = "(SELECT id FROM dashboard_summary WHERE user_id = ?)";

        Map<String, Long> counts = new LinkedHashMap<>();
        counts.put("dashboard_summary", count(jdbc, "SELECT count(*) FROM dashboard_summary WHERE user_id = ?", id));
        counts.put("dashboard_summary_modalities", count(jdbc, "SELECT count(*) FROM dashboard_summary_modalities WHERE dashboard_summary_id IN " + summaries, id));
        counts.put("dashboard_summary_modality_stats", count(jdbc, "SELECT count(*) FROM dashboard_summary_modality_stats WHERE dashboard_summary_id IN " + summaries, id));
        counts.put("dashboard_summary_records", count(jdbc, "SELECT count(*) FROM dashboard_summary_records WHERE dashboard_summary_id IN " + summaries, id));
        counts.put("series_results", count(jdbc, "SELECT count(*) FROM series_results WHERE series_id IN " + series, id));
        counts.put("series", count(jdbc, "SELECT count(*) FROM series WHERE training_id IN " + trainings, id));
        counts.put("trainings", count(jdbc, "SELECT count(*) FROM trainings WHERE visit_id IN " + visits, id));
        counts.put("visits", count(jdbc, "SELECT count(*) FROM visits WHERE user_id = ?", id));
        counts.put("training_locations", count(jdbc, "SELECT count(*) FROM training_locations WHERE user_id = ?", id));
        counts.put("accessory_weapons", count(jdbc, "SELECT count(*) FROM accessory_weapons WHERE accessory_id IN (SELECT id FROM accessories WHERE user_id = ?)", id));
        counts.put("accessories", count(jdbc, "SELECT count(*) FROM accessories WHERE user_id = ?", id));
        counts.put("ammunitions", count(jdbc, "SELECT count(*) FROM ammunitions WHERE user_id = ?", id));
        counts.put("weapons", count(jdbc, "SELECT count(*) FROM weapons WHERE user_id = ?", id));
        counts.put("modality_result_type_selections", count(jdbc, "SELECT count(*) FROM modality_result_type_selections WHERE user_id = ?", id));
        counts.put("practiced_modalities", count(jdbc, "SELECT count(*) FROM practiced_modalities WHERE user_id = ?", id));
        counts.put("refresh_tokens", count(jdbc, "SELECT count(*) FROM refresh_tokens WHERE user_id = ?", id));
        counts.put("email_verification_codes", count(jdbc, "SELECT count(*) FROM email_verification_codes WHERE user_id = ?", id));
        counts.put("password_reset_tokens", count(jdbc, "SELECT count(*) FROM password_reset_tokens WHERE user_id = ?", id));
        counts.put("pending_registrations", count(jdbc, "SELECT count(*) FROM pending_registrations WHERE email = ?", athlete.email()));
        counts.put("users", count(jdbc, "SELECT count(*) FROM users WHERE id = ?", id));
        return counts;
    }

    private static long count(JdbcTemplate jdbc, String sql, Object param) {
        // o nº de "?" varia: as subconsultas encadeadas repetem o mesmo parâmetro
        int placeholders = (int) sql.chars().filter(c -> c == '?').count();
        Object[] params = new Object[placeholders];
        java.util.Arrays.fill(params, param);
        Long value = jdbc.queryForObject(sql, Long.class, params);
        return value == null ? 0 : value;
    }

    private static void postJson(MockMvc mockMvc, ObjectMapper objectMapper, String token, String path, Object body) throws Exception {
        mockMvc.perform(post(path)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)));
    }

    private static UUID idFrom(ObjectMapper objectMapper, String json) throws Exception {
        return UUID.fromString(objectMapper.readTree(json).get("data").get("id").asText());
    }

    private static UUID idByName(MockMvc mockMvc, ObjectMapper objectMapper, String token, String path, String name) throws Exception {
        var result = mockMvc.perform(get(path).header("Authorization", "Bearer " + token)).andReturn();
        for (JsonNode item : objectMapper.readTree(result.getResponse().getContentAsString()).get("data")) {
            if (item.get("name").asText().equals(name)) {
                return UUID.fromString(item.get("id").asText());
            }
        }
        throw new AssertionError("Item não encontrado no catálogo (" + path + "): " + name);
    }
}
