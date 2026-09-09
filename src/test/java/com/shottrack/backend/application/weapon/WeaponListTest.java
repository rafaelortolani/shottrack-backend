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
class WeaponListTest {

    private static final String PASSWORD = "senha12345";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private UUID pistolaId;
    private UUID glockId;
    private UUID g17Id;
    private UUID caliber9mmId;

    @BeforeEach
    void carregaCatalogo() throws Exception {
        String tempToken = cadastraELoga("atleta.catalogo.list@shottrack.com");

        pistolaId = idByName(tempToken, "/api/weapon-catalog/types", "Pistola");
        glockId = idByName(tempToken, "/api/weapon-catalog/brands", "Glock");
        g17Id = idByName(tempToken, "/api/weapon-catalog/brands/" + glockId + "/models", "G17");
        caliber9mmId = idByName(tempToken, "/api/weapon-catalog/calibers", "9mm");
    }

    @Test
    void deveListarArmasDoAtleta() throws Exception {
        String token = cadastraELoga("atleta.listaarmas@shottrack.com");
        cadastraArma(token);
        cadastraArma(token);

        mockMvc.perform(get("/api/weapons")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2));
    }

    @Test
    void deveRetornarListaVaziaSemArmasCadastradas() throws Exception {
        String token = cadastraELoga("atleta.semarmas@shottrack.com");

        mockMvc.perform(get("/api/weapons")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    void deveRejeitarRequisicaoSemToken() throws Exception {
        mockMvc.perform(get("/api/weapons"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
    }

    @Test
    void nuncaDeveIncluirArmaDeOutroAtleta() throws Exception {
        String tokenA = cadastraELoga("atleta.a.armas@shottrack.com");
        String tokenB = cadastraELoga("atleta.b.armas@shottrack.com");
        cadastraArma(tokenA);

        mockMvc.perform(get("/api/weapons")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    private void cadastraArma(String token) throws Exception {
        var request = new WeaponRegisterRequest(pistolaId, glockId, g17Id, caliber9mmId, null);
        mockMvc.perform(post("/api/weapons")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));
    }

    private String cadastraELoga(String email) throws Exception {
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
