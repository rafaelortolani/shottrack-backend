package com.shottrack.backend.application.user.usecase;

import com.shottrack.backend.application.user.dto.ChangeEmailRequest;
import com.shottrack.backend.application.user.dto.ChangePasswordRequest;
import com.shottrack.backend.application.user.dto.ConfirmEmailChangeRequest;
import com.shottrack.backend.application.user.dto.UpdateProfileRequest;
import com.shottrack.backend.application.user.dto.UserRegisterRequest;
import com.shottrack.backend.application.user.dto.UserResponse;
import com.shottrack.backend.application.user.gateway.EmailVerificationCodeGateway;
import com.shottrack.backend.application.user.gateway.UserGateway;
import com.shottrack.backend.application.user.mapper.UserMapper;
import com.shottrack.backend.application.user.model.EmailVerificationCode;
import com.shottrack.backend.application.user.model.ExperienceLevel;
import com.shottrack.backend.application.user.model.User;
import com.shottrack.backend.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final Duration VERIFICATION_CODE_TTL = Duration.ofMinutes(15);
    private static final String VERIFICATION_EMAIL_FROM = "noreply@shottrack.com";

    private final UserGateway userGateway;
    private final EmailVerificationCodeGateway emailVerificationCodeGateway;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JavaMailSender mailSender;

    public UserResponse register(UserRegisterRequest request) {
        if (userGateway.existsByEmail(request.email())) {
            throw new BusinessException("EMAIL_ALREADY_REGISTERED", HttpStatus.CONFLICT);
        }

        String passwordHash = passwordEncoder.encode(request.password());
        User user = new User(request.name(), request.email(), passwordHash);
        User saved = userGateway.save(user);

        return userMapper.toResponse(saved);
    }

    public UserResponse getProfile(UUID userId) {
        return userMapper.toResponse(findUserOrThrow(userId));
    }

    public UserResponse updateProfile(UUID userId, UpdateProfileRequest request) {
        User user = findUserOrThrow(userId);
        user.setName(request.name());
        user.setExperienceLevel(ExperienceLevel.valueOf(request.experienceLevel()));
        return userMapper.toResponse(userGateway.save(user));
    }

    /**
     * Envia o código de verificação (ADR-0002) para o novo email — o email de
     * login só muda quando o código for confirmado em confirmEmailChange.
     */
    public void requestEmailChange(UUID userId, ChangeEmailRequest request) {
        findUserOrThrow(userId);

        if (userGateway.existsByEmail(request.email())) {
            throw new BusinessException("EMAIL_ALREADY_REGISTERED", HttpStatus.CONFLICT);
        }

        emailVerificationCodeGateway.invalidatePendingByUserId(userId);

        String code = generateVerificationCode();
        EmailVerificationCode verification = new EmailVerificationCode(
                userId, request.email(), code, Instant.now().plus(VERIFICATION_CODE_TTL));
        emailVerificationCodeGateway.save(verification);

        sendVerificationCodeEmail(request.email(), code);
    }

    public UserResponse confirmEmailChange(UUID userId, ConfirmEmailChangeRequest request) {
        EmailVerificationCode verification = emailVerificationCodeGateway.findLatestPendingByUserId(userId)
                .filter(pending -> pending.getCode().equals(request.code()))
                .orElseThrow(() -> new BusinessException("INVALID_VERIFICATION_CODE", HttpStatus.BAD_REQUEST));

        if (verification.isExpired()) {
            throw new BusinessException("VERIFICATION_CODE_EXPIRED", HttpStatus.BAD_REQUEST);
        }

        verification.markUsed();
        emailVerificationCodeGateway.save(verification);

        User user = findUserOrThrow(userId);
        user.setEmail(verification.getNewEmail());
        return userMapper.toResponse(userGateway.save(user));
    }

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
}
