package com.shottrack.backend.application.ammunition;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shottrack.backend.application.ammunition.dto.AmmunitionRegisterRequest;
import com.shottrack.backend.application.ammunition.dto.AmmunitionUpdateRequest;
import com.shottrack.backend.application.ammunition.gateway.repository.AmmunitionManufacturerRepository;
import com.shottrack.backend.application.auth.dto.LoginRequest;
import com.shottrack.backend.application.user.dto.UserRegisterRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AmmunitionUpdateTest {

    private static final String EMAIL = "atleta.editamunicao@shottrack.com";
    private static final String PASSWORD = "senha12345";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AmmunitionManufacturerRepository ammunitionManufacturerRepository;

    private String accessToken;
    private UUID cbcId;
    private UUID magtechId;
    private UUID caliber9mmId;
    private UUID caliber40Id;
    private UUID ammunitionId;

    @BeforeEach
    void prepareExistingAmmunition() throws Exception {
        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new UserRegisterRequest("Atleta Edita Munição", EMAIL, PASSWORD))));

        var loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(EMAIL, PASSWORD))))
                .andReturn();
        accessToken = objectMapper.readTree(loginResult.getResponse().getContentAsString())
                .get("data").get("accessToken").asText();

        cbcId = manufacturerIdByName("CBC");
        magtechId = manufacturerIdByName("Magtech");
        caliber9mmId = idByName("/api/weapon-catalog/calibers", "9mm");
        caliber40Id = idByName("/api/weapon-catalog/calibers", ".40 S&W");

        var registerRequest = new AmmunitionRegisterRequest(cbcId, caliber9mmId, "Original", null, null, null, null, null);
        var registerResult = mockMvc.perform(post("/api/ammunitions")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andReturn();
        ammunitionId = UUID.fromString(objectMapper.readTree(registerResult.getResponse().getContentAsString())
                .get("data").get("id").asText());
    }

    @Test
    void shouldEditOnlyNicknameKeepingTheRest() throws Exception {
        var request = new AmmunitionUpdateRequest(null, null, "Apelido novo", null, null, null, null, null);

        mockMvc.perform(patch("/api/ammunitions/" + ammunitionId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.nickname").value("Apelido novo"))
                .andExpect(jsonPath("$.data.manufacturer.name").value("CBC"))
                .andExpect(jsonPath("$.data.caliber.name").value("9mm"));
    }

    @Test
    void shouldEditMultipleFieldsAtOnce() throws Exception {
        var request = new AmmunitionUpdateRequest(magtechId, caliber40Id, "Apelido novo",
                new BigDecimal("180.00"), new BigDecimal("5.10"), "JHP", "L2025B", "Recarga nova");

        mockMvc.perform(patch("/api/ammunitions/" + ammunitionId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.manufacturer.name").value("Magtech"))
                .andExpect(jsonPath("$.data.caliber.name").value(".40 S&W"))
                .andExpect(jsonPath("$.data.nickname").value("Apelido novo"))
                .andExpect(jsonPath("$.data.projectileWeightGrains").value(180.00))
                .andExpect(jsonPath("$.data.powderCharge").value(5.10))
                .andExpect(jsonPath("$.data.projectileType").value("JHP"))
                .andExpect(jsonPath("$.data.lot").value("L2025B"))
                .andExpect(jsonPath("$.data.notes").value("Recarga nova"));
    }

    @Test
    void shouldRejectRequestWithoutToken() throws Exception {
        var request = new AmmunitionUpdateRequest(null, null, "Apelido novo", null, null, null, null, null);

        mockMvc.perform(patch("/api/ammunitions/" + ammunitionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
    }

    @Test
    void shouldRejectNonExistentAmmunition() throws Exception {
        var request = new AmmunitionUpdateRequest(null, null, "Apelido novo", null, null, null, null, null);

        mockMvc.perform(patch("/api/ammunitions/" + UUID.randomUUID())
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("AMMUNITION_NOT_FOUND"));
    }

    @Test
    void shouldRejectEditingAnotherAthletesAmmunition() throws Exception {
        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new UserRegisterRequest("Outro Atleta", "atleta.outromunicao@shottrack.com", PASSWORD))));
        var otherLoginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("atleta.outromunicao@shottrack.com", PASSWORD))))
                .andReturn();
        String otherToken = objectMapper.readTree(otherLoginResult.getResponse().getContentAsString())
                .get("data").get("accessToken").asText();

        var request = new AmmunitionUpdateRequest(null, null, "Apelido novo", null, null, null, null, null);

        mockMvc.perform(patch("/api/ammunitions/" + ammunitionId)
                        .header("Authorization", "Bearer " + otherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("AMMUNITION_NOT_FOUND"));
    }

    @Test
    void shouldRejectEditResultingInNoManufacturerAndNoNickname() throws Exception {
        var nicknameOnlyRequest = new AmmunitionRegisterRequest(null, null, "Só apelido", null, null, null, null, null);
        var registerResult = mockMvc.perform(post("/api/ammunitions")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(nicknameOnlyRequest)))
                .andReturn();
        UUID nicknameOnlyId = UUID.fromString(objectMapper.readTree(registerResult.getResponse().getContentAsString())
                .get("data").get("id").asText());

        var request = new AmmunitionUpdateRequest(null, null, "   ", null, null, null, null, null);

        mockMvc.perform(patch("/api/ammunitions/" + nicknameOnlyId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("AMMUNITION_IDENTIFICATION_REQUIRED"));
    }

    @Test
    void shouldRejectNonExistentManufacturer() throws Exception {
        var request = new AmmunitionUpdateRequest(UUID.randomUUID(), null, null, null, null, null, null, null);

        mockMvc.perform(patch("/api/ammunitions/" + ammunitionId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("AMMUNITION_MANUFACTURER_NOT_FOUND"));
    }

    @Test
    void shouldRejectNonExistentCaliber() throws Exception {
        var request = new AmmunitionUpdateRequest(null, UUID.randomUUID(), null, null, null, null, null, null);

        mockMvc.perform(patch("/api/ammunitions/" + ammunitionId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("WEAPON_CALIBER_NOT_FOUND"));
    }

    private UUID manufacturerIdByName(String name) {
        return ammunitionManufacturerRepository.findAll().stream()
                .filter(m -> m.getName().equals(name))
                .findFirst()
                .orElseThrow()
                .getId();
    }

    private UUID idByName(String path, String name) throws Exception {
        var result = mockMvc.perform(get(path)
                        .header("Authorization", "Bearer " + accessToken))
                .andReturn();

        JsonNode items = objectMapper.readTree(result.getResponse().getContentAsString()).get("data");
        for (JsonNode item : items) {
            if (item.get("name").asText().equals(name)) {
                return UUID.fromString(item.get("id").asText());
            }
        }
        throw new AssertionError("Item não encontrado no catálogo (" + path + "): " + name);
    }
}
