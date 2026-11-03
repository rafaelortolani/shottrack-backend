package com.shottrack.backend.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shottrack.backend.application.accessory.dto.AccessoryRegisterRequest;
import com.shottrack.backend.application.accessory.dto.AccessoryUpdateRequest;
import com.shottrack.backend.application.accessory.dto.AssociateAccessoryWeaponRequest;
import com.shottrack.backend.application.modality.dto.AddModalityResultTypeRequest;
import com.shottrack.backend.application.modality.dto.AddPracticedModalityRequest;
import com.shottrack.backend.application.traininglocation.dto.TrainingLocationRegisterRequest;
import com.shottrack.backend.application.traininglocation.dto.TrainingLocationUpdateRequest;
import com.shottrack.backend.application.user.gateway.repository.PendingRegistrationRepository;
import com.shottrack.backend.support.DashboardTestSupport;
import com.shottrack.backend.support.SeriesTestSupport;
import com.shottrack.backend.support.TestUsers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Q05 (auditoria): o atleta B nunca vê nem altera o dado do atleta A — acessório (UC20), local de treino (UC26),
 * associação acessório–arma (UC19), evolução (UC46), modalidades praticadas (UC12) e tipos de resultado por
 * modalidade (UC30). O esperado é NOT_FOUND do recurso, com a MESMA resposta de um id que não existe (não vaza
 * que o recurso é de outra pessoa), e o dado de A fica intacto.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AthleteIsolationTest {

    private static final String PASSWORD = "senha12345";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PendingRegistrationRepository pendingRegistrationRepository;

    private String tokenA;
    private String tokenB;
    private UUID accessoryA;
    private UUID accessoryB;
    private UUID weaponA;
    private UUID weaponB;
    private UUID locationA;
    private UUID ipsc;
    private UUID tempo;
    /** Tipo que só A configurou em IPSC (Tempo, Pontuação e Fator de desempenho já vêm como sugestão padrão). */
    private UUID custom;

    @BeforeEach
    void prepareTwoAthletes() throws Exception {
        tokenA = TestUsers.registerAndLogin(mockMvc, objectMapper, pendingRegistrationRepository, "Atleta A", "isolamento.a@shottrack.com", PASSWORD);
        tokenB = TestUsers.registerAndLogin(mockMvc, objectMapper, pendingRegistrationRepository, "Atleta B", "isolamento.b@shottrack.com", PASSWORD);

        UUID lunetaType = catalogId(tokenA, "/api/accessory-catalog/types", "Luneta");
        accessoryA = createdId(post("/api/accessories"), tokenA, new AccessoryRegisterRequest("Luneta do A", lunetaType, "obs do A"));
        accessoryB = createdId(post("/api/accessories"), tokenB, new AccessoryRegisterRequest("Luneta do B", lunetaType, null));
        weaponA = SeriesTestSupport.registerWeapon(mockMvc, objectMapper, tokenA, "Arma do A");
        weaponB = SeriesTestSupport.registerWeapon(mockMvc, objectMapper, tokenB, "Arma do B");
        locationA = createdId(post("/api/training-locations"), tokenA, new TrainingLocationRegisterRequest("Clube do A", "Campinas", "SP"));

        // A pratica IPSC (abrir treino já adiciona a modalidade) e configura "Tempo" nela
        SeriesTestSupport.openTraining(mockMvc, objectMapper, tokenA, "IPSC");
        ipsc = DashboardTestSupport.modalityId(mockMvc, objectMapper, tokenA, "IPSC");
        tempo = DashboardTestSupport.resultTypeId(mockMvc, objectMapper, tokenA, "Tempo");
        custom = DashboardTestSupport.resultTypeId(mockMvc, objectMapper, tokenA, "Anotação livre");
        DashboardTestSupport.configureResultType(mockMvc, objectMapper, tokenA, ipsc, custom);
    }

    // ---- UC20: editar acessório ----

    @Test
    void shouldNotLetAthleteBEditAthleteAsAccessory() throws Exception {
        String foreign = body(send(patch("/api/accessories/" + accessoryA), tokenB, new AccessoryUpdateRequest("Roubado", null, "invadido"))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.error.code").value("ACCESSORY_NOT_FOUND")));
        String missing = body(send(patch("/api/accessories/" + UUID.randomUUID()), tokenB, new AccessoryUpdateRequest("Roubado", null, "invadido"))
                .andExpect(status().isNotFound()));

        assertThat(foreign).isEqualTo(missing);
        mockMvc.perform(get("/api/accessories").header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].name").value("Luneta do A"))
                .andExpect(jsonPath("$.data[0].notes").value("obs do A"));
    }

    // ---- UC26: editar local de treino ----

    @Test
    void shouldNotLetAthleteBEditAthleteAsTrainingLocation() throws Exception {
        String foreign = body(send(patch("/api/training-locations/" + locationA), tokenB,
                new TrainingLocationUpdateRequest("Invadido", "Outra", "RJ", true))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.error.code").value("TRAINING_LOCATION_NOT_FOUND")));
        String missing = body(send(patch("/api/training-locations/" + UUID.randomUUID()), tokenB,
                new TrainingLocationUpdateRequest("Invadido", "Outra", "RJ", true))
                .andExpect(status().isNotFound()));

        assertThat(foreign).isEqualTo(missing);
        mockMvc.perform(get("/api/training-locations").header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].name").value("Clube do A"))
                .andExpect(jsonPath("$.data[0].city").value("Campinas"))
                .andExpect(jsonPath("$.data[0].state").value("SP"));
    }

    // ---- UC19: associar/desassociar acessório–arma ----

    @Test
    void shouldNotAssociateAnotherAthletesAccessoryOrWeapon() throws Exception {
        // acessório de A + arma de B
        String foreignAccessory = body(associate(accessoryA, weaponB, tokenB)
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.error.code").value("ACCESSORY_NOT_FOUND")));
        assertThat(foreignAccessory).isEqualTo(body(associate(UUID.randomUUID(), weaponB, tokenB).andExpect(status().isNotFound())));

        // acessório de B + arma de A
        String foreignWeapon = body(associate(accessoryB, weaponA, tokenB)
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.error.code").value("WEAPON_NOT_FOUND")));
        assertThat(foreignWeapon).isEqualTo(body(associate(accessoryB, UUID.randomUUID(), tokenB).andExpect(status().isNotFound())));

        // acessório e arma de A, por B
        associate(accessoryA, weaponA, tokenB).andExpect(status().isNotFound());

        // nada foi associado: A ainda consegue associar (não é conflito) e B também a si mesmo
        associate(accessoryA, weaponA, tokenA).andExpect(status().isCreated()).andExpect(jsonPath("$.data.weapons.length()").value(1));
        associate(accessoryB, weaponB, tokenB).andExpect(status().isCreated()).andExpect(jsonPath("$.data.weapons.length()").value(1));
    }

    @Test
    void shouldNotDisassociateAnotherAthletesAccessoryOrWeapon() throws Exception {
        associate(accessoryA, weaponA, tokenA).andExpect(status().isCreated());
        associate(accessoryB, weaponB, tokenB).andExpect(status().isCreated());

        // acessório de A por B (com a arma de A e com a de B)
        String foreignAccessory = body(disassociate(accessoryA, weaponA, tokenB)
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.error.code").value("ACCESSORY_NOT_FOUND")));
        assertThat(foreignAccessory).isEqualTo(body(disassociate(UUID.randomUUID(), weaponA, tokenB).andExpect(status().isNotFound())));
        disassociate(accessoryA, weaponB, tokenB).andExpect(status().isNotFound());

        // acessório de B com a arma de A: a arma não é de B
        disassociate(accessoryB, weaponA, tokenB).andExpect(status().isNotFound());

        // as associações de A e de B seguem de pé
        disassociate(accessoryA, weaponA, tokenA).andExpect(status().isOk()).andExpect(jsonPath("$.data.weapons.length()").value(0));
        disassociate(accessoryB, weaponB, tokenB).andExpect(status().isOk()).andExpect(jsonPath("$.data.weapons.length()").value(0));
    }

    // ---- UC46: evolução ----

    @Test
    void shouldNotRevealAthleteAsEvolutionDataToAthleteB() throws Exception {
        // B não pratica IPSC: mesma resposta de uma modalidade qualquer que ele não pratique
        String foreignModality = body(evolution(tokenB, ipsc, tempo)
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.error.code").value("MODALITY_NOT_PRACTICED")));
        assertThat(foreignModality).isEqualTo(body(evolution(tokenB, UUID.randomUUID(), tempo).andExpect(status().isNotFound())));

        // B passa a praticar IPSC (recebe só a sugestão padrão); o tipo que só A configurou não vale para B
        mockMvc.perform(post("/api/practiced-modalities").header("Authorization", "Bearer " + tokenB)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new AddPracticedModalityRequest(ipsc)))).andExpect(status().isCreated());
        String foreignResultType = body(evolution(tokenB, ipsc, custom)
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.error.code").value("RESULT_TYPE_NOT_CONFIGURED_FOR_TRAINING")));
        assertThat(foreignResultType).isEqualTo(body(evolution(tokenB, ipsc, UUID.randomUUID()).andExpect(status().isNotFound())));
    }

    @Test
    void shouldNotMixAthleteAsValuesIntoAthleteBsEvolution() throws Exception {
        UUID trainingA = SeriesTestSupport.openTraining(mockMvc, objectMapper, tokenA, "IPSC");
        DashboardTestSupport.registerSeriesWithResult(mockMvc, objectMapper, tokenA, trainingA, tempo, "9.5");

        // B pratica a mesma modalidade com o mesmo tipo, mas não registrou nada: a evolução dele é vazia
        UUID trainingB = SeriesTestSupport.openTraining(mockMvc, objectMapper, tokenB, "IPSC");
        assertThat(trainingB).isNotNull();
        DashboardTestSupport.configureResultType(mockMvc, objectMapper, tokenB, ipsc, tempo);
        evolution(tokenB, ipsc, tempo).andExpect(status().isOk()).andExpect(jsonPath("$.data.length()").value(0));
        evolution(tokenA, ipsc, tempo).andExpect(status().isOk()).andExpect(jsonPath("$.data.length()").value(1));
    }

    // ---- UC12: modalidades praticadas ----

    @Test
    void shouldNotListAthleteAsPracticedModalitiesToAthleteB() throws Exception {
        mockMvc.perform(get("/api/practiced-modalities").header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(0));
        mockMvc.perform(get("/api/practiced-modalities").header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.id=='" + ipsc + "')]").isNotEmpty());
    }

    @Test
    void shouldNotLetAthleteBRemoveAthleteAsPracticedModality() throws Exception {
        String foreign = body(mockMvc.perform(delete("/api/practiced-modalities/" + ipsc).header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isNotFound()));
        assertThat(foreign).contains("MODALITY_NOT_ASSOCIATED");

        // A ainda pratica IPSC, com o tipo configurado
        mockMvc.perform(get("/api/practiced-modalities").header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data[?(@.id=='" + ipsc + "')]").isNotEmpty());
        mockMvc.perform(get("/api/practiced-modalities/" + ipsc + "/result-types").header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.id=='" + tempo + "')]").isNotEmpty())
                .andExpect(jsonPath("$.data[?(@.id=='" + custom + "')]").isNotEmpty());
    }

    // ---- UC30: tipos de resultado por modalidade ----

    @Test
    void shouldNotLetAthleteBSeeOrChangeAthleteAsResultTypesOfAModality() throws Exception {
        // B não pratica IPSC: nem lista, nem adiciona, nem remove
        mockMvc.perform(get("/api/practiced-modalities/" + ipsc + "/result-types").header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.error.code").value("MODALITY_NOT_PRACTICED"));
        send(post("/api/practiced-modalities/" + ipsc + "/result-types"), tokenB, new AddModalityResultTypeRequest(tempo))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.error.code").value("MODALITY_NOT_PRACTICED"));
        mockMvc.perform(delete("/api/practiced-modalities/" + ipsc + "/result-types/" + custom).header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.error.code").value("MODALITY_NOT_PRACTICED"));

        // a configuração de A está intacta
        assertAthleteAsResultTypesAreIntact();
    }

    @Test
    void shouldKeepResultTypesPerAthleteWhenBothPracticeTheSameModality() throws Exception {
        mockMvc.perform(post("/api/practiced-modalities").header("Authorization", "Bearer " + tokenB)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new AddPracticedModalityRequest(ipsc)))).andExpect(status().isCreated());

        // B tem só a sugestão padrão: o tipo que só A configurou não aparece para ele nem pode ser removido por ele
        mockMvc.perform(get("/api/practiced-modalities/" + ipsc + "/result-types").header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.id=='" + custom + "')]").isEmpty())
                .andExpect(jsonPath("$.data[?(@.id=='" + tempo + "')]").isNotEmpty());
        mockMvc.perform(delete("/api/practiced-modalities/" + ipsc + "/result-types/" + custom).header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.error.code").value("RESULT_TYPE_NOT_CONFIGURED"));

        // B remove o PRÓPRIO "Tempo": o de A continua
        mockMvc.perform(delete("/api/practiced-modalities/" + ipsc + "/result-types/" + tempo).header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk());
        assertAthleteAsResultTypesAreIntact();
    }

    private void assertAthleteAsResultTypesAreIntact() throws Exception {
        mockMvc.perform(get("/api/practiced-modalities/" + ipsc + "/result-types").header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.id=='" + tempo + "')]").isNotEmpty())
                .andExpect(jsonPath("$.data[?(@.id=='" + custom + "')]").isNotEmpty());
    }

    // ---- helpers ----

    private ResultActions send(MockHttpServletRequestBuilder builder, String token, Object payload) throws Exception {
        return mockMvc.perform(builder.header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)));
    }

    private ResultActions associate(UUID accessoryId, UUID weaponId, String token) throws Exception {
        return send(post("/api/accessories/" + accessoryId + "/weapons"), token, new AssociateAccessoryWeaponRequest(weaponId));
    }

    private ResultActions disassociate(UUID accessoryId, UUID weaponId, String token) throws Exception {
        return mockMvc.perform(delete("/api/accessories/" + accessoryId + "/weapons/" + weaponId)
                .header("Authorization", "Bearer " + token));
    }

    private ResultActions evolution(String token, UUID modalityId, UUID resultTypeId) throws Exception {
        return mockMvc.perform(get("/api/dashboard/evolution")
                .header("Authorization", "Bearer " + token)
                .param("modalityId", modalityId.toString())
                .param("resultTypeId", resultTypeId.toString())
                .param("period", "30d")
                .param("mode", "MEDIA"));
    }

    private UUID createdId(MockHttpServletRequestBuilder builder, String token, Object payload) throws Exception {
        String json = body(send(builder, token, payload).andExpect(status().isCreated()));
        return UUID.fromString(objectMapper.readTree(json).get("data").get("id").asText());
    }

    private UUID catalogId(String token, String path, String name) throws Exception {
        JsonNode items = objectMapper.readTree(body(mockMvc.perform(get(path).header("Authorization", "Bearer " + token))))
                .get("data");
        for (JsonNode item : items) {
            if (item.get("name").asText().equals(name)) {
                return UUID.fromString(item.get("id").asText());
            }
        }
        throw new IllegalStateException(name + " não existe em " + path);
    }

    private String body(ResultActions result) throws Exception {
        return result.andReturn().getResponse().getContentAsString();
    }
}
