package com.shottrack.backend.support;

import jakarta.persistence.EntityManager;
import org.springframework.boot.test.autoconfigure.web.servlet.MockMvcBuilderCustomizer;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Nos testes `@Transactional` o Hibernate só grava no banco no flush; o JdbcTemplate dos testes lê direto do banco e não
 * veria o que a requisição acabou de fazer. Este customizador dá o flush ao fim de cada requisição do MockMvc.
 */
@TestConfiguration
public class FlushAfterEachRequest {

    @Bean
    MockMvcBuilderCustomizer flushAfterEachRequestCustomizer(EntityManager entityManager) {
        return builder -> builder.alwaysDo(result -> {
            if (TransactionSynchronizationManager.isActualTransactionActive()) {
                entityManager.flush();
            }
        });
    }
}
