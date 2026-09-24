package com.shottrack.backend.application.weapon;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shottrack.backend.application.user.gateway.repository.PendingRegistrationRepository;
import com.shottrack.backend.application.weapon.dto.WeaponRegisterRequest;
import com.shottrack.backend.application.weapon.dto.WeaponUpdateRequest;
import com.shottrack.backend.support.TestUsers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
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

    @Autowired
    private PendingRegistrationRepository pendingRegistrationRepository;

    private String accessToken;
    private UUID g17Id;
    private UUID caliber9mmId;
    private UUID weaponId;

    @BeforeEach
    void prepareExistingWeapon() throws Exception {
        accessToken = TestUsers.registerAndLogin(mockMvc, objectMapper, pendingRegistrationRepository,
                "Atleta Edita Arma", EMAIL, PASSWORD);

        UUID glockId = idByName("/api/weapon-catalog/brands", "Glock");
        g17Id = idByName("/api/weapon-catalog/brands/" + glockId + "/models", "G17");
        caliber9mmId = idByName("/api/weapon-catalog/models/" + g17Id + "/calibers", "9mm");

        weaponId = registerWeapon(accessToken);
        update(weaponId, new WeaponUpdateRequest(g17Id, caliber9mmId, "Original"))
                .andExpect(status().isOk());
    }

    @Test
    void shouldEditOnlyNicknameKeepingTheRest() throws Exception {
        update(weaponId, new WeaponUpdateRequest(g17Id, caliber9mmId, "Minha 9mm de competição"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.nickname").value("Minha 9mm de competição"))
                .andExpect(jsonPath("$.data.type.name").value("Pistola"))
                .andExpect(jsonPath("$.data.brand.name").value("Glock"))
                .andExpect(jsonPath("$.data.model.name").value("G17"))
                .andExpect(jsonPath("$.data.caliber.name").value("9mm"));
    }

    @Test
    void shouldReflectTypeAndBrandOfNewModelWithDifferentCaliber() throws Exception {
        UUID taurusId = idByName("/api/weapon-catalog/brands", "Taurus");
        UUID model605Id = idByName("/api/weapon-catalog/brands/" + taurusId + "/models", "605");
        UUID caliber357Id = idByName("/api/weapon-catalog/models/" + model605Id + "/calibers", ".357 Magnum");

        update(weaponId, new WeaponUpdateRequest(model605Id, caliber357Id, "Original"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.type.name").value("Revólver"))
                .andExpect(jsonPath("$.data.brand.name").value("Taurus"))
                .andExpect(jsonPath("$.data.model.name").value("605"))
                .andExpect(jsonPath("$.data.caliber.name").value(".357 Magnum"));

        // e é isso que ficou salvo, não só o que a resposta mostrou
        mockMvc.perform(get("/api/weapons")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(jsonPath("$.data[0].type.name").value("Revólver"))
                .andExpect(jsonPath("$.data[0].brand.name").value("Taurus"));
    }

    @Test
    void shouldRejectMissingModel() throws Exception {
        update(weaponId, new WeaponUpdateRequest(null, caliber9mmId, "Original"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    void shouldRejectMissingCaliber() throws Exception {
        update(weaponId, new WeaponUpdateRequest(g17Id, null, "Original"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    void shouldRejectRequestWithoutToken() throws Exception {
        mockMvc.perform(patch("/api/weapons/" + weaponId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new WeaponUpdateRequest(g17Id, caliber9mmId, "Original"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
    }

    @Test
    void shouldRejectNonExistentWeapon() throws Exception {
        update(UUID.randomUUID(), new WeaponUpdateRequest(g17Id, caliber9mmId, "Original"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("WEAPON_NOT_FOUND"));
    }

    @Test
    void shouldRejectAnotherAthletesWeapon() throws Exception {
        String otherToken = TestUsers.registerAndLogin(mockMvc, objectMapper, pendingRegistrationRepository,
                "Outro Atleta", "atleta.outroeditaarma@shottrack.com", PASSWORD);
        UUID otherWeaponId = registerWeapon(otherToken);

        update(otherWeaponId, new WeaponUpdateRequest(g17Id, caliber9mmId, "Não é minha"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("WEAPON_NOT_FOUND"));
    }

    @Test
    void shouldRejectNonExistentModel() throws Exception {
        update(weaponId, new WeaponUpdateRequest(UUID.randomUUID(), caliber9mmId, "Original"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("WEAPON_MODEL_NOT_FOUND"));
    }

    @Test
    void shouldRejectCaliberNotAllowedForModel() throws Exception {
        UUID caliber38Id = idByName("/api/weapon-catalog/calibers", ".38 Special");

        update(weaponId, new WeaponUpdateRequest(g17Id, caliber38Id, "Original"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("WEAPON_CALIBER_NOT_ALLOWED_FOR_MODEL"));
    }

    private UUID registerWeapon(String token) throws Exception {
        var result = mockMvc.perform(post("/api/weapons")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new WeaponRegisterRequest(g17Id, caliber9mmId))))
                .andExpect(status().isCreated())
                .andReturn();
        return UUID.fromString(objectMapper.readTree(result.getResponse().getContentAsString())
                .get("data").get("id").asText());
    }

    private ResultActions update(UUID id, WeaponUpdateRequest request) throws Exception {
        return mockMvc.perform(patch("/api/weapons/" + id)
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));
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
