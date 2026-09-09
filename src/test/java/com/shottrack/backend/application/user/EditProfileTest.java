package com.shottrack.backend.application.user;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shottrack.backend.application.auth.dto.LoginRequest;
import com.shottrack.backend.application.user.dto.ChangeEmailRequest;
import com.shottrack.backend.application.user.dto.ConfirmEmailChangeRequest;
import com.shottrack.backend.application.user.dto.UpdateProfileRequest;
import com.shottrack.backend.application.user.dto.UserRegisterRequest;
import com.shottrack.backend.application.user.gateway.repository.EmailVerificationCodeRepository;
import com.shottrack.backend.application.user.model.EmailVerificationCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class EditProfileTest {

    private static final String NAME = "Atleta Perfil";
    private static final String EMAIL = "atleta.editar@shottrack.com";
    private static final String PASSWORD = "senha12345";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private EmailVerificationCodeRepository emailVerificationCodeRepository;

    private String accessToken;

    @BeforeEach
    void registerAndLoginUser() throws Exception {
        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new UserRegisterRequest(NAME, EMAIL, PASSWORD))));

        var result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(EMAIL, PASSWORD))))
                .andReturn();

        JsonNode data = objectMapper.readTree(result.getResponse().getContentAsString()).get("data");
        accessToken = data.get("accessToken").asText();
    }

    @Test
    void shouldEditNameAndExperienceLevel() throws Exception {
        mockMvc.perform(patch("/api/users/me")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateProfileRequest("Novo Nome", "ADVANCED"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Novo Nome"))
                .andExpect(jsonPath("$.data.experienceLevel").value("ADVANCED"));
    }

    @Test
    void shouldRejectEmptyName() throws Exception {
        mockMvc.perform(patch("/api/users/me")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateProfileRequest("", "BEGINNER"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    void shouldRejectInvalidExperienceLevel() throws Exception {
        mockMvc.perform(patch("/api/users/me")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateProfileRequest("Novo Nome", "EXPERT"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    void shouldRejectRequestWithoutToken() throws Exception {
        mockMvc.perform(patch("/api/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateProfileRequest("Novo Nome", "BEGINNER"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
    }

    @Test
    void shouldChangeEmailAfterConfirmingCode() throws Exception {
        String newEmail = "novo.email@shottrack.com";

        mockMvc.perform(post("/api/users/me/email")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ChangeEmailRequest(newEmail))))
                .andExpect(status().isAccepted());

        String codigo = pendingCodeFor(newEmail).getCode();

        mockMvc.perform(post("/api/users/me/email/confirmation")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ConfirmEmailChangeRequest(codigo))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value(newEmail));
    }

    @Test
    void shouldRejectChangeToAlreadyRegisteredEmail() throws Exception {
        String emailExistente = "outro.atleta@shottrack.com";
        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new UserRegisterRequest("Outro Atleta", emailExistente, PASSWORD))));

        mockMvc.perform(post("/api/users/me/email")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ChangeEmailRequest(emailExistente))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("EMAIL_ALREADY_REGISTERED"));
    }

    @Test
    void shouldRejectConfirmationWithoutToken() throws Exception {
        mockMvc.perform(post("/api/users/me/email/confirmation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ConfirmEmailChangeRequest("123456"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
    }

    @Test
    void shouldRejectIncorrectCode() throws Exception {
        String newEmail = "codigo.incorreto@shottrack.com";
        mockMvc.perform(post("/api/users/me/email")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ChangeEmailRequest(newEmail))))
                .andExpect(status().isAccepted());

        mockMvc.perform(post("/api/users/me/email/confirmation")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ConfirmEmailChangeRequest("000000"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_VERIFICATION_CODE"));
    }

    @Test
    void shouldRejectExpiredCode() throws Exception {
        String newEmail = "codigo.expirado@shottrack.com";
        mockMvc.perform(post("/api/users/me/email")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ChangeEmailRequest(newEmail))))
                .andExpect(status().isAccepted());

        EmailVerificationCode pendente = pendingCodeFor(newEmail);
        ReflectionTestUtils.setField(pendente, "expiresAt", Instant.now().minusSeconds(1));
        emailVerificationCodeRepository.saveAndFlush(pendente);

        mockMvc.perform(post("/api/users/me/email/confirmation")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ConfirmEmailChangeRequest(pendente.getCode()))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VERIFICATION_CODE_EXPIRED"));
    }

    private EmailVerificationCode pendingCodeFor(String newEmail) {
        return emailVerificationCodeRepository.findAll().stream()
                .filter(v -> v.getNewEmail().equals(newEmail) && !v.isUsed())
                .findFirst()
                .orElseThrow();
    }
}
