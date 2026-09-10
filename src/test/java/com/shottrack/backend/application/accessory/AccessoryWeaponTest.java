package com.shottrack.backend.application.accessory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shottrack.backend.application.accessory.dto.AccessoryRegisterRequest;
import com.shottrack.backend.application.accessory.dto.AssociateAccessoryWeaponRequest;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AccessoryWeaponTest {

    private static final String EMAIL = "atleta.associaacessorio@shottrack.com";
    private static final String PASSWORD = "senha12345";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String accessToken;
    private UUID accessoryId;
    private UUID weaponId;
    private UUID secondWeaponId;

    @BeforeEach
    void prepareAccessoryAndWeapons() throws Exception {
        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new UserRegisterRequest("Atleta Associa Acessório", EMAIL, PASSWORD))));

        var loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(EMAIL, PASSWORD))))
                .andReturn();
        accessToken = objectMapper.readTree(loginResult.getResponse().getContentAsString())
                .get("data").get("accessToken").asText();

        var accessoryResult = mockMvc.perform(post("/api/accessories")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AccessoryRegisterRequest("Luneta 4x32", "Luneta", null))))
                .andReturn();
        accessoryId = UUID.fromString(objectMapper.readTree(accessoryResult.getResponse().getContentAsString())
                .get("data").get("id").asText());

        UUID pistolaId = idByName("/api/weapon-catalog/types", "Pistola");
        UUID glockId = idByName("/api/weapon-catalog/brands", "Glock");
        UUID g17Id = idByName("/api/weapon-catalog/brands/" + glockId + "/models", "G17");
        UUID g19Id = idByName("/api/weapon-catalog/brands/" + glockId + "/models", "G19");
        UUID caliber9mmId = idByName("/api/weapon-catalog/calibers", "9mm");

        weaponId = registerWeapon(pistolaId, glockId, g17Id, caliber9mmId);
        secondWeaponId = registerWeapon(pistolaId, glockId, g19Id, caliber9mmId);
    }

    @Test
    void shouldAssociateAccessoryToWeapon() throws Exception {
        mockMvc.perform(post("/api/accessories/" + accessoryId + "/weapons")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AssociateAccessoryWeaponRequest(weaponId))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.weapons.length()").value(1))
                .andExpect(jsonPath("$.data.weapons[0].id").value(weaponId.toString()));
    }

    @Test
    void shouldAssociateSameAccessoryToSecondWeapon() throws Exception {
        associate(weaponId);

        mockMvc.perform(post("/api/accessories/" + accessoryId + "/weapons")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AssociateAccessoryWeaponRequest(secondWeaponId))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.weapons.length()").value(2));
    }

    @Test
    void shouldDisassociateWithoutAffectingOtherAssociations() throws Exception {
        associate(weaponId);
        associate(secondWeaponId);

        mockMvc.perform(delete("/api/accessories/" + accessoryId + "/weapons/" + weaponId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.weapons.length()").value(1))
                .andExpect(jsonPath("$.data.weapons[0].id").value(secondWeaponId.toString()));
    }

    @Test
    void shouldRejectRequestWithoutToken() throws Exception {
        mockMvc.perform(post("/api/accessories/" + accessoryId + "/weapons")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AssociateAccessoryWeaponRequest(weaponId))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
    }

    @Test
    void shouldRejectNonExistentAccessory() throws Exception {
        mockMvc.perform(post("/api/accessories/" + UUID.randomUUID() + "/weapons")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AssociateAccessoryWeaponRequest(weaponId))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("ACCESSORY_NOT_FOUND"));
    }

    @Test
    void shouldRejectNonExistentWeapon() throws Exception {
        mockMvc.perform(post("/api/accessories/" + accessoryId + "/weapons")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AssociateAccessoryWeaponRequest(UUID.randomUUID()))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("WEAPON_NOT_FOUND"));
    }

    @Test
    void shouldRejectAlreadyAssociatedAccessoryAndWeapon() throws Exception {
        associate(weaponId);

        mockMvc.perform(post("/api/accessories/" + accessoryId + "/weapons")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AssociateAccessoryWeaponRequest(weaponId))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("ACCESSORY_ALREADY_ASSOCIATED"));
    }

    @Test
    void shouldRejectDisassociatingWhenAssociationDoesNotExist() throws Exception {
        mockMvc.perform(delete("/api/accessories/" + accessoryId + "/weapons/" + weaponId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("ACCESSORY_NOT_ASSOCIATED"));
    }

    private void associate(UUID weaponIdToAssociate) throws Exception {
        mockMvc.perform(post("/api/accessories/" + accessoryId + "/weapons")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new AssociateAccessoryWeaponRequest(weaponIdToAssociate))));
    }

    private UUID registerWeapon(UUID typeId, UUID brandId, UUID modelId, UUID caliberId) throws Exception {
        var request = new WeaponRegisterRequest(typeId, brandId, modelId, caliberId, null);
        var result = mockMvc.perform(post("/api/weapons")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andReturn();

        return UUID.fromString(objectMapper.readTree(result.getResponse().getContentAsString()).get("data").get("id").asText());
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
