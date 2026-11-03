package com.shottrack.backend.common.logging;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * S09: o console recebe a mesma máscara de e-mail/JWT do arquivo JSON (ADR-0019) — inclusive dentro de
 * mensagens de exceção de terceiros, como a violação de unique do Postgres.
 */
@Slf4j
@SpringBootTest
@ExtendWith(OutputCaptureExtension.class)
class ConsoleLogMaskingTest {

    private static final String JWT = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxMjMifQ.c2lnbmF0dXJl";

    @Test
    void shouldMaskEmailInsideAnExceptionMessage(CapturedOutput output) {
        log.error("Falha ao gravar", new IllegalStateException(
                "ERROR: duplicate key value violates unique constraint\n  Detail: Key (email)=(x@y.com) already exists."));

        assertThat(output.getAll()).contains("Key (email)=(***@***) already exists").doesNotContain("x@y.com");
    }

    @Test
    void shouldMaskEmailAndJwtInTheLogMessage(CapturedOutput output) {
        log.warn("login de maria.silva+tiro@exemplo.com.br com token {}", JWT);

        assertThat(output.getAll()).contains("login de ***@*** com token ***")
                .doesNotContain("maria.silva").doesNotContain(JWT);
    }

    @Test
    void shouldMaskEmailInTheCauseChain(CapturedOutput output) {
        log.error("Erro", new RuntimeException("externo", new IllegalArgumentException("usuário a.b@c.org inválido")));

        assertThat(output.getAll()).contains("usuário ***@*** inválido").doesNotContain("a.b@c.org");
    }
}
