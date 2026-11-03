package com.shottrack.backend.common.validation;

import java.util.Locale;

/**
 * S15: e-mail entra sempre sem espaços nas pontas e em minúsculas (cadastro, login, troca de e-mail e
 * redefinição) — a unicidade é por lower(email) e comparar com a caixa original deixaria o login falhar.
 */
public final class EmailNormalizer {

    private EmailNormalizer() {
    }

    public static String normalize(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }
}
