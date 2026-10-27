package com.shottrack.backend.application.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shottrack.backend.application.auth.dto.LoginRequest;
import com.shottrack.backend.application.auth.dto.PasswordResetConfirmRequest;
import com.shottrack.backend.application.auth.dto.PasswordResetRequest;
import com.shottrack.backend.application.auth.dto.RefreshRequest;
import com.shottrack.backend.application.auth.gateway.repository.PasswordResetTokenRepository;
import com.shottrack.backend.application.auth.model.PasswordResetToken;
import com.shottrack.backend.application.user.gateway.repository.PendingRegistrationRepository;
import com.shottrack.backend.application.user.gateway.repository.UserRepository;
import com.shottrack.backend.common.security.TokenHasher;
import com.shottrack.backend.support.TestUsers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PasswordResetConfirmTest {

    private static final String EMAIL = "atleta.redefine@shottrack.com";
    private static final String PASSWORD = "senha12345";
    private static final String NEW_PASSWORD = "senhaNova123";
    private static final String LINK_PREFIX = "http://localhost:3000/redefinir-senha?token=";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PendingRegistrationRepository pendingRegistrationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @MockBean
    private JavaMailSenderImpl mailSender;

    private UUID userId;

    @BeforeEach
    void registerUser() throws Exception {
        TestUsers.register(mockMvc, objectMapper, pendingRegistrationRepository, "Atleta Teste", EMAIL, PASSWORD);
        userId = userRepository.findByEmail(EMAIL).orElseThrow().getId();
        org.mockito.Mockito.clearInvocations(mailSender);
    }

    @Test
    void shouldResetPasswordAllowingNewOneAndRejectingOldOne() throws Exception {
        String token = requestResetAndGetToken();

        confirm(token, NEW_PASSWORD).andExpect(status().isOk());

        login(EMAIL, NEW_PASSWORD).andExpect(status().isOk());
        login(EMAIL, PASSWORD)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("INVALID_CREDENTIALS"));
    }

    @Test
    void shouldRevokePreviousRefreshTokensAfterReset() throws Exception {
        String refreshToken = loginAndGetRefreshToken();
        String token = requestResetAndGetToken();

        confirm(token, NEW_PASSWORD).andExpect(status().isOk());

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RefreshRequest(refreshToken))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("INVALID_REFRESH_TOKEN"));
    }

    @Test
    void shouldRejectUnknownToken() throws Exception {
        confirm("token-que-nunca-existiu", NEW_PASSWORD)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("PASSWORD_RESET_TOKEN_INVALID"));
    }

    @Test
    void shouldRejectExpiredToken() throws Exception {
        String rawToken = "token-expirado";
        passwordResetTokenRepository.save(PasswordResetToken.builder()
                .userId(userId)
                .tokenHash(TokenHasher.sha256Hex(rawToken))
                .expiresAt(Instant.now().minus(Duration.ofMinutes(1)))
                .build());

        confirm(rawToken, NEW_PASSWORD)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("PASSWORD_RESET_TOKEN_EXPIRED"));

        login(EMAIL, PASSWORD).andExpect(status().isOk());
    }

    @Test
    void shouldRejectAlreadyUsedToken() throws Exception {
        String token = requestResetAndGetToken();
        confirm(token, NEW_PASSWORD).andExpect(status().isOk());

        confirm(token, "outraSenha456")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("PASSWORD_RESET_TOKEN_ALREADY_USED"));

        login(EMAIL, NEW_PASSWORD).andExpect(status().isOk());
    }

    @Test
    void shouldRejectTokenReplacedByNewerRequest() throws Exception {
        String firstToken = requestResetAndGetToken();
        org.mockito.Mockito.clearInvocations(mailSender);
        requestResetAndGetToken();

        confirm(firstToken, NEW_PASSWORD)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("PASSWORD_RESET_TOKEN_INVALID"));
    }

    @Test
    void shouldRejectPasswordShorterThanEightCharactersWithoutConsumingToken() throws Exception {
        String token = requestResetAndGetToken();

        confirm(token, "curta")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));

        confirm(token, NEW_PASSWORD).andExpect(status().isOk());
    }

    @Test
    void shouldRejectBlankPassword() throws Exception {
        String token = requestResetAndGetToken();

        confirm(token, "")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    void shouldRejectMissingToken() throws Exception {
        mockMvc.perform(post("/api/auth/password-reset/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"newPassword\":\"" + NEW_PASSWORD + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    void shouldTranslateErrorMessageToEnglish() throws Exception {
        mockMvc.perform(post("/api/auth/password-reset/confirm")
                        .header("Accept-Language", "en")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new PasswordResetConfirmRequest("nope", NEW_PASSWORD))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.message").value("Invalid password reset link"));
    }

    private String requestResetAndGetToken() throws Exception {
        mockMvc.perform(post("/api/auth/password-reset/request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new PasswordResetRequest(EMAIL))))
                .andExpect(status().isAccepted());

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        return captor.getValue().getText().lines()
                .filter(line -> line.startsWith(LINK_PREFIX))
                .findFirst()
                .orElseThrow()
                .substring(LINK_PREFIX.length());
    }

    private String loginAndGetRefreshToken() throws Exception {
        String body = login(EMAIL, PASSWORD).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("data").get("refreshToken").asText();
    }

    private ResultActions login(String email, String password) throws Exception {
        return mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new LoginRequest(email, password))));
    }

    private ResultActions confirm(String token, String newPassword) throws Exception {
        return mockMvc.perform(post("/api/auth/password-reset/confirm")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new PasswordResetConfirmRequest(token, newPassword))));
    }
}
