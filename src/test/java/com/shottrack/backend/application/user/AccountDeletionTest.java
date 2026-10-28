package com.shottrack.backend.application.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shottrack.backend.application.auth.dto.LoginRequest;
import com.shottrack.backend.application.auth.dto.RefreshRequest;
import com.shottrack.backend.application.dashboard.event.DashboardRecalculationRequestedEvent;
import com.shottrack.backend.application.dashboard.messaging.DashboardRecalculationListener;
import com.shottrack.backend.application.dashboard.usecase.DashboardRecalculationService;
import com.shottrack.backend.application.user.dto.DeleteAccountRequest;
import com.shottrack.backend.application.user.gateway.repository.PendingRegistrationRepository;
import com.shottrack.backend.application.user.gateway.repository.UserRepository;
import com.shottrack.backend.support.AthleteDataTestSupport;
import com.shottrack.backend.support.AthleteDataTestSupport.Athlete;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * UC52/ADR-0019. A transação do teste envolve a do serviço (TransactionTemplate
 * entra nela), então o rollback de verdade fica em AccountDeletionRollbackTest.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ExtendWith(OutputCaptureExtension.class)
class AccountDeletionTest {

    private static final String PASSWORD = AthleteDataTestSupport.PASSWORD;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PendingRegistrationRepository pendingRegistrationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private DashboardRecalculationService dashboardRecalculationService;

    @Autowired
    private DashboardRecalculationListener dashboardRecalculationListener;

    @PersistenceContext
    private EntityManager entityManager;

    @MockBean
    private JavaMailSenderImpl mailSender;

    @Test
    void shouldDeleteAllDataOfTheAccountInEveryTable() throws Exception {
        Athlete athlete = populatedAthlete("atleta.excluir@shottrack.com");
        Map<String, Long> before = counts(athlete);
        assertThat(before).allSatisfy((table, rows) -> assertThat(rows)
                .as("a tabela %s precisa ter dados antes da exclusão", table).isPositive());

        deleteAccount(athlete, PASSWORD).andExpect(status().isOk());

        assertThat(counts(athlete)).allSatisfy((table, rows) -> assertThat(rows)
                .as("a tabela %s deveria estar vazia depois da exclusão", table).isZero());
    }

    @Test
    void shouldKeepAnotherAthletesDataIntact() throws Exception {
        Athlete deleted = populatedAthlete("atleta.excluido@shottrack.com");
        Athlete other = populatedAthlete("atleta.intacto@shottrack.com");
        Map<String, Long> otherBefore = counts(other);

        deleteAccount(deleted, PASSWORD).andExpect(status().isOk());

        assertThat(counts(other)).isEqualTo(otherBefore);
        login(other.email(), PASSWORD).andExpect(status().isOk());
    }

