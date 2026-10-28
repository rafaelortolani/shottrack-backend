package com.shottrack.backend.application.auth.usecase;

import com.shottrack.backend.application.auth.dto.PasswordResetConfirmRequest;
import com.shottrack.backend.application.auth.dto.PasswordResetRequest;
import com.shottrack.backend.application.auth.gateway.PasswordResetTokenGateway;
import com.shottrack.backend.application.auth.gateway.RefreshTokenGateway;
import com.shottrack.backend.application.auth.model.PasswordResetToken;
import com.shottrack.backend.application.user.gateway.UserGateway;
import com.shottrack.backend.application.user.model.User;
import com.shottrack.backend.common.exception.BusinessException;
import com.shottrack.backend.common.security.TokenHasher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
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
import java.util.Base64;

/**
 * UC49/UC50/ADR-0018: recuperação de senha por link de uso único.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final Duration TOKEN_TTL = Duration.ofHours(1);
    private static final int TOKEN_BYTES = 32;
    private static final String EMAIL_FROM = "noreply@shottrack.com";

    private final UserGateway userGateway;
    private final PasswordResetTokenGateway passwordResetTokenGateway;
    private final RefreshTokenGateway refreshTokenGateway;
    private final PasswordEncoder passwordEncoder;
    private final JavaMailSender mailSender;
    private final MessageSource messageSource;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    /**
     * UC49: a resposta ao cliente é a mesma exista a conta ou não (ADR-0018).
     * O envio é síncrono, então o tempo de resposta de uma conta existente é
     * um pouco maior (SMTP) — aceito e documentado no UC49; o rate limit por
     * e-mail/IP limita o quanto isso pode ser explorado. Falha no SMTP é só
     * logada: devolver erro aqui revelaria que a conta existe.
     */
    @Transactional
    public void request(PasswordResetRequest request) {
        userGateway.findByEmail(request.email()).ifPresent(this::issueToken);
    }

    /**
     * UC50: valida o token, troca a senha e derruba todas as sessões, tudo na
     * mesma transação — se algo falhar no meio, o token continua utilizável.
     */
    @Transactional
    public void confirm(PasswordResetConfirmRequest request) {
        PasswordResetToken token = passwordResetTokenGateway
                .findByTokenHash(TokenHasher.sha256Hex(request.token()))
                .orElseThrow(() -> new BusinessException("PASSWORD_RESET_TOKEN_INVALID", HttpStatus.BAD_REQUEST));

        if (token.isUsed()) {
            throw new BusinessException("PASSWORD_RESET_TOKEN_ALREADY_USED", HttpStatus.BAD_REQUEST);
        }

        if (token.isExpired()) {
            throw new BusinessException("PASSWORD_RESET_TOKEN_EXPIRED", HttpStatus.BAD_REQUEST);
        }

        User user = userGateway.findById(token.getUserId())
                .orElseThrow(() -> new BusinessException("PASSWORD_RESET_TOKEN_INVALID", HttpStatus.BAD_REQUEST));

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userGateway.save(user);

        token.markUsed(Instant.now());
        passwordResetTokenGateway.save(token);

        refreshTokenGateway.revokeAllByUserId(user.getId());
    }

    /**
     * Um novo pedido substitui o anterior: os tokens ainda não usados do
     * usuário são apagados, então o link antigo passa a ser "inexistente"
     * (PASSWORD_RESET_TOKEN_INVALID). Tokens já usados ficam, pra que reuso
     * continue respondendo PASSWORD_RESET_TOKEN_ALREADY_USED.
     */
    private void issueToken(User user) {
        passwordResetTokenGateway.deletePendingByUserId(user.getId());

        String token = generateToken();
        PasswordResetToken resetToken = PasswordResetToken.builder()
                .userId(user.getId())
                .tokenHash(TokenHasher.sha256Hex(token))
                .expiresAt(Instant.now().plus(TOKEN_TTL))
                .build();
        passwordResetTokenGateway.save(resetToken);

        sendResetEmail(user.getEmail(), token);
    }

    private String generateToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private void sendResetEmail(String to, String token) {
        String link = frontendUrl + "/redefinir-senha?token=" + token;
        var locale = LocaleContextHolder.getLocale();

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(EMAIL_FROM);
        message.setTo(to);
        message.setSubject(messageSource.getMessage("PASSWORD_RESET_EMAIL_SUBJECT", null, locale));
        message.setText(messageSource.getMessage("PASSWORD_RESET_EMAIL_BODY", new Object[]{link}, locale));

        try {
            mailSender.send(message);
        } catch (MailException e) {
            // ADR-0019: a mensagem da exceção do SMTP costuma trazer o endereço do
            // destinatário — loga só o tipo, nunca o e-mail.
            log.error("Falha ao enviar e-mail de redefinição de senha ({})", e.getClass().getSimpleName());
        }
    }
}
