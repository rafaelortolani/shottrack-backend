package com.shottrack.backend.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shottrack.backend.application.ammunition.dto.AmmunitionRegisterRequest;
import com.shottrack.backend.application.modality.dto.AddPracticedModalityRequest;
import com.shottrack.backend.application.traininglocation.dto.TrainingLocationRegisterRequest;
import com.shottrack.backend.application.visit.dto.OpenTrainingRequest;
import com.shottrack.backend.application.visit.dto.StartVisitRequest;
import com.shottrack.backend.application.weapon.dto.WeaponRegisterRequest;
import com.shottrack.backend.application.weapon.dto.WeaponUpdateRequest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * Cadeia Visita -> Treino (UC31/UC32) e Arma/Munição (UC06/UC13) só existe
 * em várias chamadas — helper pra não duplicar isso em todo teste de Série
 * que só precisa de um treino aberto/arma/munição prontos.
 */
public final class SeriesTestSupport {

    private SeriesTestSupport() {
    }

    public static UUID openTraining(MockMvc mockMvc, ObjectMapper objectMapper, String token, String modalityName) throws Exception {
        UUID visitId = openVisit(mockMvc, objectMapper, token);
        return openTrainingInVisit(mockMvc, objectMapper, token, visitId, modalityName);
    }

    public static UUID openVisit(MockMvc mockMvc, ObjectMapper objectMapper, String token) throws Exception {
        var locationResult = mockMvc.perform(post("/api/training-locations")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new TrainingLocationRegisterRequest("Clube de Tiro Central", "São Paulo", "SP"))))
                .andReturn();
        UUID trainingLocationId = idFromResponse(objectMapper, locationResult);

        var visitResult = mockMvc.perform(post("/api/visits")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new StartVisitRequest(trainingLocationId, null))))
                .andReturn();
        return idFromResponse(objectMapper, visitResult);
    }

    public static UUID openTrainingInVisit(MockMvc mockMvc, ObjectMapper objectMapper, String token, UUID visitId,
                                           String modalityName) throws Exception {
        UUID modalityId = modalityIdByName(mockMvc, objectMapper, token, modalityName);

        mockMvc.perform(post("/api/practiced-modalities")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new AddPracticedModalityRequest(modalityId))));

        var trainingResult = mockMvc.perform(post("/api/trainings")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new OpenTrainingRequest(visitId, modalityId))))
                .andReturn();
        return idFromResponse(objectMapper, trainingResult);
    }

    public static UUID registerWeapon(MockMvc mockMvc, ObjectMapper objectMapper, String token) throws Exception {
        return registerWeapon(mockMvc, objectMapper, token, null);
    }

    /**
     * Glock G17 9mm. Apelido não existe no cadastro (UC06) — quando
     * informado, é aplicado numa edição logo em seguida (UC10).
     */
    public static UUID registerWeapon(MockMvc mockMvc, ObjectMapper objectMapper, String token, String nickname) throws Exception {
        UUID brandId = idByName(mockMvc, objectMapper, token, "/api/weapon-catalog/brands", "Glock");
        UUID modelId = idByName(mockMvc, objectMapper, token, "/api/weapon-catalog/brands/" + brandId + "/models", "G17");
        UUID caliberId = idByName(mockMvc, objectMapper, token, "/api/weapon-catalog/models/" + modelId + "/calibers", "9mm");

        var result = mockMvc.perform(post("/api/weapons")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new WeaponRegisterRequest(modelId, caliberId))))
                .andReturn();
        UUID weaponId = idFromResponse(objectMapper, result);

        if (nickname != null) {
            mockMvc.perform(patch("/api/weapons/" + weaponId)
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(new WeaponUpdateRequest(modelId, caliberId, nickname))));
        }
        return weaponId;
    }

    public static UUID registerAmmunition(MockMvc mockMvc, ObjectMapper objectMapper, String token) throws Exception {
        var request = new AmmunitionRegisterRequest(null, null, "Munição de teste", null, null, null, null, null);
        var result = mockMvc.perform(post("/api/ammunitions")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andReturn();

        return idFromResponse(objectMapper, result);
    }

    private static UUID modalityIdByName(MockMvc mockMvc, ObjectMapper objectMapper, String token, String name) throws Exception {
        return idByName(mockMvc, objectMapper, token, "/api/modality-catalog", name);
    }

    private static UUID idByName(MockMvc mockMvc, ObjectMapper objectMapper, String token, String path, String name) throws Exception {
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

    private static UUID idFromResponse(ObjectMapper objectMapper, org.springframework.test.web.servlet.MvcResult result) throws Exception {
        return UUID.fromString(objectMapper.readTree(result.getResponse().getContentAsString()).get("data").get("id").asText());
    }
}