    @Test
    void shouldRejectLoginAndRefreshAfterDeletion() throws Exception {
        Athlete athlete = populatedAthlete("atleta.sessoes@shottrack.com");

        deleteAccount(athlete, PASSWORD).andExpect(status().isOk());

        login(athlete.email(), PASSWORD)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("INVALID_CREDENTIALS"));
        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RefreshRequest(athlete.refreshToken()))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("INVALID_REFRESH_TOKEN"));
    }

    @Test
    void shouldRemovePendingRegistrationOfTheSameEmail() throws Exception {
        Athlete athlete = populatedAthlete("atleta.pendente@shottrack.com");
        assertThat(pendingRegistrationRepository.findAllByEmailAndUsedFalse(athlete.email())).isNotEmpty();

        deleteAccount(athlete, PASSWORD).andExpect(status().isOk());

        entityManager.flush();
        entityManager.clear();
        assertThat(pendingRegistrationRepository.findAll())
                .noneMatch(pending -> pending.getEmail().equals(athlete.email()));
    }

    @Test
    void shouldRejectWrongPasswordAndDeleteNothing() throws Exception {
        Athlete athlete = populatedAthlete("atleta.senhaerrada@shottrack.com");
        Map<String, Long> before = counts(athlete);
        clearInvocations(mailSender);

        deleteAccount(athlete, "senhaErrada123")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("INVALID_CURRENT_PASSWORD"));

        assertThat(counts(athlete)).isEqualTo(before);
        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    void shouldRejectMissingPassword() throws Exception {
        Athlete athlete = populatedAthlete("atleta.semsenha@shottrack.com");

        mockMvc.perform(post("/api/users/me/deletion")
                        .header("Authorization", "Bearer " + athlete.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));

        assertThat(userRepository.findByEmail(athlete.email())).isPresent();
    }

    @Test
    void shouldRejectBlankPassword() throws Exception {
        Athlete athlete = populatedAthlete("atleta.senhabranco@shottrack.com");

        deleteAccount(athlete, "")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    void shouldRejectRequestWithoutToken() throws Exception {
        mockMvc.perform(post("/api/users/me/deletion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new DeleteAccountRequest(PASSWORD))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
    }

    @Test
    void shouldDeleteAccountThatHasNoDataBesidesTheProfile() throws Exception {
        Athlete athlete = AthleteDataTestSupport.registerAthlete(mockMvc, objectMapper, pendingRegistrationRepository,
                userRepository, "atleta.vazio@shottrack.com");

        deleteAccount(athlete, PASSWORD).andExpect(status().isOk());

        assertThat(userRepository.findByEmail(athlete.email())).isEmpty();
    }

    @Test
    void shouldAllowRegisteringTheSameEmailAgainAfterDeletion() throws Exception {
        Athlete athlete = AthleteDataTestSupport.registerAthlete(mockMvc, objectMapper, pendingRegistrationRepository,
                userRepository, "atleta.volta@shottrack.com");
        deleteAccount(athlete, PASSWORD).andExpect(status().isOk());
        entityManager.flush();
        entityManager.clear();

        Athlete again = AthleteDataTestSupport.registerAthlete(mockMvc, objectMapper, pendingRegistrationRepository,
                userRepository, athlete.email());

        assertThat(again.userId()).isNotEqualTo(athlete.userId());
    }

    @Test
    void shouldSendDeletionEmailInPortugueseByDefault() throws Exception {
        Athlete athlete = populatedAthlete("atleta.avisopt@shottrack.com");
        clearInvocations(mailSender);

        deleteAccount(athlete, PASSWORD).andExpect(status().isOk());

        SimpleMailMessage mail = sentMail();
        assertThat(mail.getTo()).containsExactly(athlete.email());
        assertThat(mail.getSubject()).isEqualTo("Sua conta ShotTrack foi excluída");
        assertThat(mail.getText()).contains("excluídos de forma definitiva");
    }

    @Test
    void shouldSendDeletionEmailInEnglishWhenRequested() throws Exception {
        Athlete athlete = populatedAthlete("atleta.avisoen@shottrack.com");
        clearInvocations(mailSender);

        mockMvc.perform(post("/api/users/me/deletion")
                        .header("Authorization", "Bearer " + athlete.accessToken())
                        .header("Accept-Language", "en")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new DeleteAccountRequest(PASSWORD))))
                .andExpect(status().isOk());

        SimpleMailMessage mail = sentMail();
        assertThat(mail.getSubject()).isEqualTo("Your ShotTrack account has been deleted");
        assertThat(mail.getText()).contains("permanently deleted");
    }

    @Test
    void shouldKeepDeletionAndNotLogTheAddressWhenEmailSendingFails(CapturedOutput output) throws Exception {
        Athlete athlete = populatedAthlete("atleta.smtpfora@shottrack.com");
        doThrow(new MailSendException("Invalid Addresses: " + athlete.email()))
                .when(mailSender).send(any(SimpleMailMessage.class));

        deleteAccount(athlete, PASSWORD).andExpect(status().isOk());

        assertThat(userRepository.findByEmail(athlete.email())).isEmpty();
        assertThat(output.getAll())
                .contains("Falha ao enviar e-mail de aviso de exclusão de conta (MailSendException)")
                .doesNotContain(athlete.email());
    }

    @Test
    void shouldIgnoreDashboardRecalculationEventOfDeletedUserWithoutError() throws Exception {
        Athlete athlete = populatedAthlete("atleta.eventodashboard@shottrack.com");
        dashboardRecalculationService.recalculate(athlete.userId());
        deleteAccount(athlete, PASSWORD).andExpect(status().isOk());
        entityManager.flush();
        entityManager.clear();

        // o evento chegou da fila depois da exclusão: sem exceção (nada de retry/DLQ) e sem resumo recriado
        dashboardRecalculationListener.onMessage(new DashboardRecalculationRequestedEvent(athlete.userId()));
        entityManager.flush();

        assertThat(counts(athlete).get("dashboard_summary")).isZero();
    }

    private Athlete populatedAthlete(String email) throws Exception {
        Athlete athlete = AthleteDataTestSupport.registerAthlete(mockMvc, objectMapper, pendingRegistrationRepository,
                userRepository, email);
        AthleteDataTestSupport.populate(mockMvc, objectMapper, pendingRegistrationRepository, athlete);
        dashboardRecalculationService.recalculate(athlete.userId());
        return athlete;
    }

    private Map<String, Long> counts(Athlete athlete) {
        entityManager.flush();
        entityManager.clear();
        return AthleteDataTestSupport.countRows(jdbcTemplate, athlete);
    }

    private ResultActions deleteAccount(Athlete athlete, String password) throws Exception {
        return mockMvc.perform(post("/api/users/me/deletion")
                .header("Authorization", "Bearer " + athlete.accessToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new DeleteAccountRequest(password))));
    }

    private ResultActions login(String email, String password) throws Exception {
        return mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new LoginRequest(email, password))));
    }

    private SimpleMailMessage sentMail() {
        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        return captor.getValue();
    }
}
