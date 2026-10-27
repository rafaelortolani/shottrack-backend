package com.shottrack.backend.application.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shottrack.backend.application.auth.gateway.repository.PasswordResetTokenRepository;
import com.shottrack.backend.application.auth.gateway.repository.RefreshTokenRepository;
import com.shottrack.backend.application.auth.model.PasswordResetToken;
import com.shottrack.backend.application.auth.model.RefreshToken;
import com.shottrack.backend.application.user.gateway.repository.EmailVerificationCodeRepository;
import com.shottrack.backend.application.user.gateway.repository.PendingRegistrationRepository;
import com.shottrack.backend.application.user.gateway.repository.UserRepository;
import com.shottrack.backend.application.user.model.EmailVerificationCode;
import com.shottrack.backend.application.user.model.PendingRegistration;
import com.shottrack.backend.application.user.usecase.ExpiredDataCleanupScheduler;
import com.shottrack.backend.application.user.usecase.ExpiredDataCleanupService;
import com.shottrack.backend.config.SchedulingConfig;
import com.shottrack.backend.support.AthleteDataTestSupport;
import com.shottrack.backend.support.AthleteDataTestSupport.Athlete;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.ApplicationContext;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ADR-0019: limpeza diária. O método é chamado direto (o agendamento fica
 * desligado nos testes); só o que venceu há mais de 7 dias é apagado.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ExpiredDataCleanupTest {

    private static final Duration OLD = Duration.ofDays(8);
    private static final Duration RECENT = Duration.ofDays(1);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private ExpiredDataCleanupService cleanupService;

    @Autowired
    private PendingRegistrationRepository pendingRegistrationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private EmailVerificationCodeRepository emailVerificationCodeRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @MockBean
    private JavaMailSenderImpl mailSender;

    @Test
    void shouldDeleteOnlyExpiredDataOlderThanTheSevenDayMargin() throws Exception {
        Athlete athlete = AthleteDataTestSupport.registerAthlete(mockMvc, objectMapper, pendingRegistrationRepository,
                userRepository, "atleta.limpeza@shottrack.com");
        UUID userId = athlete.userId();

        PendingRegistration oldPending = pendingRegistrationRepository.save(pending("velho@shottrack.com", OLD));
        PendingRegistration recentPending = pendingRegistrationRepository.save(pending("recente@shottrack.com", RECENT));
        PendingRegistration validPending = pendingRegistrationRepository.save(pending("valido@shottrack.com", Duration.ofDays(-1)));

        PasswordResetToken oldReset = passwordResetTokenRepository.save(reset(userId, "velho", OLD));
        PasswordResetToken recentReset = passwordResetTokenRepository.save(reset(userId, "recente", RECENT));
        PasswordResetToken validReset = passwordResetTokenRepository.save(reset(userId, "valido", Duration.ofHours(-1)));

        RefreshToken oldRefresh = refreshTokenRepository.save(refresh(userId, "velho", OLD));
        RefreshToken recentRefresh = refreshTokenRepository.save(refresh(userId, "recente", RECENT));
        RefreshToken validRefresh = refreshTokenRepository.save(refresh(userId, "valido", Duration.ofDays(-1)));

        EmailVerificationCode oldCode = emailVerificationCodeRepository.save(code(userId, "111111", OLD));
        EmailVerificationCode recentCode = emailVerificationCodeRepository.save(code(userId, "222222", RECENT));
        EmailVerificationCode validCode = emailVerificationCodeRepository.save(code(userId, "333333", Duration.ofMinutes(-10)));

        cleanupService.cleanup();
        entityManager.flush();
        entityManager.clear();

        assertThat(pendingRegistrationRepository.findById(oldPending.getId())).isEmpty();
        assertThat(pendingRegistrationRepository.findById(recentPending.getId())).isPresent();
        assertThat(pendingRegistrationRepository.findById(validPending.getId())).isPresent();

        assertThat(passwordResetTokenRepository.findById(oldReset.getId())).isEmpty();
        assertThat(passwordResetTokenRepository.findById(recentReset.getId())).isPresent();
        assertThat(passwordResetTokenRepository.findById(validReset.getId())).isPresent();

        assertThat(refreshTokenRepository.findById(oldRefresh.getId())).isEmpty();
        assertThat(refreshTokenRepository.findById(recentRefresh.getId())).isPresent();
        assertThat(refreshTokenRepository.findById(validRefresh.getId())).isPresent();
        // o refresh token da sessão ativa do login continua valendo
        assertThat(refreshTokenRepository.findByToken(athlete.refreshToken())).isPresent();

        assertThat(emailVerificationCodeRepository.findById(oldCode.getId())).isEmpty();
        assertThat(emailVerificationCodeRepository.findById(recentCode.getId())).isPresent();
        assertThat(emailVerificationCodeRepository.findById(validCode.getId())).isPresent();
    }

    @Test
    void shouldDoNothingWhenThereIsNothingToClean() {
        long users = userRepository.count();

        cleanupService.cleanup();

        assertThat(userRepository.count()).isEqualTo(users);
    }

    @Test
    void shouldNotScheduleTheCleanupInTests() {
        assertThat(applicationContext.getBeansOfType(ExpiredDataCleanupScheduler.class)).isEmpty();
    }

    @Test
    void shouldScheduleTheCleanupOnlyWhenEnabledByProperty() {
        var runner = new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of())
                .withUserConfiguration(SchedulingConfig.class, ExpiredDataCleanupScheduler.class)
                .withBean(ExpiredDataCleanupService.class, () -> Mockito.mock(ExpiredDataCleanupService.class))
                .withPropertyValues("app.cleanup.cron=0 0 3 * * *");

        runner.withPropertyValues("app.cleanup.enabled=true")
                .run(context -> assertThat(context).hasSingleBean(ExpiredDataCleanupScheduler.class));
        runner.withPropertyValues("app.cleanup.enabled=false")
                .run(context -> assertThat(context).doesNotHaveBean(ExpiredDataCleanupScheduler.class));
    }

    private static PendingRegistration pending(String email, Duration expiredAgo) {
        return PendingRegistration.builder().email(email).token(UUID.randomUUID().toString())
                .expiresAt(Instant.now().minus(expiredAgo)).build();
    }

    private static PasswordResetToken reset(UUID userId, String hash, Duration expiredAgo) {
        return PasswordResetToken.builder().userId(userId).tokenHash(hash + UUID.randomUUID())
                .expiresAt(Instant.now().minus(expiredAgo)).build();
    }

    private static RefreshToken refresh(UUID userId, String token, Duration expiredAgo) {
        return RefreshToken.builder().token(token + UUID.randomUUID()).userId(userId)
                .expiresAt(Instant.now().minus(expiredAgo)).build();
    }

    private static EmailVerificationCode code(UUID userId, String code, Duration expiredAgo) {
        return EmailVerificationCode.builder().userId(userId).newEmail("novo@shottrack.com").code(code)
                .expiresAt(Instant.now().minus(expiredAgo)).build();
    }
}
