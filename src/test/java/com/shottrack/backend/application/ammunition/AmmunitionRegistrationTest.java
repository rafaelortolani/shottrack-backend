package com.shottrack.backend.application.ammunition;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shottrack.backend.application.ammunition.dto.AmmunitionRegisterRequest;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AmmunitionRegistrationTest {

    private static final String EMAIL = "atleta.cadastramunicao@shottrack.com";
    private static final String PASSWORD = "senha12345";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AmmunitionManufacturerRepository ammunitionManufacturerRepository;

    private String accessToken;
    private UUID cbcId;
    private UUID caliber9mmId;

    @BeforeEach
    void registerAndLoginUser() throws Exception {
        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new UserRegisterRequest("Atleta Cadastra Munição", EMAIL, PASSWORD))));

        var result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(EMAIL, PASSWORD))))
                .andReturn();

        JsonNode data = objectMapper.readTree(result.getResponse().getContentAsString()).get("data");
        accessToken = data.get("accessToken").asText();

        cbcId = ammunitionManufacturerRepository.findAll().stream()
                .filter(m -> m.getName().equals("CBC"))
                .findFirst()
                .orElseThrow()
                .getId();
        caliber9mmId = idByName("/api/weapon-catalog/calibers", "9mm");
    }

    @Test
    void shouldRegisterAmmunitionWithManufacturerOnly() throws Exception {
        var request = new AmmunitionRegisterRequest(cbcId, null, null, null, null, null, null, null);

        mockMvc.perform(post("/api/ammunitions")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.manufacturer.name").value("CBC"))
                .andExpect(jsonPath("$.data.nickname").isEmpty())
                .andExpect(jsonPath("$.data.caliber").isEmpty());
    }

    @Test
    void shouldRegisterAmmunitionWithNicknameOnly() throws Exception {
        var request = new AmmunitionRegisterRequest(null, null, "Minha 9mm de treino", null, null, null, null, null);

        mockMvc.perform(post("/api/ammunitions")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.nickname").value("Minha 9mm de treino"))
                .andExpect(jsonPath("$.data.manufacturer").isEmpty());
    }

    @Test
    void shouldRegisterAmmunitionWithAllFields() throws Exception {
        var request = new AmmunitionRegisterRequest(cbcId, caliber9mmId, "Minha 9mm de competição",
                new BigDecimal("124.00"), new BigDecimal("4.20"), "FMJ", "L2024A", "Boa precisão no HK25");

        mockMvc.perform(post("/api/ammunitions")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.manufacturer.name").value("CBC"))
                .andExpect(jsonPath("$.data.caliber.name").value("9mm"))
                .andExpect(jsonPath("$.data.nickname").value("Minha 9mm de competição"))
                .andExpect(jsonPath("$.data.projectileWeightGrains").value(124.00))
                .andExpect(jsonPath("$.data.powderCharge").value(4.20))
                .andExpect(jsonPath("$.data.projectileType").value("FMJ"))
                .andExpect(jsonPath("$.data.lot").value("L2024A"))
                .andExpect(jsonPath("$.data.notes").value("Boa precisão no HK25"));
    }

    @Test
    void shouldRejectRequestWithoutToken() throws Exception {
        var request = new AmmunitionRegisterRequest(cbcId, null, null, null, null, null, null, null);

        mockMvc.perform(post("/api/ammunitions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
    }

    @Test
    void shouldRejectWhenNeitherManufacturerNorNicknameInformed() throws Exception {
        var request = new AmmunitionRegisterRequest(null, caliber9mmId, null, null, null, null, null, null);

        mockMvc.perform(post("/api/ammunitions")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("AMMUNITION_IDENTIFICATION_REQUIRED"));
    }

    @Test
    void shouldRejectNonExistentManufacturer() throws Exception {
        var request = new AmmunitionRegisterRequest(UUID.randomUUID(), null, null, null, null, null, null, null);

        mockMvc.perform(post("/api/ammunitions")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("AMMUNITION_MANUFACTURER_NOT_FOUND"));
    }

    @Test
    void shouldRejectNonExistentCaliber() throws Exception {
        var request = new AmmunitionRegisterRequest(cbcId, UUID.randomUUID(), null, null, null, null, null, null);

        mockMvc.perform(post("/api/ammunitions")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("WEAPON_CALIBER_NOT_FOUND"));
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
