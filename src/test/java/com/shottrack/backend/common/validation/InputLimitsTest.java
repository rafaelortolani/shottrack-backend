package com.shottrack.backend.common.validation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shottrack.backend.application.user.gateway.repository.PendingRegistrationRepository;
import com.shottrack.backend.support.DashboardTestSupport;
import com.shottrack.backend.support.SeriesTestSupport;
import com.shottrack.backend.support.TestUsers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * S10/S18: cada texto de entrada tem um limite (consistente com a coluna) — no limite aceita, um acima recusa
 * com VALIDATION_ERROR — e as senhas novas ficam entre 8 caracteres e 72 bytes UTF-8.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class InputLimitsTest {

    private static final String PASSWORD = "senha12345";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PendingRegistrationRepository pendingRegistrationRepository;

    @MockBean
    private JavaMailSenderImpl mailSender;

    private String token;

    @BeforeEach
    void registerAthlete() throws Exception {
        token = TestUsers.registerAndLogin(mockMvc, objectMapper, pendingRegistrationRepository,
                "Atleta", "atleta.limites@shottrack.com", PASSWORD);
    }

    private static String str(int size) {
        return "a".repeat(size);
    }

    private ResultActions send(HttpMethod method, String path, String athleteToken, Map<String, Object> body) throws Exception {
        var builder = request(method, path).contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body));
        if (athleteToken != null) {
            builder.header("Authorization", "Bearer " + athleteToken);
        }
        return mockMvc.perform(builder);
    }

    private static Map<String, Object> map(Object... pairs) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            map.put((String) pairs[i], pairs[i + 1]);
        }
        return map;
    }

    private void assertRejected(ResultActions result) throws Exception {
        result.andExpect(status().isBadRequest()).andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    private UUID idFrom(ResultActions result) throws Exception {
        return UUID.fromString(objectMapper.readTree(result.andReturn().getResponse().getContentAsString())
                .get("data").get("id").asText());
    }

    // ---- usuário e autenticação ----

    @Test
    void shouldLimitTheProfileName() throws Exception {
        send(HttpMethod.PATCH, "/api/users/me", token, map("name", str(100), "experienceLevel", "BEGINNER")).andExpect(status().isOk());
        assertRejected(send(HttpMethod.PATCH, "/api/users/me", token, map("name", str(101), "experienceLevel", "BEGINNER")));
    }

    private static String email(int totalLength) {
        // 64 (parte local) + @ + 63.63.N.com = 197 + N
        return str(64) + "@" + str(63) + "." + str(63) + "." + str(totalLength - 197) + ".com";
    }

    @Test
    void shouldLimitTheEmailLengthAt254() throws Exception {
        send(HttpMethod.POST, "/api/users/registration", null, map("email", email(254))).andExpect(status().isAccepted());
        assertRejected(send(HttpMethod.POST, "/api/users/registration", null, map("email", email(255))));
        assertRejected(send(HttpMethod.POST, "/api/auth/login", null, map("email", email(255), "password", "x")));
        send(HttpMethod.POST, "/api/auth/login", null, map("email", email(254), "password", "x")).andExpect(status().isUnauthorized());
        assertRejected(send(HttpMethod.POST, "/api/users/me/email", token, map("email", email(255), "currentPassword", PASSWORD)));
        assertRejected(send(HttpMethod.POST, "/api/auth/password-reset/request", null, map("email", email(255))));
    }

    private String pendingToken(String email) throws Exception {
        send(HttpMethod.POST, "/api/users/registration", null, map("email", email));
        return pendingRegistrationRepository.findAllByEmailAndUsedFalse(email).get(0).getToken();
    }

    private ResultActions complete(String token, String name, String password) throws Exception {
        return send(HttpMethod.POST, "/api/users/registration/completion", null,
                map("token", token, "name", name, "password", password, "acceptedTerms", true));
    }

    @Test
    void shouldLimitNameAndPasswordOnRegistrationCompletion() throws Exception {
        assertRejected(complete(pendingToken("limite.nome.a@shottrack.com"), str(101), PASSWORD));
        assertRejected(complete(pendingToken("limite.nome.b@shottrack.com"), "Atleta", str(7)));
        assertRejected(complete(pendingToken("limite.nome.c@shottrack.com"), "Atleta", str(73)));
        assertRejected(complete("t".repeat(256), "Atleta", PASSWORD));

        complete(pendingToken("limite.nome.d@shottrack.com"), str(100), str(72)).andExpect(status().isCreated());
        complete(pendingToken("limite.nome.e@shottrack.com"), "Atleta", str(8)).andExpect(status().isCreated());
    }

    @Test
    void shouldExplainThePasswordCeilingInPortugueseAndEnglish() throws Exception {
        String registrationToken = pendingToken("limite.senha.msg@shottrack.com");

        send(HttpMethod.POST, "/api/users/registration/completion", null,
                map("token", registrationToken, "name", "Atleta", "password", str(73), "acceptedTerms", true))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.message").value("password: a senha pode ter no máximo 72 bytes; acentos contam mais de um"));

        mockMvc.perform(request(HttpMethod.POST, "/api/users/registration/completion")
                        .header("Accept-Language", "en")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                map("token", registrationToken, "name", "Atleta", "password", str(73), "acceptedTerms", true))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.message").value("password: the password can have at most 72 bytes; accented characters count as more than one"));
    }

    @Test
    void shouldMeasureThePasswordCeilingInUtf8BytesOnRegistrationCompletion() throws Exception {
        // N2: o limite do BCrypt é em BYTES; 37 "ç" são 37 caracteres mas 74 bytes (antes: 500)
        assertRejected(complete(pendingToken("limite.bytes.a@shottrack.com"), "Atleta", "ç".repeat(37)));
        assertRejected(complete(pendingToken("limite.bytes.b@shottrack.com"), "Atleta", "ç".repeat(80)));
        assertRejected(complete(pendingToken("limite.bytes.c@shottrack.com"), "Atleta", "\uD83D\uDD2B".repeat(19))); // 76 bytes

        complete(pendingToken("limite.bytes.d@shottrack.com"), "Atleta", "a".repeat(72)).andExpect(status().isCreated());
        // exatamente 72 bytes: cadastra e depois entra com a mesma senha
        String accented36 = "ç".repeat(36);
        complete(pendingToken("limite.bytes.e@shottrack.com"), "Atleta", accented36).andExpect(status().isCreated());
        send(HttpMethod.POST, "/api/auth/login", null, map("email", "limite.bytes.e@shottrack.com", "password", accented36))
                .andExpect(status().isOk());
    }

    @Test
    void shouldMeasureThePasswordCeilingInUtf8BytesOnPasswordChange() throws Exception {
        assertRejected(send(HttpMethod.PATCH, "/api/users/me/password", token, map("currentPassword", PASSWORD, "newPassword", "ç".repeat(37))));
        assertRejected(send(HttpMethod.PATCH, "/api/users/me/password", token, map("currentPassword", PASSWORD, "newPassword", "ç".repeat(80))));

        send(HttpMethod.PATCH, "/api/users/me/password", token, map("currentPassword", PASSWORD, "newPassword", "a".repeat(72))).andExpect(status().isOk());
        send(HttpMethod.PATCH, "/api/users/me/password", token, map("currentPassword", "a".repeat(72), "newPassword", "ç".repeat(36))).andExpect(status().isOk());
    }

    @Test
    void shouldMeasureThePasswordCeilingInUtf8BytesOnPasswordResetConfirmation() throws Exception {
        assertRejected(send(HttpMethod.POST, "/api/auth/password-reset/confirm", null, map("token", "inexistente", "newPassword", "ç".repeat(37))));
        assertRejected(send(HttpMethod.POST, "/api/auth/password-reset/confirm", null, map("token", "inexistente", "newPassword", "ç".repeat(80))));

        // dentro do limite a senha passa na validação e cai no token inexistente (não em VALIDATION_ERROR)
        for (String ok : new String[]{"a".repeat(72), "ç".repeat(36)}) {
            send(HttpMethod.POST, "/api/auth/password-reset/confirm", null, map("token", "inexistente", "newPassword", ok))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error.code").value("PASSWORD_RESET_TOKEN_INVALID"));
        }
    }

    @Test
    void shouldLimitTheNewPasswordOnChangeAndReset() throws Exception {
        assertRejected(send(HttpMethod.PATCH, "/api/users/me/password", token, map("currentPassword", PASSWORD, "newPassword", str(73))));
        assertRejected(send(HttpMethod.PATCH, "/api/users/me/password", token, map("currentPassword", PASSWORD, "newPassword", str(7))));
        send(HttpMethod.PATCH, "/api/users/me/password", token, map("currentPassword", PASSWORD, "newPassword", str(72))).andExpect(status().isOk());

        assertRejected(send(HttpMethod.POST, "/api/auth/password-reset/confirm", null, map("token", "inexistente", "newPassword", str(73))));
        assertRejected(send(HttpMethod.POST, "/api/auth/password-reset/confirm", null, map("token", "t".repeat(256), "newPassword", str(72))));
        // no limite a senha passa na validação (e cai no token inexistente, não em VALIDATION_ERROR)
        send(HttpMethod.POST, "/api/auth/password-reset/confirm", null, map("token", "inexistente", "newPassword", str(72)))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error.code").value("PASSWORD_RESET_TOKEN_INVALID"));
    }

    @Test
    void shouldLimitRefreshTokenAndVerificationCodeLengths() throws Exception {
        assertRejected(send(HttpMethod.POST, "/api/auth/refresh", null, map("refreshToken", "t".repeat(256))));
        assertRejected(send(HttpMethod.POST, "/api/auth/logout", null, map("refreshToken", "t".repeat(256))));
        assertRejected(send(HttpMethod.POST, "/api/users/me/email/confirmation", token, map("code", "1".repeat(21))));
        send(HttpMethod.POST, "/api/auth/refresh", null, map("refreshToken", "t".repeat(255))).andExpect(status().isUnauthorized());
    }

    // ---- armas, munições, acessórios ----

    @Test
    void shouldLimitTheWeaponNickname() throws Exception {
        UUID weaponId = SeriesTestSupport.registerWeapon(mockMvc, objectMapper, token);
        var model = weaponModel();
        for (int size : new int[]{100}) {
            send(HttpMethod.PATCH, "/api/weapons/" + weaponId, token,
                    map("modelId", model[0], "caliberId", model[1], "nickname", str(size))).andExpect(status().isOk());
        }
        assertRejected(send(HttpMethod.PATCH, "/api/weapons/" + weaponId, token,
                map("modelId", model[0], "caliberId", model[1], "nickname", str(101))));
    }

    private String[] weaponModel() throws Exception {
        JsonNode weapons = objectMapper.readTree(mockMvc.perform(get("/api/weapons").header("Authorization", "Bearer " + token))
                .andReturn().getResponse().getContentAsString()).get("data");
        JsonNode weapon = weapons.get(0);
        return new String[]{weapon.get("model").get("id").asText(), weapon.get("caliber").get("id").asText()};
    }

    @Test
    void shouldLimitAmmunitionTextsAndNumbers() throws Exception {
        for (Map<String, Object> tooLong : new Map[]{map("nickname", str(101)), map("projectileType", str(51)),
                map("lot", str(51)), map("notes", str(2001))}) {
            assertRejected(send(HttpMethod.POST, "/api/ammunitions", token, tooLong));
        }
        send(HttpMethod.POST, "/api/ammunitions", token,
                map("nickname", str(100), "projectileType", str(50), "lot", str(50), "notes", str(2000)))
                .andExpect(status().isCreated());
        send(HttpMethod.POST, "/api/ammunitions", token, map("nickname", "ok", "projectileWeightGrains", 9999.99, "powderCharge", 0))
                .andExpect(status().isCreated());

        for (Map<String, Object> invalid : new Map[]{map("nickname", "x", "projectileWeightGrains", 10000),
                map("nickname", "x", "projectileWeightGrains", 1.234), map("nickname", "x", "powderCharge", -1),
                map("nickname", "x", "projectileWeightGrains", new java.math.BigDecimal("1e999999999"))}) {
            assertRejected(send(HttpMethod.POST, "/api/ammunitions", token, invalid));
        }
        UUID id = idFrom(send(HttpMethod.POST, "/api/ammunitions", token, map("nickname", "para editar")));
        assertRejected(send(HttpMethod.PATCH, "/api/ammunitions/" + id, token, map("lot", str(51))));
        send(HttpMethod.PATCH, "/api/ammunitions/" + id, token, map("lot", str(50))).andExpect(status().isOk());
    }

    private UUID accessoryType() throws Exception {
        JsonNode types = objectMapper.readTree(mockMvc.perform(get("/api/accessory-catalog/types")
                .header("Authorization", "Bearer " + token)).andReturn().getResponse().getContentAsString()).get("data");
        return UUID.fromString(types.get(0).get("id").asText());
    }

    @Test
    void shouldLimitAccessoryNameAndNotes() throws Exception {
        UUID type = accessoryType();
        assertRejected(send(HttpMethod.POST, "/api/accessories", token, map("name", str(101), "typeId", type)));
        assertRejected(send(HttpMethod.POST, "/api/accessories", token, map("name", "Luneta", "typeId", type, "notes", str(2001))));
        UUID id = idFrom(send(HttpMethod.POST, "/api/accessories", token,
                map("name", str(100), "typeId", type, "notes", str(2000))).andExpect(status().isCreated()));

        assertRejected(send(HttpMethod.PATCH, "/api/accessories/" + id, token, map("name", str(101))));
        assertRejected(send(HttpMethod.PATCH, "/api/accessories/" + id, token, map("notes", str(2001))));
        send(HttpMethod.PATCH, "/api/accessories/" + id, token, map("name", str(100), "notes", str(2000))).andExpect(status().isOk());
    }

    // ---- locais, visitas, séries ----

    @Test
    void shouldLimitTrainingLocationFields() throws Exception {
        assertRejected(send(HttpMethod.POST, "/api/training-locations", token, map("name", str(151), "city", "C", "state", "SP")));
        assertRejected(send(HttpMethod.POST, "/api/training-locations", token, map("name", "N", "city", str(101), "state", "SP")));
        assertRejected(send(HttpMethod.POST, "/api/training-locations", token, map("name", "N", "city", "C", "state", str(51))));
        UUID id = idFrom(send(HttpMethod.POST, "/api/training-locations", token,
                map("name", str(150), "city", str(100), "state", str(50))).andExpect(status().isCreated()));

        assertRejected(send(HttpMethod.PATCH, "/api/training-locations/" + id, token, map("name", str(151))));
        assertRejected(send(HttpMethod.PATCH, "/api/training-locations/" + id, token, map("city", str(101))));
        assertRejected(send(HttpMethod.PATCH, "/api/training-locations/" + id, token, map("state", str(51))));
        send(HttpMethod.PATCH, "/api/training-locations/" + id, token, map("name", str(150))).andExpect(status().isOk());
    }

    @Test
    void shouldLimitVisitObservations() throws Exception {
        UUID location = idFrom(send(HttpMethod.POST, "/api/training-locations", token, map("name", "Clube", "city", "C", "state", "SP")));

        assertRejected(send(HttpMethod.POST, "/api/visits", token, map("trainingLocationId", location, "observations", str(2001))));
        send(HttpMethod.POST, "/api/visits", token, map("trainingLocationId", location, "observations", str(2000)))
                .andExpect(status().isCreated());
    }

    @Test
    void shouldLimitSeriesTextsAndDistance() throws Exception {
        UUID trainingId = SeriesTestSupport.openTraining(mockMvc, objectMapper, token, "IPSC");

        assertRejected(send(HttpMethod.POST, "/api/series", token, map("trainingId", trainingId, "target", str(101))));
        assertRejected(send(HttpMethod.POST, "/api/series", token, map("trainingId", trainingId, "notes", str(2001))));
        UUID seriesId = idFrom(send(HttpMethod.POST, "/api/series", token,
                map("trainingId", trainingId, "target", str(100), "notes", str(2000), "distanceMeters", 25.5)).andExpect(status().isCreated()));

        for (Object distance : new Object[]{-1, 100000.01, 1234567, 1.234, new java.math.BigDecimal("1e999999999"),
                new java.math.BigDecimal("-1E+99999999")}) {
            assertRejected(send(HttpMethod.POST, "/api/series", token, map("trainingId", trainingId, "distanceMeters", distance)));
            assertRejected(send(HttpMethod.PATCH, "/api/series/" + seriesId, token, map("distanceMeters", distance)));
        }
        for (Object distance : new Object[]{0, 100000, 25, 99999.99}) {
            send(HttpMethod.POST, "/api/series", token, map("trainingId", trainingId, "distanceMeters", distance))
                    .andExpect(status().isCreated());
        }
        assertRejected(send(HttpMethod.PATCH, "/api/series/" + seriesId, token, map("target", str(101))));
        send(HttpMethod.PATCH, "/api/series/" + seriesId, token, map("target", str(100), "notes", str(2000))).andExpect(status().isOk());
    }

    @Test
    void shouldLimitTheFreeTextResultValue() throws Exception {
        UUID trainingId = SeriesTestSupport.openTraining(mockMvc, objectMapper, token, "IPSC");
        UUID ipsc = DashboardTestSupport.modalityId(mockMvc, objectMapper, token, "IPSC");
        UUID note = DashboardTestSupport.resultTypeId(mockMvc, objectMapper, token, "Anotação livre");
        DashboardTestSupport.configureResultType(mockMvc, objectMapper, token, ipsc, note);
        UUID seriesId = DashboardTestSupport.registerSeries(mockMvc, objectMapper, token, trainingId, null);

        assertRejected(send(HttpMethod.POST, "/api/series/" + seriesId + "/results", token, map("resultTypeId", note, "value", str(501))));
        send(HttpMethod.POST, "/api/series/" + seriesId + "/results", token, map("resultTypeId", note, "value", str(500)))
                .andExpect(status().isOk());
    }

    @Test
    void shouldLimitTheTrackingLevelName() throws Exception {
        assertRejected(send(HttpMethod.PUT, "/api/habituality/tracking", token, map("ruleId", UUID.randomUUID(), "levelName", str(101))));
        // no limite passa na validação e cai na regra inexistente
        send(HttpMethod.PUT, "/api/habituality/tracking", token, map("ruleId", UUID.randomUUID(), "levelName", str(100)))
                .andExpect(status().isNotFound());
    }
}
