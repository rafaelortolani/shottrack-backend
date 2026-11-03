package com.shottrack.backend.common.validation;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTimeout;

/** S04: só número "simples" e dentro da faixa passa; o resto é recusado SEM fazer conta com ele. */
class SafeDecimalTest {

    @Test
    void shouldAcceptNormalNumbers() {
        for (String text : new String[]{"0", "9.8", "-0.5", "+5", ".5", "100", "0.000001", "123456789012345678", "-12.345678"}) {
            assertThat(SafeDecimal.parse(text)).as(text).isPresent();
        }
        assertThat(SafeDecimal.parse("9.8")).contains(new BigDecimal("9.8"));
        assertThat(SafeDecimal.parse("0")).contains(BigDecimal.ZERO);
    }

    @Test
    void shouldRejectScientificNotationThatFreezesBigDecimalMath() {
        assertTimeout(Duration.ofSeconds(2), () -> {
            for (String text : new String[]{"1E+99999999", "1e999999999", "-1E+99999999", "1e3", "2E10", "1E-99999999"}) {
                assertThat(SafeDecimal.parse(text)).as(text).isEmpty();
            }
        });
    }

    @Test
    void shouldRejectTooLongTextAndOutOfRangePrecisionAndScale() {
        assertThat(SafeDecimal.parse("9".repeat(33))).as("33 caracteres").isEmpty();
        assertThat(SafeDecimal.parse("1".repeat(19))).as("19 dígitos").isEmpty();
        assertThat(SafeDecimal.parse("0.0000001")).as("7 casas decimais").isEmpty();
        assertThat(SafeDecimal.parse("1." + "0".repeat(30))).as("32 caracteres com escala 30").isEmpty();
    }

    @Test
    void shouldRejectNonNumericText() {
        for (String text : new String[]{null, "", " ", "abc", "1,5", "1 2", "--1", "1.", "0x10", "NaN", "Infinity", "sim"}) {
            assertThat(SafeDecimal.parse(text)).as(String.valueOf(text)).isEmpty();
        }
    }

    @Test
    void shouldAcceptTheExactLimits() {
        assertThat(SafeDecimal.parse("9".repeat(SafeDecimal.MAX_PRECISION))).isPresent();
        assertThat(SafeDecimal.parse("0." + "1".repeat(SafeDecimal.MAX_SCALE))).isPresent();
        assertThat(SafeDecimal.parse("1".repeat(SafeDecimal.MAX_LENGTH))).as("32 dígitos passam do limite de precisão").isEmpty();
    }
}
