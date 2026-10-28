package com.shottrack.backend.application.user;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shottrack.backend.application.user.gateway.UserGateway;
import com.shottrack.backend.application.user.gateway.repository.EmailVerificationCodeRepository;
import com.shottrack.backend.application.user.gateway.repository.PendingRegistrationRepository;
import com.shottrack.backend.application.user.gateway.repository.UserRepository;
import com.shottrack.backend.application.user.model.EmailVerificationCode;
import com.shottrack.backend.application.user.model.PendingRegistration;
import com.shottrack.backend.application.user.model.User;
import com.shottrack.backend.support.TestUsers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.MediaType;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * S02 (reautenticação na troca de e-mail, aviso ao e-mail antigo, sessões revogadas) e S22 (unicidade
 * do e-mail revalidada no cadastro e na confirmação, inclusive na corrida contra o banco) — ADR-0019/UC04/UC23.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class EmailChangeProtectionTest {

    private static final String OLD_EMAIL = "troca.protegida@shottrack.com";
    private static final String NEW_EMAIL = "novo.protegido@shottrack.com";
    private static final String PASSWORD = "senha12345";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PendingRegistrationRepository pendingRegistrationRepository;

    @Autowired
    private EmailVerificationCodeRepository emailVerificationCodeRepository;

    @Autowired
    private UserRepository userRepository;

    @MockBean
    private JavaMailSenderImpl mailSender;

    @SpyBean
    private UserGateway userGateway;

    private String accessToken;
    private String refreshToken;

    @BeforeEach
    void registerAndLogin() throws Exception {
        accessToken = TestUsers.registerAndLogin(mockMvc, objectMapper, pendingRegistrationRepository,
                "Atleta Protegido", OLD_EMAIL, PASSWORD);
        refreshToken = login(OLD_EMAIL).get("refreshToken").asText();
        clearInvocations(mailSender);
    }

    private JsonNode login(String email) throws Exception {
        var result = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("email", email, "password", PASSWORD)))).andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("data");
    }

    private ResultActions requestChange(Map<String, Object> body) throws Exception {
        return mockMvc.perform(post("/api/users/me/email")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)));
    }

    private ResultActions requestChange(String newEmail, String password) throws Exception {
        return requestChange(Map.of("email", newEmail, "currentPassword", password));
    }

    private ResultActions confirm(String code) throws Exception {
        return mockMvc.perform(post("/api/users/me/email/confirmation")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("code", code))));
    }

    private String pendingCode(String newEmail) {
        return emailVerificationCodeRepository.findAll().stream()
                .filter(v -> v.getNewEmail().equals(newEmail) && !v.isUsed())
                .map(EmailVerificationCode::getCode).findFirst().orElseThrow();
    }

    private boolean hasPendingCode(String newEmail) {
        return emailVerificationCodeRepository.findAll().stream()
                .anyMatch(v -> v.getNewEmail().equals(newEmail));
    }

    private List<SimpleMailMessage> sentMessages(int expected) {
        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender, times(expected)).send(captor.capture());
        return captor.getAllValues();
    }

    private int refreshStatus(String token) throws Exception {
        return mockMvc.perform(post("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("refreshToken", token))))
                .andReturn().getResponse().getStatus();
    }

    private String currentEmail() throws Exception {
        var result = mockMvc.perform(get("/api/users/me").header("Authorization", "Bearer " + accessToken)).andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("data").get("email").asText();
    }

    // ---- S02: senha obrigatória ----

    @Test
    void shouldRejectEmailChangeWithoutPassword() throws Exception {
        for (Map<String, Object> body : List.of(Map.<String, Object>of("email", NEW_EMAIL),
                Map.<String, Object>of("email", NEW_EMAIL, "currentPassword", ""),
                Map.<String, Object>of("email", NEW_EMAIL, "currentPassword", "  "))) {
            requestChange(body)
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
        }

        assertThat(hasPendingCode(NEW_EMAIL)).isFalse();
        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    void shouldRejectEmailChangeWithWrongPassword() throws Exception {
        requestChange(NEW_EMAIL, "senhaErrada1")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("INVALID_CURRENT_PASSWORD"))
                .andExpect(jsonPath("$.error.message").value("Senha atual incorreta"));

        assertThat(hasPendingCode(NEW_EMAIL)).isFalse();
        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    void shouldCheckThePasswordBeforeRevealingThatTheEmailIsTaken() throws Exception {
        TestUsers.register(mockMvc, objectMapper, pendingRegistrationRepository, "Outro", NEW_EMAIL, PASSWORD);

        requestChange(NEW_EMAIL, "senhaErrada1")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("INVALID_CURRENT_PASSWORD"));
    }

    @Test
    void shouldAcceptEmailChangeWithCorrectPasswordAndWarnTheOldAddressWithoutTheCode() throws Exception {
        requestChange(NEW_EMAIL, PASSWORD).andExpect(status().isAccepted());

        String code = pendingCode(NEW_EMAIL);
        List<SimpleMailMessage> messages = sentMessages(2);
        SimpleMailMessage toNew = messages.stream().filter(m -> NEW_EMAIL.equals(m.getTo()[0])).findFirst().orElseThrow();
        SimpleMailMessage toOld = messages.stream().filter(m -> OLD_EMAIL.equals(m.getTo()[0])).findFirst().orElseThrow();

        assertThat(toNew.getText()).contains(code);
        assertThat(toOld.getSubject()).isEqualTo("Pedido de troca de e-mail - ShotTrack");
        assertThat(toOld.getText()).doesNotContain(code).doesNotContain(NEW_EMAIL);
    }

    @Test
    void shouldWarnTheOldAddressInEnglishWhenRequested() throws Exception {
        mockMvc.perform(post("/api/users/me/email")
                        .header("Authorization", "Bearer " + accessToken)
                        .header("Accept-Language", "en")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", NEW_EMAIL, "currentPassword", PASSWORD))))
                .andExpect(status().isAccepted());

        SimpleMailMessage toOld = sentMessages(2).stream()
                .filter(m -> OLD_EMAIL.equals(m.getTo()[0])).findFirst().orElseThrow();
        assertThat(toOld.getSubject()).isEqualTo("Email change request - ShotTrack");
    }

    @Test
    void shouldStillAcceptTheRequestWhenTheWarningEmailFails() throws Exception {
        doNothing().doThrow(new MailSendException("smtp fora do ar")).when(mailSender).send(any(SimpleMailMessage.class));

        requestChange(NEW_EMAIL, PASSWORD).andExpect(status().isAccepted());

        assertThat(hasPendingCode(NEW_EMAIL)).isTrue();
    }

    // ---- S02: confirmação revoga sessões e avisa ----

    @Test
    void shouldRevokeEveryRefreshTokenAndWarnTheOldAddressWhenTheChangeIsConfirmed() throws Exception {
        String secondSession = login(OLD_EMAIL).get("refreshToken").asText();
        requestChange(NEW_EMAIL, PASSWORD).andExpect(status().isAccepted());
        clearInvocations(mailSender);

        confirm(pendingCode(NEW_EMAIL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value(NEW_EMAIL));

        assertThat(refreshStatus(refreshToken)).isEqualTo(401);
        assertThat(refreshStatus(secondSession)).isEqualTo(401);
        SimpleMailMessage notice = sentMessages(1).get(0);
        assertThat(notice.getTo()[0]).isEqualTo(OLD_EMAIL);
        assertThat(notice.getSubject()).isEqualTo("O e-mail da sua conta ShotTrack foi alterado");
        assertThat(notice.getText()).doesNotContain(NEW_EMAIL);
    }

    @Test
    void shouldKeepSessionsAndSendNothingWhenTheCodeIsWrong() throws Exception {
        requestChange(NEW_EMAIL, PASSWORD).andExpect(status().isAccepted());
        clearInvocations(mailSender);

        confirm("000000").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_VERIFICATION_CODE"));

        verify(mailSender, never()).send(any(SimpleMailMessage.class));
        assertThat(refreshStatus(refreshToken)).isEqualTo(200);
        assertThat(currentEmail()).isEqualTo(OLD_EMAIL);
    }

    // ---- S22: unicidade ----

    @Test
    void shouldAnswerConflictWhenTheNewEmailWasTakenBetweenRequestAndConfirmation() throws Exception {
        requestChange(NEW_EMAIL, PASSWORD).andExpect(status().isAccepted());
        String code = pendingCode(NEW_EMAIL);
        TestUsers.register(mockMvc, objectMapper, pendingRegistrationRepository, "Quem chegou antes", NEW_EMAIL, PASSWORD);
        clearInvocations(mailSender);

        confirm(code)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("EMAIL_ALREADY_REGISTERED"));

        assertThat(currentEmail()).isEqualTo(OLD_EMAIL);
        assertThat(refreshStatus(refreshToken)).isEqualTo(200);
        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    void shouldAnswerConflictInsteadOf500WhenTheDatabaseUniquenessRaceHappensOnConfirmation() throws Exception {
        requestChange(NEW_EMAIL, PASSWORD).andExpect(status().isAccepted());
        String code = pendingCode(NEW_EMAIL);
        TestUsers.register(mockMvc, objectMapper, pendingRegistrationRepository, "Quem chegou antes", NEW_EMAIL, PASSWORD);
        // a checagem prévia "não vê" o outro usuário (corrida): quem barra é a restrição única do banco
        doReturn(false).when(userGateway).existsByEmail(NEW_EMAIL);

        confirm(code)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("EMAIL_ALREADY_REGISTERED"));
    }

    @Test
    void shouldAnswerConflictWhenTheEmailWasTakenBeforeCompletingTheRegistration() throws Exception {
        String email = "corrida.cadastro@shottrack.com";
        String token = pendingTokenFor(email);
        takeEmailDirectly(email);

        complete(token)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("EMAIL_ALREADY_REGISTERED"));
    }

    @Test
    void shouldAnswerConflictInsteadOf500WhenTheDatabaseUniquenessRaceHappensOnRegistration() throws Exception {
        String email = "corrida.cadastro.banco@shottrack.com";
        String token = pendingTokenFor(email);
        takeEmailDirectly(email);
        doReturn(false).when(userGateway).existsByEmail(email);

        complete(token)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("EMAIL_ALREADY_REGISTERED"));
    }

    @Test
    void shouldKeepTheRegistrationLinkUsableWhenTheEmailIsTakenSoNothingIsBurned() throws Exception {
        String email = "link.preservado@shottrack.com";
        String token = pendingTokenFor(email);
        takeEmailDirectly(email);

        complete(token).andExpect(status().isConflict());

        PendingRegistration pending = pendingRegistrationRepository.findAllByEmailAndUsedFalse(email).get(0);
        assertThat(pending.isUsed()).isFalse();
    }

    private String pendingTokenFor(String email) throws Exception {
        mockMvc.perform(post("/api/users/registration")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("email", email))));
        return pendingRegistrationRepository.findAllByEmailAndUsedFalse(email).get(0).getToken();
    }

    private void takeEmailDirectly(String email) {
        userRepository.saveAndFlush(User.builder().name("Dono do e-mail").email(email)
                .passwordHash("hash-qualquer").build());
    }

    private ResultActions complete(String token) throws Exception {
        return mockMvc.perform(post("/api/users/registration/completion")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("token", token, "name", "Atleta",
                        "password", PASSWORD, "acceptedTerms", true))));
    }
}
