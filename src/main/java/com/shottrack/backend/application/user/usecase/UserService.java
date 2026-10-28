package com.shottrack.backend.application.user.usecase;

import com.shottrack.backend.application.auth.gateway.RefreshTokenGateway;
import com.shottrack.backend.application.user.dto.ChangeEmailRequest;
import com.shottrack.backend.application.user.dto.ChangePasswordRequest;
import com.shottrack.backend.application.user.dto.CompleteRegistrationRequest;
import com.shottrack.backend.application.user.dto.ConfirmEmailChangeRequest;
import com.shottrack.backend.application.user.dto.RegistrationRequest;
import com.shottrack.backend.application.user.dto.UpdateProfileRequest;
import com.shottrack.backend.application.user.dto.UserResponse;
import com.shottrack.backend.application.user.gateway.EmailVerificationCodeGateway;
import com.shottrack.backend.application.user.gateway.PendingRegistrationGateway;
import com.shottrack.backend.application.user.gateway.UserGateway;
import com.shottrack.backend.application.user.mapper.UserMapper;
import com.shottrack.backend.application.user.model.EmailVerificationCode;
import com.shottrack.backend.application.user.model.ExperienceLevel;
import com.shottrack.backend.application.user.model.PendingRegistration;
import com.shottrack.backend.application.user.model.User;
import com.shottrack.backend.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final Duration VERIFICATION_CODE_TTL = Duration.ofMinutes(15);
    private static final Duration REGISTRATION_TOKEN_TTL = Duration.ofHours(24);
    private static final String VERIFICATION_EMAIL_FROM = "noreply@shottrack.com";

    private final UserGateway userGateway;
    private final EmailVerificationCodeGateway emailVerificationCodeGateway;
    private final PendingRegistrationGateway pendingRegistrationGateway;
    private final RefreshTokenGateway refreshTokenGateway;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JavaMailSender mailSender;
    private final MessageSource messageSource;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @Value("${app.legal.version}")
    private String legalVersion;

    /**
     * Etapa 1 do cadastro (UC01/ADR-0009) — só o email, sem provar posse ainda.
     * Reenvio (mesmo email antes de completar) invalida o token anterior e
     * envia um novo, sem endpoint separado.
     */
    public void requestRegistration(RegistrationRequest request) {
        if (userGateway.existsByEmail(request.email())) {
            throw new BusinessException("EMAIL_ALREADY_REGISTERED", HttpStatus.CONFLICT);
        }

        pendingRegistrationGateway.invalidatePendingByEmail(request.email());

        String token = generateRegistrationToken();
        PendingRegistration pendingRegistration = PendingRegistration.builder()
                .email(request.email())
                .token(token)
                .expiresAt(Instant.now().plus(REGISTRATION_TOKEN_TTL))
                .build();
        pendingRegistrationGateway.save(pendingRegistration);

        sendRegistrationConfirmationEmail(request.email(), token);
    }

    /**
     * Etapa 2 do cadastro (UC23) — só aqui a conta de verdade é criada.
     */
    public UserResponse completeRegistration(CompleteRegistrationRequest request) {
        PendingRegistration pendingRegistration = pendingRegistrationGateway.findByToken(request.token())
                .orElseThrow(() -> new BusinessException("REGISTRATION_TOKEN_INVALID", HttpStatus.BAD_REQUEST));

        if (pendingRegistration.isExpired()) {
            throw new BusinessException("REGISTRATION_TOKEN_EXPIRED", HttpStatus.BAD_REQUEST);
        }

        if (pendingRegistration.isUsed()) {
            throw new BusinessException("REGISTRATION_TOKEN_ALREADY_USED", HttpStatus.BAD_REQUEST);
        }

        // ADR-0019: checado antes de consumir o token — sem aceite o link continua utilizável.
        if (!Boolean.TRUE.equals(request.acceptedTerms())) {
            throw new BusinessException("TERMS_NOT_ACCEPTED", HttpStatus.BAD_REQUEST);
        }

        // S22: o e-mail pode ter sido ocupado entre o pedido e a conclusão do cadastro.
        if (userGateway.existsByEmail(pendingRegistration.getEmail())) {
            throw new BusinessException("EMAIL_ALREADY_REGISTERED", HttpStatus.CONFLICT);
        }

        pendingRegistration.markUsed();
        pendingRegistrationGateway.save(pendingRegistration);

        String passwordHash = passwordEncoder.encode(request.password());
        User user = User.builder()
                .name(request.name())
                .email(pendingRegistration.getEmail())
                .passwordHash(passwordHash)
                .termsAcceptedAt(Instant.now())
                .termsVersion(legalVersion)
                .build();
        User saved = saveRevalidatingEmail(user);

        return userMapper.toResponse(saved);
    }

    public UserResponse getProfile(UUID userId) {
        return userMapper.toResponse(findUserOrThrow(userId));
    }

    public UserResponse updateProfile(UUID userId, UpdateProfileRequest request) {
        User user = findUserOrThrow(userId);
        user.setName(request.name());
        user.setExperienceLevel(ExperienceLevel.valueOf(request.experienceLevel()));
        user.markProfileCompleted(Instant.now());
        return userMapper.toResponse(userGateway.save(user));
    }

    /**
     * Envia o código de verificação (ADR-0002) para o novo email — o email de
     * login só muda quando o código for confirmado em confirmEmailChange.
     */
    public void requestEmailChange(UUID userId, ChangeEmailRequest request) {
        User user = findUserOrThrow(userId);

        // S02: sem a senha atual, quem tem só o token de acesso trocaria o e-mail da conta.
        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new BusinessException("INVALID_CURRENT_PASSWORD", HttpStatus.UNAUTHORIZED);
        }

        if (userGateway.existsByEmail(request.email())) {
            throw new BusinessException("EMAIL_ALREADY_REGISTERED", HttpStatus.CONFLICT);
        }

        emailVerificationCodeGateway.invalidatePendingByUserId(userId);

        String code = generateVerificationCode();
        EmailVerificationCode verification = EmailVerificationCode.builder()
                .userId(userId)
                .newEmail(request.email())
                .code(code)
                .expiresAt(Instant.now().plus(VERIFICATION_CODE_TTL))
                .build();
        emailVerificationCodeGateway.save(verification);

        sendVerificationCodeEmail(request.email(), code);
        // S02: o dono avisado no e-mail ANTIGO (sem o código) pode reagir a um pedido que não fez.
        sendEmailChangeNotice(user.getEmail(), "EMAIL_CHANGE_REQUESTED_NOTICE");
    }

    /**
     * S02: ao efetivar a troca, todas as sessões (refresh tokens) são revogadas — como na troca de senha
     * (UC05) — e o e-mail antigo é avisado. S22: a unicidade é revalidada aqui (o novo e-mail pode ter sido
     * ocupado desde o pedido) e a violação no banco vira EMAIL_ALREADY_REGISTERED, não 500.
     */
    @Transactional
    public UserResponse confirmEmailChange(UUID userId, ConfirmEmailChangeRequest request) {
        EmailVerificationCode verification = emailVerificationCodeGateway.findLatestPendingByUserId(userId)
                .filter(pending -> pending.getCode().equals(request.code()))
                .orElseThrow(() -> new BusinessException("INVALID_VERIFICATION_CODE", HttpStatus.BAD_REQUEST));

        if (verification.isExpired()) {
            throw new BusinessException("VERIFICATION_CODE_EXPIRED", HttpStatus.BAD_REQUEST);
        }

        if (userGateway.existsByEmail(verification.getNewEmail())) {
            throw new BusinessException("EMAIL_ALREADY_REGISTERED", HttpStatus.CONFLICT);
        }

        verification.markUsed();
        emailVerificationCodeGateway.save(verification);

        User user = findUserOrThrow(userId);
        String oldEmail = user.getEmail();
        user.setEmail(verification.getNewEmail());
        User saved = saveRevalidatingEmail(user);

        refreshTokenGateway.revokeAllByUserId(userId);
        sendEmailChangeNotice(oldEmail, "EMAIL_CHANGED_NOTICE");
        return userMapper.toResponse(saved);
    }

    /** S22: grava na hora e traduz a violação de unicidade do e-mail (corrida) em 409. */
    private User saveRevalidatingEmail(User user) {
        try {
            return userGateway.saveAndFlush(user);
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException("EMAIL_ALREADY_REGISTERED", HttpStatus.CONFLICT);
        }
    }

    /**
     * Aviso ao e-mail antigo (pt/en pelo Accept-Language). Nunca leva o código. Falha de envio não
     * desfaz a troca: só é logada, sem o endereço.
     */
    private void sendEmailChangeNotice(String to, String messageKey) {
        var locale = LocaleContextHolder.getLocale();
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(VERIFICATION_EMAIL_FROM);
        message.setTo(to);
        message.setSubject(messageSource.getMessage(messageKey + "_SUBJECT", null, locale));
        message.setText(messageSource.getMessage(messageKey + "_BODY", null, locale));
        try {
            mailSender.send(message);
        } catch (MailException e) {
            log.error("Falha ao enviar aviso de troca de e-mail ({})", e.getClass().getSimpleName());
        }
    }

    /**
     * UC05/ADR-0018: ao trocar a senha, todas as sessões (refresh tokens) do
     * atleta são revogadas — o front leva de volta ao login.
     */
    @Transactional
    public void changePassword(UUID userId, ChangePasswordRequest request) {
        User user = findUserOrThrow(userId);

        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new BusinessException("INVALID_CURRENT_PASSWORD", HttpStatus.UNAUTHORIZED);
        }

        if (passwordEncoder.matches(request.newPassword(), user.getPasswordHash())) {
            throw new BusinessException("PASSWORD_UNCHANGED", HttpStatus.BAD_REQUEST);
        }

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userGateway.save(user);

        refreshTokenGateway.revokeAllByUserId(userId);
    }

    private User findUserOrThrow(UUID userId) {
        return userGateway.findById(userId)
                .orElseThrow(() -> new BusinessException("UNAUTHORIZED", HttpStatus.UNAUTHORIZED));
    }

    private String generateVerificationCode() {
        return String.format("%06d", SECURE_RANDOM.nextInt(1_000_000));
    }

    private void sendVerificationCodeEmail(String to, String code) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(VERIFICATION_EMAIL_FROM);
        message.setTo(to);
        message.setSubject("Confirme seu novo email - ShotTrack");
        message.setText("Seu código de verificação é " + code + ". Ele expira em 15 minutos.");
        mailSender.send(message);
    }

    private String generateRegistrationToken() {
        return UUID.randomUUID().toString();
    }

    private void sendRegistrationConfirmationEmail(String to, String token) {
        String link = frontendUrl + "/cadastro/completar?token=" + token;

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(VERIFICATION_EMAIL_FROM);
        message.setTo(to);
        message.setSubject("Confirme seu cadastro - ShotTrack");
        message.setText("Clique no link para completar seu cadastro (expira em 24 horas):\n\n" + link);
        mailSender.send(message);
    }
}
