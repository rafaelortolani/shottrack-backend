package com.shottrack.backend.application.modality;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shottrack.backend.application.user.gateway.repository.PendingRegistrationRepository;
import com.shottrack.backend.support.TestUsers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ResultTypeCatalogTest {

    private static final String EMAIL = "atleta.tiporesultado@shottrack.com";
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
                "Atleta Tipo Resultado", EMAIL, PASSWORD);
    }

    @Test
    void shouldListResultTypeCatalog() throws Exception {
        mockMvc.perform(get("/api/result-type-catalog")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(9))
                .andExpect(jsonPath("$.data[?(@.name == 'Pontuação')]").isNotEmpty())
                .andExpect(jsonPath("$.data[?(@.name == 'Tempo')]").isNotEmpty())
                .andExpect(jsonPath("$.data[?(@.name == 'Agrupamento')]").isNotEmpty())
                .andExpect(jsonPath("$.data[?(@.name == 'Acertos')]").isNotEmpty())
                .andExpect(jsonPath("$.data[?(@.name == 'Erros')]").isNotEmpty())
                .andExpect(jsonPath("$.data[?(@.name == 'Penalidades')]").isNotEmpty())
                .andExpect(jsonPath("$.data[?(@.name == 'Fator de desempenho')]").isNotEmpty())
                .andExpect(jsonPath("$.data[?(@.name == 'Exercício concluído')]").isNotEmpty())
                .andExpect(jsonPath("$.data[?(@.name == 'Anotação livre')]").isNotEmpty());
    }

    @Test
    void shouldRejectRequestWithoutToken() throws Exception {
        mockMvc.perform(get("/api/result-type-catalog"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
    }
}
