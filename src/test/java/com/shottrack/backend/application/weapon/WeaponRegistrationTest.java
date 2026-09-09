package com.shottrack.backend.application.weapon;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shottrack.backend.application.auth.dto.LoginRequest;
import com.shottrack.backend.application.user.dto.UserRegisterRequest;
import com.shottrack.backend.application.weapon.dto.WeaponRegisterRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class WeaponRegistrationTest {

    private static final String EMAIL = "atleta.cadastraarma@shottrack.com";
    private static final String PASSWORD = "senha12345";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String accessToken;
    private UUID pistolaId;
    private UUID glockId;
    private UUID g17Id;
    private UUID caliber9mmId;

    @BeforeEach
    void registerAndLoginUser() throws Exception {
        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new UserRegisterRequest("Atleta Cadastra Arma", EMAIL, PASSWORD))));

        var result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(EMAIL, PASSWORD))))
                .andReturn();

        JsonNode data = objectMapper.readTree(result.getResponse().getContentAsString()).get("data");
        accessToken = data.get("accessToken").asText();

        pistolaId = idByName("/api/weapon-catalog/types", "Pistola");
        glockId = idByName("/api/weapon-catalog/brands", "Glock");
        g17Id = idByName("/api/weapon-catalog/brands/" + glockId + "/models", "G17");
        caliber9mmId = idByName("/api/weapon-catalog/calibers", "9mm");
    }

    @Test
    void shouldRegisterWeaponWithValidData() throws Exception {
        var request = new WeaponRegisterRequest(pistolaId, glockId, g17Id, caliber9mmId, null);

        mockMvc.perform(post("/api/weapons")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.type.name").value("Pistola"))
                .andExpect(jsonPath("$.data.brand.name").value("Glock"))
                .andExpect(jsonPath("$.data.model.name").value("G17"))
                .andExpect(jsonPath("$.data.caliber.name").value("9mm"));
    }

    @Test
    void shouldRegisterWeaponWithOptionalNickname() throws Exception {
        var request = new WeaponRegisterRequest(pistolaId, glockId, g17Id, caliber9mmId, "Minha 9mm de competição");

        mockMvc.perform(post("/api/weapons")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.nickname").value("Minha 9mm de competição"));
    }

    @Test
    void shouldRejectMissingRequiredField() throws Exception {
        var request = new WeaponRegisterRequest(null, glockId, g17Id, caliber9mmId, null);

        mockMvc.perform(post("/api/weapons")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    void shouldRejectRequestWithoutToken() throws Exception {
        var request = new WeaponRegisterRequest(pistolaId, glockId, g17Id, caliber9mmId, null);

        mockMvc.perform(post("/api/weapons")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
    }

    @Test
    void shouldRejectNonExistentType() throws Exception {
        var request = new WeaponRegisterRequest(UUID.randomUUID(), glockId, g17Id, caliber9mmId, null);

        mockMvc.perform(post("/api/weapons")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("WEAPON_TYPE_NOT_FOUND"));
    }

    @Test
    void shouldRejectNonExistentBrand() throws Exception {
        var request = new WeaponRegisterRequest(pistolaId, UUID.randomUUID(), g17Id, caliber9mmId, null);

        mockMvc.perform(post("/api/weapons")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("WEAPON_BRAND_NOT_FOUND"));
    }

    @Test
    void shouldRejectNonExistentModel() throws Exception {
        var request = new WeaponRegisterRequest(pistolaId, glockId, UUID.randomUUID(), caliber9mmId, null);

        mockMvc.perform(post("/api/weapons")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("WEAPON_MODEL_NOT_FOUND"));
    }

    @Test
    void shouldRejectNonExistentCaliber() throws Exception {
        var request = new WeaponRegisterRequest(pistolaId, glockId, g17Id, UUID.randomUUID(), null);

        mockMvc.perform(post("/api/weapons")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("WEAPON_CALIBER_NOT_FOUND"));
    }

    @Test
    void shouldRejectModelThatDoesNotBelongToGivenBrand() throws Exception {
        UUID taurusId = idByName("/api/weapon-catalog/brands", "Taurus");
        UUID modelo856Id = idByName("/api/weapon-catalog/brands/" + taurusId + "/models", "856");

        var request = new WeaponRegisterRequest(pistolaId, glockId, modelo856Id, caliber9mmId, null);

        mockMvc.perform(post("/api/weapons")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("WEAPON_MODEL_BRAND_MISMATCH"));
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
