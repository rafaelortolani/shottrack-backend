package com.shottrack.backend.application.accessory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shottrack.backend.application.accessory.dto.AccessoryRegisterRequest;
import com.shottrack.backend.application.accessory.dto.AssociateAccessoryWeaponRequest;
import com.shottrack.backend.application.auth.dto.LoginRequest;
import com.shottrack.backend.application.user.dto.UserRegisterRequest;
import com.shottrack.backend.application.weapon.dto.WeaponRegisterRequest;
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
class AccessoryDeleteTest {

    private static final String PASSWORD = "senha12345";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldDeleteAccessorySuccessfullyIncludingItsAssociations() throws Exception {
        String token = registerAndLogin("atleta.excluiacessorio@shottrack.com");
        UUID accessoryId = registerAccessory(token);
        UUID weaponId = registerWeapon(token);

        mockMvc.perform(post("/api/accessories/" + accessoryId + "/weapons")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AssociateAccessoryWeaponRequest(weaponId))))
                .andExpect(status().isCreated());

        mockMvc.perform(delete("/api/accessories/" + accessoryId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/accessories")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    void shouldRejectRequestWithoutToken() throws Exception {
        mockMvc.perform(delete("/api/accessories/" + UUID.randomUUID()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
    }

    @Test
    void shouldRejectNonExistentAccessory() throws Exception {
        String token = registerAndLogin("atleta.acessorioinexistente@shottrack.com");

        mockMvc.perform(delete("/api/accessories/" + UUID.randomUUID())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("ACCESSORY_NOT_FOUND"));
    }

    @Test
    void shouldRejectDeletionOfAnotherAthletesAccessory() throws Exception {
        String tokenDono = registerAndLogin("atleta.donoacessorio@shottrack.com");
        String tokenOutro = registerAndLogin("atleta.naoedonoacessorio@shottrack.com");
        UUID accessoryId = registerAccessory(tokenDono);

        mockMvc.perform(delete("/api/accessories/" + accessoryId)
                        .header("Authorization", "Bearer " + tokenOutro))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("ACCESSORY_NOT_FOUND"));
    }

    private UUID registerAccessory(String token) throws Exception {
        var request = new AccessoryRegisterRequest("Acessório pra excluir", null, null);
        var result = mockMvc.perform(post("/api/accessories")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andReturn();

        return UUID.fromString(objectMapper.readTree(result.getResponse().getContentAsString()).get("data").get("id").asText());
    }

    private UUID registerWeapon(String token) throws Exception {
        UUID pistolaId = idByName(token, "/api/weapon-catalog/types", "Pistola");
        UUID glockId = idByName(token, "/api/weapon-catalog/brands", "Glock");
        UUID g17Id = idByName(token, "/api/weapon-catalog/brands/" + glockId + "/models", "G17");
        UUID caliber9mmId = idByName(token, "/api/weapon-catalog/calibers", "9mm");

        var request = new WeaponRegisterRequest(pistolaId, glockId, g17Id, caliber9mmId, null);
        var result = mockMvc.perform(post("/api/weapons")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andReturn();

        return UUID.fromString(objectMapper.readTree(result.getResponse().getContentAsString()).get("data").get("id").asText());
    }

    private String registerAndLogin(String email) throws Exception {
        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new UserRegisterRequest("Atleta Teste", email, PASSWORD))));

        var result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(email, PASSWORD))))
                .andReturn();

        JsonNode data = objectMapper.readTree(result.getResponse().getContentAsString()).get("data");
        return data.get("accessToken").asText();
    }

    private UUID idByName(String token, String path, String name) throws Exception {
        var result = mockMvc.perform(get(path)
                        .header("Authorization", "Bearer " + token))
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
