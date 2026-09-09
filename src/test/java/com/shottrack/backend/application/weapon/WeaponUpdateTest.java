package com.shottrack.backend.application.weapon;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shottrack.backend.application.auth.dto.LoginRequest;
import com.shottrack.backend.application.user.dto.UserRegisterRequest;
import com.shottrack.backend.application.weapon.dto.WeaponRegisterRequest;
import com.shottrack.backend.application.weapon.dto.WeaponUpdateRequest;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class WeaponUpdateTest {

    private static final String EMAIL = "atleta.editaarma@shottrack.com";
    private static final String PASSWORD = "senha12345";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String accessToken;
    private UUID pistolaId;
    private UUID revolverId;
    private UUID glockId;
    private UUID g17Id;
    private UUID g19Id;
    private UUID caliber9mmId;
    private UUID caliber40Id;
    private UUID weaponId;

    @BeforeEach
    void preparaArmaExistente() throws Exception {
        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new UserRegisterRequest("Atleta Edita Arma", EMAIL, PASSWORD))));

        var loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(EMAIL, PASSWORD))))
                .andReturn();
        accessToken = objectMapper.readTree(loginResult.getResponse().getContentAsString())
                .get("data").get("accessToken").asText();

        pistolaId = idByName("/api/weapon-catalog/types", "Pistola");
        revolverId = idByName("/api/weapon-catalog/types", "Revólver");
        glockId = idByName("/api/weapon-catalog/brands", "Glock");
        g17Id = idByName("/api/weapon-catalog/brands/" + glockId + "/models", "G17");
        g19Id = idByName("/api/weapon-catalog/brands/" + glockId + "/models", "G19");
        caliber9mmId = idByName("/api/weapon-catalog/calibers", "9mm");
        caliber40Id = idByName("/api/weapon-catalog/calibers", ".40 S&W");

        var registerRequest = new WeaponRegisterRequest(pistolaId, glockId, g17Id, caliber9mmId, "Original");
        var registerResult = mockMvc.perform(post("/api/weapons")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andReturn();
        weaponId = UUID.fromString(objectMapper.readTree(registerResult.getResponse().getContentAsString())
                .get("data").get("id").asText());
    }

    @Test
    void deveEditarSoOApelidoMantendoOResto() throws Exception {
        var request = new WeaponUpdateRequest(pistolaId, glockId, g17Id, caliber9mmId, "Minha 9mm de competição");

        mockMvc.perform(patch("/api/weapons/" + weaponId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.nickname").value("Minha 9mm de competição"))
                .andExpect(jsonPath("$.data.type.name").value("Pistola"))
                .andExpect(jsonPath("$.data.model.name").value("G17"));
    }

    @Test
    void deveCorrigirTipoModeloECalibre() throws Exception {
        var request = new WeaponUpdateRequest(revolverId, glockId, g19Id, caliber40Id, "Original");

        mockMvc.perform(patch("/api/weapons/" + weaponId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.type.name").value("Revólver"))
                .andExpect(jsonPath("$.data.model.name").value("G19"))
                .andExpect(jsonPath("$.data.caliber.name").value(".40 S&W"));
    }

    @Test
    void deveRejeitarCampoObrigatorioAusente() throws Exception {
        var request = new WeaponUpdateRequest(null, glockId, g17Id, caliber9mmId, "Original");

        mockMvc.perform(patch("/api/weapons/" + weaponId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    void deveRejeitarRequisicaoSemToken() throws Exception {
        var request = new WeaponUpdateRequest(pistolaId, glockId, g17Id, caliber9mmId, "Original");

        mockMvc.perform(patch("/api/weapons/" + weaponId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
    }

    @Test
    void deveRejeitarArmaInexistenteOuDeOutroAtleta() throws Exception {
        var request = new WeaponUpdateRequest(pistolaId, glockId, g17Id, caliber9mmId, "Original");

        mockMvc.perform(patch("/api/weapons/" + UUID.randomUUID())
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("WEAPON_NOT_FOUND"));
    }

    @Test
    void deveRejeitarTipoInexistente() throws Exception {
        var request = new WeaponUpdateRequest(UUID.randomUUID(), glockId, g17Id, caliber9mmId, "Original");

        mockMvc.perform(patch("/api/weapons/" + weaponId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("WEAPON_TYPE_NOT_FOUND"));
    }

    @Test
    void deveRejeitarMarcaInexistente() throws Exception {
        var request = new WeaponUpdateRequest(pistolaId, UUID.randomUUID(), g17Id, caliber9mmId, "Original");

        mockMvc.perform(patch("/api/weapons/" + weaponId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("WEAPON_BRAND_NOT_FOUND"));
    }

    @Test
    void deveRejeitarModeloInexistente() throws Exception {
        var request = new WeaponUpdateRequest(pistolaId, glockId, UUID.randomUUID(), caliber9mmId, "Original");

        mockMvc.perform(patch("/api/weapons/" + weaponId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("WEAPON_MODEL_NOT_FOUND"));
    }

    @Test
    void deveRejeitarCalibreInexistente() throws Exception {
        var request = new WeaponUpdateRequest(pistolaId, glockId, g17Id, UUID.randomUUID(), "Original");

        mockMvc.perform(patch("/api/weapons/" + weaponId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("WEAPON_CALIBER_NOT_FOUND"));
    }

    @Test
    void deveRejeitarModeloQueNaoPertenceAMarcaInformada() throws Exception {
        UUID taurusId = idByName("/api/weapon-catalog/brands", "Taurus");
        UUID modelo856Id = idByName("/api/weapon-catalog/brands/" + taurusId + "/models", "856");

        var request = new WeaponUpdateRequest(pistolaId, glockId, modelo856Id, caliber9mmId, "Original");

        mockMvc.perform(patch("/api/weapons/" + weaponId)
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
