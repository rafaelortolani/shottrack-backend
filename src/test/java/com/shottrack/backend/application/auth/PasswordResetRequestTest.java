package com.shottrack.backend.application.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shottrack.backend.application.auth.dto.PasswordResetConfirmRequest;
import com.shottrack.backend.application.auth.dto.PasswordResetRequest;
import com.shottrack.backend.application.auth.gateway.repository.PasswordResetTokenRepository;
import com.shottrack.backend.application.auth.model.PasswordResetToken;
import com.shottrack.backend.application.user.gateway.repository.PendingRegistrationRepository;
import com.shottrack.backend.application.user.gateway.repository.UserRepository;
import com.shottrack.backend.common.security.TokenHasher;
import com.shottrack.backend.support.TestUsers;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PasswordResetRequestTest {

    private static final String PASSWORD = "senha12345";
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

    @Test
    void shouldSendEmailWithLinkOnOwnLineAndStoreOnlyTheHash() throws Exception {
        String email = "atleta.esqueci@shottrack.com";
        UUID userId = createUser(email);

        requestReset(email).andExpect(status().isAccepted());

        SimpleMailMessage mail = sentMail();
        assertThat(mail.getTo()).containsExactly(email);
        assertThat(mail.getSubject()).isEqualTo("Redefinição de senha - ShotTrack");
        String token = extractToken(mail);

        List<PasswordResetToken> stored = passwordResetTokenRepository.findAllByUserIdAndUsedAtIsNull(userId);
        assertThat(stored).hasSize(1);
        PasswordResetToken saved = stored.get(0);
        assertThat(saved.getTokenHash()).isEqualTo(TokenHasher.sha256Hex(token)).isNotEqualTo(token);
        assertThat(saved.getExpiresAt())
                .isBetween(Instant.now().plus(Duration.ofMinutes(59)), Instant.now().plus(Duration.ofMinutes(61)));
    }

    @Test
    void shouldSendEmailInEnglishWhenRequested() throws Exception {
        String email = "athlete.forgot@shottrack.com";
        createUser(email);

        mockMvc.perform(post("/api/auth/password-reset/request")
                        .header("Accept-Language", "en")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new PasswordResetRequest(email))))
                .andExpect(status().isAccepted());

        SimpleMailMessage mail = sentMail();
        assertThat(mail.getSubject()).isEqualTo("Password reset - ShotTrack");
        assertThat(mail.getText()).contains("We received a request to reset your account password");
        assertThat(extractToken(mail)).isNotBlank();
    }

    @Test
    void shouldReturnSameResponseForUnknownEmailAndSendNothing() throws Exception {
        createUser("atleta.conhecido@shottrack.com");

        String known = requestReset("atleta.conhecido@shottrack.com")
                .andExpect(status().isAccepted())
                .andReturn().getResponse().getContentAsString();
        String unknown = requestReset("ninguem@shottrack.com")
                .andExpect(status().isAccepted())
                .andReturn().getResponse().getContentAsString();

        assertThat(unknown).isEqualTo(known);
        verify(mailSender, org.mockito.Mockito.times(1)).send(any(SimpleMailMessage.class));
    }

    @Test
    void shouldInvalidatePreviousTokenOnNewRequest() throws Exception {
        String email = "atleta.doispedidos@shottrack.com";
        UUID userId = createUser(email);

        requestReset(email).andExpect(status().isAccepted());
        String firstToken = extractToken(sentMail());

        requestReset(email).andExpect(status().isAccepted());
        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender, org.mockito.Mockito.times(2)).send(captor.capture());
        String secondToken = extractToken(captor.getAllValues().get(1));

        assertThat(secondToken).isNotEqualTo(firstToken);
        assertThat(passwordResetTokenRepository.findAllByUserIdAndUsedAtIsNull(userId)).hasSize(1);

        confirm(firstToken, "senhaNova123")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("PASSWORD_RESET_TOKEN_INVALID"));
        confirm(secondToken, "senhaNova123").andExpect(status().isOk());
    }

    @Test
    void shouldRejectInvalidEmailFormat() throws Exception {
        requestReset("isso-nao-e-email")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));

        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    void shouldRejectMissingEmail() throws Exception {
        mockMvc.perform(post("/api/auth/password-reset/request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    void shouldStillRespondSuccessWhenEmailSendingFails() throws Exception {
        String email = "atleta.smtpfora@shottrack.com";
        createUser(email);
        doThrow(new MailSendException("smtp fora do ar")).when(mailSender).send(any(SimpleMailMessage.class));

        requestReset(email).andExpect(status().isAccepted());
    }

    private UUID createUser(String email) throws Exception {
        TestUsers.register(mockMvc, objectMapper, pendingRegistrationRepository, "Atleta Teste", email, PASSWORD);
        org.mockito.Mockito.clearInvocations(mailSender);
        return userRepository.findByEmail(email).orElseThrow().getId();
    }

    private org.springframework.test.web.servlet.ResultActions requestReset(String email) throws Exception {
        return mockMvc.perform(post("/api/auth/password-reset/request")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new PasswordResetRequest(email))));
    }

    private org.springframework.test.web.servlet.ResultActions confirm(String token, String newPassword) throws Exception {
        return mockMvc.perform(post("/api/auth/password-reset/confirm")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new PasswordResetConfirmRequest(token, newPassword))));
    }

    private SimpleMailMessage sentMail() {
        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        return captor.getValue();
    }

    /** O link tem que estar sozinho numa linha — clientes de e-mail quebram o link se vier colado em texto. */
    private String extractToken(SimpleMailMessage mail) {
        String linkLine = mail.getText().lines()
                .filter(line -> line.startsWith(LINK_PREFIX))
                .findFirst()
                .orElseThrow(() -> new AssertionError("link em linha própria não encontrado: " + mail.getText()));
        return linkLine.substring(LINK_PREFIX.length());
    }
}
