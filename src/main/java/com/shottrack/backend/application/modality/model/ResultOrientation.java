package com.shottrack.backend.application.modality.model;

/**
 * ADR-0011 (revisão): atributo fixo do catálogo (seed), não algo que o
 * atleta configura — define se, pra esse tipo de resultado, um valor menor
 * ou maior é "melhor" (usado no destaque dinâmico do dashboard, UC42).
 */
public enum ResultOrientation {
    MENOR_MELHOR,
    MAIOR_MELHOR,
    NAO_APLICAVEL
}
