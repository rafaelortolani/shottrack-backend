package com.shottrack.backend.application.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shottrack.backend.application.user.dto.CompleteRegistrationRequest;
import com.shottrack.backend.application.user.dto.RegistrationRequest;
import com.shottrack.backend.application.user.gateway.repository.PendingRegistrationRepository;
import com.shottrack.backend.application.user.model.PendingRegistration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class UserRegistrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PendingRegistrationRepository pendingRegistrationRepository;

    @Test
    void shouldRequestRegistrationWithValidEmail() throws Exception {
        String email = "atleta@shottrack.com";

        mockMvc.perform(post("/api/users/registration")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RegistrationRequest(email))))
                .andExpect(status().isAccepted());

        assertThat(pendingRegistrationRepository.findAllByEmailAndUsedFalse(email))
                .as("cadastro pendente com token único precisa existir pra UC23 confirmar depois")
                .hasSize(1);
    }

    @Test
    void shouldRejectInvalidEmail() throws Exception {
        mockMvc.perform(post("/api/users/registration")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RegistrationRequest("email-invalido"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    void shouldRejectEmailWithActiveAccount() throws Exception {
        String email = "atleta.ativo@shottrack.com";
        completeRegistration(email, "Atleta Ativo", "senha12345");

        mockMvc.perform(post("/api/users/registration")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RegistrationRequest(email))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("EMAIL_ALREADY_REGISTERED"));
    }

    @Test
    void shouldInvalidatePreviousTokenOnResend() throws Exception {
        String email = "atleta.reenvio@shottrack.com";

        mockMvc.perform(post("/api/users/registration")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new RegistrationRequest(email))));

        PendingRegistration firstPending = pendingFor(email);

        mockMvc.perform(post("/api/users/registration")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new RegistrationRequest(email))));

        PendingRegistration secondPending = pendingFor(email);

        assertThat(pendingRegistrationRepository.findById(firstPending.getId()).orElseThrow().isUsed())
                .as("token anterior (ainda não usado) precisa ser invalidado no reenvio")
                .isTrue();
        assertThat(secondPending.getToken()).isNotEqualTo(firstPending.getToken());

        mockMvc.perform(post("/api/users/registration/completion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CompleteRegistrationRequest(firstPending.getToken(), "Atleta Reenvio", "senha12345"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("REGISTRATION_TOKEN_ALREADY_USED"));
    }

    private void completeRegistration(String email, String name, String password) throws Exception {
        mockMvc.perform(post("/api/users/registration")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new RegistrationRequest(email))));

        String token = pendingFor(email).getToken();

        mockMvc.perform(post("/api/users/registration/completion")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new CompleteRegistrationRequest(token, name, password))));
    }

    private PendingRegistration pendingFor(String email) {
        return pendingRegistrationRepository.findAllByEmailAndUsedFalse(email).stream()
                .findFirst()
                .orElseThrow();
    }
}
