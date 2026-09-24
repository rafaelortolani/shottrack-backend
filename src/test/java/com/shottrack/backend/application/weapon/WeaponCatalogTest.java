package com.shottrack.backend.application.weapon;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shottrack.backend.application.user.gateway.repository.PendingRegistrationRepository;
import com.shottrack.backend.support.TestUsers;
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
class WeaponCatalogTest {

    private static final String EMAIL = "atleta.catalogo@shottrack.com";
    private static final String PASSWORD = "senha12345";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PendingRegistrationRepository pendingRegistrationRepository;

    private String accessToken;

    @BeforeEach
    void registerAndLoginUser() throws Exception {
        accessToken = TestUsers.registerAndLogin(mockMvc, objectMapper, pendingRegistrationRepository,
                "Atleta Catálogo", EMAIL, PASSWORD);
    }

    @Test
    void shouldListTypes() throws Exception {
        mockMvc.perform(get("/api/weapon-catalog/types")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.name == 'Pistola')]").isNotEmpty());
    }

    @Test
    void shouldListBrands() throws Exception {
        mockMvc.perform(get("/api/weapon-catalog/brands")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.name == 'Glock')]").isNotEmpty());
    }

    @Test
    void shouldListModelsOfAValidBrandWithTheirType() throws Exception {
        UUID taurusId = brandId("Taurus");

        mockMvc.perform(get("/api/weapon-catalog/brands/" + taurusId + "/models")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.name == 'G2C')].type.name").value("Pistola"))
                .andExpect(jsonPath("$.data[?(@.name == '605')].type.name").value("Revólver"))
                .andExpect(jsonPath("$.data[*].type.id").value(org.hamcrest.Matchers.everyItem(org.hamcrest.Matchers.notNullValue())));
    }

    @Test
    void shouldListValidCalibersOfAValidModel() throws Exception {
        UUID model605Id = modelId("Taurus", "605");

        mockMvc.perform(get("/api/weapon-catalog/models/" + model605Id + "/calibers")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[*].name", org.hamcrest.Matchers.contains(".357 Magnum", ".38 Special")));
    }

    @Test
    void shouldRejectNonExistentModel() throws Exception {
        mockMvc.perform(get("/api/weapon-catalog/models/" + UUID.randomUUID() + "/calibers")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("WEAPON_MODEL_NOT_FOUND"));
    }

    @Test
    void shouldRejectNonExistentBrand() throws Exception {
        mockMvc.perform(get("/api/weapon-catalog/brands/" + UUID.randomUUID() + "/models")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("WEAPON_BRAND_NOT_FOUND"));
    }

    @Test
    void shouldListCalibers() throws Exception {
        mockMvc.perform(get("/api/weapon-catalog/calibers")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.name == '9mm')]").isNotEmpty());
    }

    @Test
    void shouldRejectRequestWithoutToken() throws Exception {
        mockMvc.perform(get("/api/weapon-catalog/types"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
    }

    private UUID modelId(String brandName, String modelName) throws Exception {
        var result = mockMvc.perform(get("/api/weapon-catalog/brands/" + brandId(brandName) + "/models")
                        .header("Authorization", "Bearer " + accessToken))
                .andReturn();

        JsonNode models = objectMapper.readTree(result.getResponse().getContentAsString()).get("data");
        for (JsonNode model : models) {
            if (model.get("name").asText().equals(modelName)) {
                return UUID.fromString(model.get("id").asText());
            }
        }
        throw new AssertionError("Modelo não encontrado no catálogo: " + brandName + " " + modelName);
    }

    private UUID brandId(String name) throws Exception {
        var result = mockMvc.perform(get("/api/weapon-catalog/brands")
                        .header("Authorization", "Bearer " + accessToken))
                .andReturn();

        JsonNode brands = objectMapper.readTree(result.getResponse().getContentAsString()).get("data");
        for (JsonNode brand : brands) {
            if (brand.get("name").asText().equals(name)) {
                return UUID.fromString(brand.get("id").asText());
            }
        }
        throw new AssertionError("Marca não encontrada no catálogo: " + name);
    }
}
