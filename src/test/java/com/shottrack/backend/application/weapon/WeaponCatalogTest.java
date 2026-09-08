package com.shottrack.backend.application.weapon;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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

    private String accessToken;

    @BeforeEach
    void cadastraELogaUsuario() throws Exception {
        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new UserRegisterRequest("Atleta Catálogo", EMAIL, PASSWORD))));

        var result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(EMAIL, PASSWORD))))
                .andReturn();

        JsonNode data = objectMapper.readTree(result.getResponse().getContentAsString()).get("data");
        accessToken = data.get("accessToken").asText();
    }

    @Test
    void deveListarTipos() throws Exception {
        mockMvc.perform(get("/api/weapon-catalog/types")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.name == 'Pistola')]").isNotEmpty());
    }

    @Test
    void deveListarMarcas() throws Exception {
        mockMvc.perform(get("/api/weapon-catalog/brands")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.name == 'Glock')]").isNotEmpty());
    }

    @Test
    void deveListarModelosDeUmaMarcaValida() throws Exception {
        UUID glockId = brandId("Glock");

        mockMvc.perform(get("/api/weapon-catalog/brands/" + glockId + "/models")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.name == 'G17')]").isNotEmpty());
    }

    @Test
    void deveRejeitarMarcaInexistente() throws Exception {
        mockMvc.perform(get("/api/weapon-catalog/brands/" + UUID.randomUUID() + "/models")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("WEAPON_BRAND_NOT_FOUND"));
    }

    @Test
    void deveListarCalibres() throws Exception {
        mockMvc.perform(get("/api/weapon-catalog/calibers")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.name == '9mm')]").isNotEmpty());
    }

    @Test
    void deveRejeitarRequisicaoSemToken() throws Exception {
        mockMvc.perform(get("/api/weapon-catalog/types"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
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
