package com.shottrack.backend.application.user.usecase;

import com.shottrack.backend.application.accessory.gateway.AccessoryGateway;
import com.shottrack.backend.application.accessory.gateway.AccessoryWeaponGateway;
import com.shottrack.backend.application.accessory.model.Accessory;
import com.shottrack.backend.application.ammunition.gateway.AmmunitionGateway;
import com.shottrack.backend.application.auth.gateway.PasswordResetTokenGateway;
import com.shottrack.backend.application.auth.gateway.RefreshTokenGateway;
import com.shottrack.backend.application.dashboard.gateway.DashboardSummaryGateway;
import com.shottrack.backend.application.habituality.gateway.AthleteHabitualityGateway;
import com.shottrack.backend.application.modality.gateway.ModalityResultTypeSelectionGateway;
import com.shottrack.backend.application.modality.gateway.PracticedModalityGateway;
import com.shottrack.backend.application.series.gateway.SeriesGateway;
import com.shottrack.backend.application.series.gateway.SeriesResultGateway;
import com.shottrack.backend.application.series.model.Series;
import com.shottrack.backend.application.traininglocation.gateway.TrainingLocationGateway;
import com.shottrack.backend.application.user.dto.DeleteAccountRequest;
import com.shottrack.backend.application.user.gateway.EmailVerificationCodeGateway;
import com.shottrack.backend.application.user.gateway.PendingRegistrationGateway;
import com.shottrack.backend.application.user.gateway.UserGateway;
import com.shottrack.backend.application.user.model.User;
import com.shottrack.backend.application.visit.gateway.TrainingGateway;
import com.shottrack.backend.application.visit.gateway.VisitGateway;
import com.shottrack.backend.application.visit.model.Training;
import com.shottrack.backend.application.visit.model.Visit;
import com.shottrack.backend.application.weapon.gateway.WeaponGateway;
import com.shottrack.backend.application.weapon.model.Weapon;
import com.shottrack.backend.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * UC52/ADR-0019: exclusão imediata e definitiva da conta. Apaga em código, na
 * ordem das dependências, sem depender de ON DELETE CASCADE — e tudo numa
 * única transação: se qualquer passo falhar, nada é apagado. O bloqueio de
 * exclusão de itens usados (ADR-0006) não vale aqui, é a conta inteira que sai.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AccountDeletionService {

    private static final String EMAIL_FROM = "noreply@shottrack.com";

    private final UserGateway userGateway;
    private final DashboardSummaryGateway dashboardSummaryGateway;
    private final AthleteHabitualityGateway athleteHabitualityGateway;
    private final SeriesResultGateway seriesResultGateway;
    private final SeriesGateway seriesGateway;
    private final TrainingGateway trainingGateway;
    private final VisitGateway visitGateway;
    private final TrainingLocationGateway trainingLocationGateway;
    private final AccessoryWeaponGateway accessoryWeaponGateway;
    private final AccessoryGateway accessoryGateway;
    private final AmmunitionGateway ammunitionGateway;
    private final WeaponGateway weaponGateway;
    private final ModalityResultTypeSelectionGateway modalityResultTypeSelectionGateway;
    private final PracticedModalityGateway practicedModalityGateway;
    private final RefreshTokenGateway refreshTokenGateway;
    private final EmailVerificationCodeGateway emailVerificationCodeGateway;
    private final PasswordResetTokenGateway passwordResetTokenGateway;
    private final PendingRegistrationGateway pendingRegistrationGateway;
    private final PasswordEncoder passwordEncoder;
    private final JavaMailSender mailSender;
    private final MessageSource messageSource;
    private final TransactionTemplate transactionTemplate;

    public void delete(UUID userId, DeleteAccountRequest request) {
        User user = userGateway.findById(userId)
                .orElseThrow(() -> new BusinessException("UNAUTHORIZED", HttpStatus.UNAUTHORIZED));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BusinessException("INVALID_CURRENT_PASSWORD", HttpStatus.UNAUTHORIZED);
        }

        String email = user.getEmail();
        Locale locale = LocaleContextHolder.getLocale();

        transactionTemplate.executeWithoutResult(status -> eraseAllData(userId, email));

        // Só depois do commit: se o envio falhar, a exclusão (já definitiva) permanece.
        sendDeletionNotice(email, locale);
    }

    /**
     * Ordem: o que referencia vem antes do que é referenciado — resumo do
     * dashboard, acompanhamento e meta de habitualidade, resultados, séries, treinos, visitas, locais, associações
     * acessório-arma, acessórios, munições, armas, seleções de resultado,
     * modalidades, tokens, cadastros pendentes e, por fim, o usuário.
     */
    private void eraseAllData(UUID userId, String email) {
        // Trava a linha do usuário até o commit: um evento de recálculo do dashboard
        // em voo (ADR-0015) que tentasse regravar o resumo no meio da exclusão fica
        // esperando — e, depois do commit, cai na checagem de "usuário inexistente".
        userGateway.findByIdForUpdate(userId)
                .orElseThrow(() -> new BusinessException("UNAUTHORIZED", HttpStatus.UNAUTHORIZED));

        dashboardSummaryGateway.deleteByUserId(userId);
        // ADR-0023: acompanhamento e meta de habitualidade são dados do atleta.
        athleteHabitualityGateway.deleteTrackingByUserId(userId);
        athleteHabitualityGateway.deleteGoalByUserId(userId);

        List<UUID> visitIds = visitGateway.findAllByUserId(userId).stream().map(Visit::getId).toList();
        if (!visitIds.isEmpty()) {
            List<UUID> trainingIds = trainingGateway.findAllByVisitIdIn(visitIds).stream()
                    .map(Training::getId).toList();
            if (!trainingIds.isEmpty()) {
                List<UUID> seriesIds = seriesGateway.findAllByTrainingIdIn(trainingIds).stream()
                        .map(Series::getId).toList();
                if (!seriesIds.isEmpty()) {
                    seriesResultGateway.deleteAllBySeriesIdIn(seriesIds);
                }
                seriesGateway.deleteAllByTrainingIdIn(trainingIds);
            }
            trainingGateway.deleteAllByVisitIdIn(visitIds);
        }
        visitGateway.deleteAllByUserId(userId);
        trainingLocationGateway.deleteAllByUserId(userId);

        List<UUID> accessoryIds = accessoryGateway.findAllByUserId(userId).stream().map(Accessory::getId).toList();
        List<UUID> weaponIds = weaponGateway.findAllByUserId(userId).stream().map(Weapon::getId).toList();
        if (!accessoryIds.isEmpty()) {
            accessoryWeaponGateway.deleteAllByAccessoryIdIn(accessoryIds);
        }
        if (!weaponIds.isEmpty()) {
            accessoryWeaponGateway.deleteAllByWeaponIdIn(weaponIds);
        }
        accessoryGateway.deleteAllByUserId(userId);
        ammunitionGateway.deleteAllByUserId(userId);
        weaponGateway.deleteAllByUserId(userId);

        modalityResultTypeSelectionGateway.deleteAllByUserId(userId);
        practicedModalityGateway.deleteAllByUserId(userId);

        refreshTokenGateway.deleteAllByUserId(userId);
        emailVerificationCodeGateway.deleteAllByUserId(userId);
        passwordResetTokenGateway.deleteAllByUserId(userId);
        pendingRegistrationGateway.deleteAllByEmail(email);

        userGateway.deleteById(userId);
    }

    private void sendDeletionNotice(String to, Locale locale) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(EMAIL_FROM);
        message.setTo(to);
        message.setSubject(messageSource.getMessage("ACCOUNT_DELETED_EMAIL_SUBJECT", null, locale));
        message.setText(messageSource.getMessage("ACCOUNT_DELETED_EMAIL_BODY", null, locale));

        try {
            mailSender.send(message);
        } catch (MailException e) {
            // ADR-0019: a mensagem da exceção do SMTP costuma trazer o endereço do
            // destinatário — loga só o tipo, nunca o e-mail.
            log.error("Falha ao enviar e-mail de aviso de exclusão de conta ({})", e.getClass().getSimpleName());
        }
    }
}
