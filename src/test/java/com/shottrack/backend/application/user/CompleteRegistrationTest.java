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
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CompleteRegistrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PendingRegistrationRepository pendingRegistrationRepository;

    @Test
    void shouldCompleteRegistrationWithValidToken() throws Exception {
        String email = "atleta.completa@shottrack.com";
        String token = requestRegistrationAndGetToken(email);

        mockMvc.perform(post("/api/users/registration/completion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CompleteRegistrationRequest(token, "Atleta Completa", "senha12345"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.name").value("Atleta Completa"))
                .andExpect(jsonPath("$.data.email").value(email))
                .andExpect(jsonPath("$.data.passwordHash").doesNotExist());
    }

    @Test
    void shouldRejectNonExistentToken() throws Exception {
        mockMvc.perform(post("/api/users/registration/completion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CompleteRegistrationRequest("token-inexistente", "Atleta", "senha12345"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("REGISTRATION_TOKEN_INVALID"));
    }

    @Test
    void shouldRejectExpiredToken() throws Exception {
        String email = "atleta.expirado@shottrack.com";
        requestRegistrationAndGetToken(email);

        PendingRegistration pending = pendingFor(email);
        ReflectionTestUtils.setField(pending, "expiresAt", Instant.now().minusSeconds(1));
        pendingRegistrationRepository.saveAndFlush(pending);

        mockMvc.perform(post("/api/users/registration/completion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CompleteRegistrationRequest(pending.getToken(), "Atleta Expirado", "senha12345"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("REGISTRATION_TOKEN_EXPIRED"));
    }

    @Test
    void shouldRejectAlreadyUsedToken() throws Exception {
        String email = "atleta.usado@shottrack.com";
        String token = requestRegistrationAndGetToken(email);

        mockMvc.perform(post("/api/users/registration/completion")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                        new CompleteRegistrationRequest(token, "Atleta Usado", "senha12345"))));

        mockMvc.perform(post("/api/users/registration/completion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CompleteRegistrationRequest(token, "Atleta Usado", "senha12345"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("REGISTRATION_TOKEN_ALREADY_USED"));
    }

    @Test
    void shouldRejectEmptyName() throws Exception {
        String token = requestRegistrationAndGetToken("atleta.semnome@shottrack.com");

        mockMvc.perform(post("/api/users/registration/completion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CompleteRegistrationRequest(token, "", "senha12345"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    void shouldRejectShortPassword() throws Exception {
        String token = requestRegistrationAndGetToken("atleta.senhacurta@shottrack.com");

        mockMvc.perform(post("/api/users/registration/completion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CompleteRegistrationRequest(token, "Atleta Senha Curta", "123"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    private String requestRegistrationAndGetToken(String email) throws Exception {
        mockMvc.perform(post("/api/users/registration")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new RegistrationRequest(email))));

        return pendingFor(email).getToken();
    }

    private PendingRegistration pendingFor(String email) {
        return pendingRegistrationRepository.findAllByEmailAndUsedFalse(email).stream()
                .findFirst()
                .orElseThrow();
    }
}
