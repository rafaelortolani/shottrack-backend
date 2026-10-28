package com.shottrack.backend.application.visit;

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
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** UC31/UC35 (ADR-0023): tipo da visita e âmbito da competição. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class VisitTypeTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PendingRegistrationRepository pendingRegistrationRepository;

    private String token;
    private UUID locationId;

    @BeforeEach
    void prepare() throws Exception {
        token = TestUsers.registerAndLogin(mockMvc, objectMapper, pendingRegistrationRepository,
                "Atleta Teste", "atleta.tipovisita@shottrack.com", "senha12345");
        var result = mockMvc.perform(post("/api/training-locations")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Clube\",\"city\":\"São Paulo\",\"state\":\"SP\"}")).andReturn();
        locationId = UUID.fromString(objectMapper.readTree(result.getResponse().getContentAsString()).get("data").get("id").asText());
    }

    private ResultActions start(String extraJson) throws Exception {
        return mockMvc.perform(post("/api/visits")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"trainingLocationId\":\"" + locationId + "\"" + extraJson + "}"));
    }

    @Test
    void shouldDefaultToTrainingWithoutScopeWhenTypeIsOmitted() throws Exception {
        start("")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.visitType").value("TREINO"))
                .andExpect(jsonPath("$.data.competitionScope").isEmpty());
    }

    @Test
    void shouldStartExplicitTrainingVisit() throws Exception {
        start(",\"visitType\":\"TREINO\"")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.visitType").value("TREINO"));
    }

    @Test
    void shouldStartCompetitionWithScope() throws Exception {
        start(",\"visitType\":\"COMPETICAO\",\"competitionScope\":\"NACIONAL\"")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.visitType").value("COMPETICAO"))
                .andExpect(jsonPath("$.data.competitionScope").value("NACIONAL"));
    }

    @Test
    void shouldStartCompetitionWithoutScope() throws Exception {
        start(",\"visitType\":\"COMPETICAO\"")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.visitType").value("COMPETICAO"))
                .andExpect(jsonPath("$.data.competitionScope").isEmpty());
    }

    @Test
    void shouldRejectScopeWithTrainingType() throws Exception {
        start(",\"visitType\":\"TREINO\",\"competitionScope\":\"ESTADUAL\"")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    void shouldRejectScopeWhenTypeIsOmitted() throws Exception {
        start(",\"competitionScope\":\"ESTADUAL\"")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    void shouldRejectValuesOutsideTheAcceptedOnes() throws Exception {
        start(",\"visitType\":\"AMISTOSO\"")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
        start(",\"visitType\":\"COMPETICAO\",\"competitionScope\":\"MUNDIAL\"")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    void shouldKeepTypeAndScopeOnClosedVisit() throws Exception {
        var result = start(",\"visitType\":\"COMPETICAO\",\"competitionScope\":\"INTERNACIONAL\"").andReturn();
        String id = objectMapper.readTree(result.getResponse().getContentAsString()).get("data").get("id").asText();

        mockMvc.perform(post("/api/visits/" + id + "/closure").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.visitType").value("COMPETICAO"))
                .andExpect(jsonPath("$.data.competitionScope").value("INTERNACIONAL"));
    }

    @Test
    void shouldListTypeAndScopeForTrainingAndCompetition() throws Exception {
        start("").andExpect(status().isCreated());
        start(",\"visitType\":\"COMPETICAO\",\"competitionScope\":\"ESTADUAL\"").andExpect(status().isCreated());

        mockMvc.perform(get("/api/visits").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[?(@.visitType=='TREINO')].competitionScope",
                        org.hamcrest.Matchers.contains(org.hamcrest.Matchers.nullValue())))
                .andExpect(jsonPath("$.data[?(@.visitType=='COMPETICAO')].competitionScope").value("ESTADUAL"));
    }

    @Test
    void shouldStillAcceptLegacyRequestWithoutNewFields() throws Exception {
        mockMvc.perform(post("/api/visits")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"trainingLocationId\":\"" + locationId + "\",\"observations\":\"antigo\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.observations").value("antigo"))
                .andExpect(jsonPath("$.data.visitType").value("TREINO"));
    }
}
