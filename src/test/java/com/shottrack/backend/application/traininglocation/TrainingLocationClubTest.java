package com.shottrack.backend.application.traininglocation;

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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** UC24/UC25/UC26 (ADR-0023): marca "é clube" no local de treino. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class TrainingLocationClubTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PendingRegistrationRepository pendingRegistrationRepository;

    private String token;

    @BeforeEach
    void register() throws Exception {
        token = TestUsers.registerAndLogin(mockMvc, objectMapper, pendingRegistrationRepository,
                "Atleta Teste", "atleta.clube@shottrack.com", "senha12345");
    }

    private ResultActions register(String clubJson) throws Exception {
        return mockMvc.perform(post("/api/training-locations")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Local\",\"city\":\"Campinas\",\"state\":\"SP\"" + clubJson + "}"));
    }

    private String idOf(ResultActions result) throws Exception {
        return objectMapper.readTree(result.andReturn().getResponse().getContentAsString()).get("data").get("id").asText();
    }

    @Test
    void shouldRegisterLocationAsClub() throws Exception {
        register(",\"club\":true")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.club").value(true));
    }

    @Test
    void shouldDefaultToNotClubWhenOmitted() throws Exception {
        register("")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.club").value(false));
    }

    @Test
    void shouldListClubFlagForTrueAndFalse() throws Exception {
        register(",\"club\":true").andExpect(status().isCreated());
        register(",\"club\":false").andExpect(status().isCreated());

        mockMvc.perform(get("/api/training-locations").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[?(@.club==true)]", org.hamcrest.Matchers.hasSize(1)))
                .andExpect(jsonPath("$.data[?(@.club==false)]", org.hamcrest.Matchers.hasSize(1)));
    }

    @Test
    void shouldEditOnlyClubFlagKeepingTheRest() throws Exception {
        String id = idOf(register(""));

        mockMvc.perform(patch("/api/training-locations/" + id)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"club\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.club").value(true))
                .andExpect(jsonPath("$.data.name").value("Local"))
                .andExpect(jsonPath("$.data.city").value("Campinas"))
                .andExpect(jsonPath("$.data.state").value("SP"));
    }

    @Test
    void shouldKeepClubFlagWhenEditingOtherFields() throws Exception {
        String id = idOf(register(",\"club\":true"));

        mockMvc.perform(patch("/api/training-locations/" + id)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Novo nome\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Novo nome"))
                .andExpect(jsonPath("$.data.club").value(true));
    }

    @Test
    void shouldUnsetClubFlag() throws Exception {
        String id = idOf(register(",\"club\":true"));

        mockMvc.perform(patch("/api/training-locations/" + id)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"club\":false}"))
                .andExpect(jsonPath("$.data.club").value(false));
    }
}
