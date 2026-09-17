package com.shottrack.backend.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shottrack.backend.application.auth.dto.LoginRequest;
import com.shottrack.backend.application.user.dto.CompleteRegistrationRequest;
import com.shottrack.backend.application.user.dto.RegistrationRequest;
import com.shottrack.backend.application.user.gateway.repository.PendingRegistrationRepository;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * Cadastro (UC01+UC23/ADR-0009) só existe em duas chamadas — helper pra não
 * duplicar essa dança em todo teste que só precisa de um usuário pronto pra
 * testar outra coisa.
 */
public final class TestUsers {

    private TestUsers() {
    }

    public static void register(MockMvc mockMvc, ObjectMapper objectMapper,
                                 PendingRegistrationRepository pendingRegistrationRepository,
                                 String name, String email, String password) throws Exception {
        mockMvc.perform(post("/api/users/registration")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new RegistrationRequest(email))));

        String token = pendingRegistrationRepository.findAllByEmailAndUsedFalse(email).stream()
                .findFirst()
                .orElseThrow()
                .getToken();

        mockMvc.perform(post("/api/users/registration/completion")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new CompleteRegistrationRequest(token, name, password))));
    }

    public static String registerAndLogin(MockMvc mockMvc, ObjectMapper objectMapper,
                                           PendingRegistrationRepository pendingRegistrationRepository,
                                           String name, String email, String password) throws Exception {
        register(mockMvc, objectMapper, pendingRegistrationRepository, name, email, password);

        var result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(email, password))))
                .andReturn();

        JsonNode data = objectMapper.readTree(result.getResponse().getContentAsString()).get("data");
        return data.get("accessToken").asText();
    }
}
