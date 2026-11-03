package com.shottrack.backend.common.validation;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * S04: leitura segura de número vindo como texto. `new BigDecimal("1E+99999999")` é aceito, mas contas
 * com ele (remainder, add, divide) tentam materializar bilhões de dígitos e travam o servidor. Aqui só
 * entra número "simples": sem notação científica, com no máximo {@value #MAX_LENGTH} caracteres,
 * precisão ≤ {@value #MAX_PRECISION} dígitos e no máximo {@value #MAX_SCALE} casas decimais.
 * Qualquer outra coisa — inclusive valor antigo já gravado fora dessa faixa — vira vazio, e quem
 * chama decide (rejeitar no cadastro, ignorar nos cálculos).
 */
public final class SafeDecimal {

    public static final int MAX_LENGTH = 32;
    public static final int MAX_PRECISION = 18;
    public static final int MAX_SCALE = 6;

    private static final Pattern SIMPLE_NUMBER = Pattern.compile("[+-]?(\\d+(\\.\\d+)?|\\.\\d+)");

    private SafeDecimal() {
    }

    public static Optional<BigDecimal> parse(String text) {
        if (text == null || text.length() > MAX_LENGTH || !SIMPLE_NUMBER.matcher(text).matches()) {
            return Optional.empty();
        }
        BigDecimal value = new BigDecimal(text);
        if (value.precision() > MAX_PRECISION || value.scale() > MAX_SCALE) {
            return Optional.empty();
        }
        return Optional.of(value);
    }
}
