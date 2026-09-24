package com.shottrack.backend.application.weapon;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shottrack.backend.application.user.gateway.repository.PendingRegistrationRepository;
import com.shottrack.backend.application.weapon.dto.WeaponRegisterRequest;
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

import java.util.Map;
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

    @Autowired
    private PendingRegistrationRepository pendingRegistrationRepository;

    private String accessToken;
    private UUID glockId;
    private UUID g17Id;
    private UUID caliber9mmId;

    @BeforeEach
    void registerAndLoginUser() throws Exception {
        accessToken = TestUsers.registerAndLogin(mockMvc, objectMapper, pendingRegistrationRepository,
                "Atleta Cadastra Arma", EMAIL, PASSWORD);

        glockId = idByName("/api/weapon-catalog/brands", "Glock");
        g17Id = idByName("/api/weapon-catalog/brands/" + glockId + "/models", "G17");
        caliber9mmId = idByName("/api/weapon-catalog/models/" + g17Id + "/calibers", "9mm");
    }

    @Test
    void shouldRegisterWeaponWithValidData() throws Exception {
        register(new WeaponRegisterRequest(g17Id, caliber9mmId))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.type.name").value("Pistola"))
                .andExpect(jsonPath("$.data.brand.name").value("Glock"))
                .andExpect(jsonPath("$.data.model.name").value("G17"))
                .andExpect(jsonPath("$.data.caliber.name").value("9mm"))
                .andExpect(jsonPath("$.data.nickname").isEmpty());
    }

    @Test
    void shouldDeriveTypeAndBrandFromModelIgnoringClientValues() throws Exception {
        UUID taurusId = idByName("/api/weapon-catalog/brands", "Taurus");
        UUID model605Id = idByName("/api/weapon-catalog/brands/" + taurusId + "/models", "605");
        UUID caliber357Id = idByName("/api/weapon-catalog/models/" + model605Id + "/calibers", ".357 Magnum");
        UUID carabinaId = idByName("/api/weapon-catalog/types", "Carabina");

        // typeId/brandId não fazem mais parte do contrato — mesmo enviados
        // (e errados), o servidor usa os do modelo
        var payload = Map.of(
                "modelId", model605Id,
                "caliberId", caliber357Id,
                "typeId", carabinaId,
                "brandId", glockId);

        var created = mockMvc.perform(post("/api/weapons")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.type.name").value("Revólver"))
                .andExpect(jsonPath("$.data.brand.name").value("Taurus"))
                .andReturn();
        String weaponId = objectMapper.readTree(created.getResponse().getContentAsString()).get("data").get("id").asText();

        // e é isso que ficou salvo, não só o que a resposta mostrou
        mockMvc.perform(get("/api/weapons")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(jsonPath("$.data[?(@.id == '" + weaponId + "')].type.name").value("Revólver"))
                .andExpect(jsonPath("$.data[?(@.id == '" + weaponId + "')].brand.name").value("Taurus"))
                .andExpect(jsonPath("$.data[?(@.id == '" + weaponId + "')].caliber.name").value(".357 Magnum"));
    }

    @Test
    void shouldRejectMissingModel() throws Exception {
        register(new WeaponRegisterRequest(null, caliber9mmId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    void shouldRejectMissingCaliber() throws Exception {
        register(new WeaponRegisterRequest(g17Id, null))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    void shouldRejectRequestWithoutToken() throws Exception {
        mockMvc.perform(post("/api/weapons")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new WeaponRegisterRequest(g17Id, caliber9mmId))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
    }

    @Test
    void shouldRejectNonExistentModel() throws Exception {
        register(new WeaponRegisterRequest(UUID.randomUUID(), caliber9mmId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("WEAPON_MODEL_NOT_FOUND"));
    }

    @Test
    void shouldRejectCaliberNotAllowedForModel() throws Exception {
        UUID caliber38Id = idByName("/api/weapon-catalog/calibers", ".38 Special");

        register(new WeaponRegisterRequest(g17Id, caliber38Id))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("WEAPON_CALIBER_NOT_ALLOWED_FOR_MODEL"));
    }

    @Test
    void shouldRejectNonExistentCaliberAsNotAllowedForModel() throws Exception {
        register(new WeaponRegisterRequest(g17Id, UUID.randomUUID()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("WEAPON_CALIBER_NOT_ALLOWED_FOR_MODEL"));
    }

    private ResultActions register(WeaponRegisterRequest request) throws Exception {
        return mockMvc.perform(post("/api/weapons")
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
