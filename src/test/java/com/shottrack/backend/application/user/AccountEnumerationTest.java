package com.shottrack.backend.application.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shottrack.backend.application.user.dto.ChangeEmailRequest;
import com.shottrack.backend.application.user.gateway.repository.EmailVerificationCodeRepository;
import com.shottrack.backend.application.user.gateway.repository.PendingRegistrationRepository;
import com.shottrack.backend.application.user.gateway.repository.UserRepository;
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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Lote 6 — S05 (a conta não pode ser descoberta pelas respostas) e S15 (e-mail único sem diferenciar caixa):
 * cadastro e troca de e-mail respondem igual exista a conta ou não, o login gasta o mesmo BCrypt com e-mail
 * inexistente, e o e-mail é gravado/comparado em minúsculas.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AccountEnumerationTest {

    private static final String PASSWORD = "senha12345";
    private static final String OWNER = "dono.da.conta@shottrack.com";

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
    private PasswordEncoder passwordEncoder;

    private String ownerToken;

    @BeforeEach
    void createOwner() throws Exception {
        ownerToken = TestUsers.registerAndLogin(mockMvc, objectMapper, pendingRegistrationRepository, "Dono", OWNER, PASSWORD);
        clearInvocations(mailSender, passwordEncoder);
    }

    private ResultActions requestRegistration(String rawEmail, String language) throws Exception {
        var request = post("/api/users/registration").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + rawEmail + "\"}");
        if (language != null) {
            request.header("Accept-Language", language);
        }
        return mockMvc.perform(request);
    }

    private List<SimpleMailMessage> sentMails() {
        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender, org.mockito.Mockito.atLeast(0)).send(captor.capture());
        return captor.getAllValues();
    }

    private String body(ResultActions result) throws Exception {
        return result.andReturn().getResponse().getContentAsString();
    }

    // ---- S05 (a): cadastro ----

    @Test
    void shouldAnswerTheSameForAnExistingAccountAndANewEmail() throws Exception {
        String existing = body(requestRegistration(OWNER, null).andExpect(status().isAccepted()));
        String brandNew = body(requestRegistration("email.novo@shottrack.com", null).andExpect(status().isAccepted()));

        assertThat(existing).isEqualTo(brandNew).isEqualTo("{\"data\":null,\"error\":null}");
    }

    @Test
    void shouldWarnTheOwnerWithoutALinkAndCreateNoPendingRegistration() throws Exception {
        requestRegistration(OWNER, null).andExpect(status().isAccepted());

        assertThat(pendingRegistrationRepository.findAllByEmailAndUsedFalse(OWNER)).isEmpty();
        List<SimpleMailMessage> mails = sentMails();
        assertThat(mails).hasSize(1);
        SimpleMailMessage notice = mails.get(0);
        assertThat(notice.getTo()).containsExactly(OWNER);
        assertThat(notice.getSubject()).isEqualTo("Tentativa de cadastro com o seu e-mail - ShotTrack");
        assertThat(notice.getText()).contains("Alguém tentou criar uma conta").doesNotContain("token").doesNotContain("http");
    }

    @Test
    void shouldTranslateTheOwnerWarningToEnglish() throws Exception {
        requestRegistration(OWNER, "en").andExpect(status().isAccepted());

        SimpleMailMessage notice = sentMails().get(0);
        assertThat(notice.getSubject()).isEqualTo("Sign-up attempt with your email - ShotTrack");
        assertThat(notice.getText()).contains("Someone tried to create a ShotTrack account");
    }

    @Test
    void shouldStillSendTheRegistrationLinkToANewEmail() throws Exception {
        requestRegistration("email.novo@shottrack.com", null).andExpect(status().isAccepted());

        assertThat(pendingRegistrationRepository.findAllByEmailAndUsedFalse("email.novo@shottrack.com")).hasSize(1);
        assertThat(sentMails().get(0).getText()).contains("/cadastro/completar?token=");
    }

    // ---- S05 (b): login com tempo igualado ----

    @Test
    void shouldRunABcryptComparisonWhenTheEmailDoesNotExist() throws Exception {
        clearInvocations(passwordEncoder);

        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"nao.existe@shottrack.com\",\"password\":\"qualquer-senha\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("INVALID_CREDENTIALS"));

        verify(passwordEncoder, times(1)).matches(eq("qualquer-senha"), argThat(hash -> hash != null && hash.startsWith("$2")));
    }

    @Test
    void shouldRunExactlyOneBcryptComparisonAlsoForAWrongPassword() throws Exception {
        clearInvocations(passwordEncoder);

        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + OWNER + "\",\"password\":\"senha-errada\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("INVALID_CREDENTIALS"));

        verify(passwordEncoder, times(1)).matches(eq("senha-errada"), any());
    }

    // ---- S05 (c): troca de e-mail ----

    @Test
    void shouldAnswerTheSameWhenChangingToATakenEmailAndToAFreeOne() throws Exception {
        String taken = "outra.conta@shottrack.com";
        TestUsers.register(mockMvc, objectMapper, pendingRegistrationRepository, "Outra", taken, PASSWORD);
        clearInvocations(mailSender);

        String forTaken = body(changeEmail(taken).andExpect(status().isAccepted()));
        String forFree = body(changeEmail("email.livre@shottrack.com").andExpect(status().isAccepted()));

        assertThat(forTaken).isEqualTo(forFree).isEqualTo("{\"data\":null,\"error\":null}");
    }

    @Test
    void shouldNotGenerateOrSendACodeForAnEmailThatBelongsToAnotherAccount() throws Exception {
        String taken = "outra.conta@shottrack.com";
        TestUsers.register(mockMvc, objectMapper, pendingRegistrationRepository, "Outra", taken, PASSWORD);
        clearInvocations(mailSender);

        changeEmail(taken).andExpect(status().isAccepted());

        assertThat(emailVerificationCodeRepository.findAll()).noneMatch(code -> taken.equals(code.getNewEmail()));
        assertThat(sentMails()).noneMatch(mail -> List.of(mail.getTo()).contains(taken));
        // o dono da conta que pediu a troca continua sendo avisado no e-mail atual
        assertThat(sentMails()).anyMatch(mail -> List.of(mail.getTo()).contains(OWNER));
    }

    private ResultActions changeEmail(String newEmail) throws Exception {
        return mockMvc.perform(post("/api/users/me/email")
                .header("Authorization", "Bearer " + ownerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new ChangeEmailRequest(newEmail, PASSWORD))));
    }

    // ---- S15: e-mail em minúsculas e único por lower(email) ----

    @Test
    void shouldStoreAndMatchTheEmailInLowercaseRegardlessOfTheCaseSentByTheClient() throws Exception {
        requestRegistration("  Maria.Tiro@Exemplo.COM ", null).andExpect(status().isAccepted());

        var pending = pendingRegistrationRepository.findAllByEmailAndUsedFalse("maria.tiro@exemplo.com");
        assertThat(pending).hasSize(1);
        mockMvc.perform(post("/api/users/registration/completion").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"" + pending.get(0).getToken() + "\",\"name\":\"Maria\",\"password\":\"" + PASSWORD
                                + "\",\"acceptedTerms\":true}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.email").value("maria.tiro@exemplo.com"));

        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\" MARIA.TIRO@exemplo.com\",\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").exists());
    }

    @Test
    void shouldTreatACaseVariantOfAnExistingEmailAsTheSameAccount() throws Exception {
        requestRegistration(OWNER.toUpperCase(), null).andExpect(status().isAccepted());

        assertThat(pendingRegistrationRepository.findAllByEmailAndUsedFalse(OWNER)).isEmpty();
        assertThat(pendingRegistrationRepository.findAllByEmailAndUsedFalse(OWNER.toUpperCase())).isEmpty();
        assertThat(sentMails().get(0).getTo()).containsExactly(OWNER);
    }

    @Test
    void shouldFindTheAccountOnPasswordResetWithAnyCase() throws Exception {
        mockMvc.perform(post("/api/auth/password-reset/request").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + OWNER.toUpperCase() + "\"}"))
                .andExpect(status().isAccepted());

        assertThat(sentMails()).anyMatch(mail -> List.of(mail.getTo()).contains(OWNER)
                && mail.getSubject().startsWith("Redefinição de senha"));
    }

    @Test
    void shouldStoreTheNewEmailOfAChangeInLowercase() throws Exception {
        mockMvc.perform(post("/api/users/me/email")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\" Novo.Endereco@Exemplo.COM \",\"currentPassword\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isAccepted());

        assertThat(emailVerificationCodeRepository.findAll())
                .anyMatch(code -> "novo.endereco@exemplo.com".equals(code.getNewEmail()));
    }

    @Test
    void shouldLetTheDatabaseRefuseACaseVariantDuplicate() {
        User variant = User.builder().name("Variante").email(OWNER.toUpperCase()).passwordHash("hash")
                .termsAcceptedAt(Instant.now()).termsVersion("2026-10").build();

        assertThatThrownBy(() -> userRepository.saveAndFlush(variant)).isInstanceOf(DataIntegrityViolationException.class);
    }
}
