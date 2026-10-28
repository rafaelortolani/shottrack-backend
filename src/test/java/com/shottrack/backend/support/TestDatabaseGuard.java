package com.shottrack.backend.support;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;

import java.util.Set;

/**
 * Trava de segurança (Lote 0): nenhum contexto de teste sobe apontando para o banco de
 * desenvolvimento (`shottrack`), para o do E2E do front (`shottrack_test`) ou para o vhost
 * padrão do RabbitMQ (`/`) — a suíte aplica migrations, grava e apaga dados. Registrado em
 * META-INF/spring.factories de src/test, então vale para todo @SpringBootTest.
 */
public class TestDatabaseGuard implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    static final Set<String> FORBIDDEN_DATABASES = Set.of("shottrack", "shottrack_test");

    @Override
    public void initialize(ConfigurableApplicationContext context) {
        check(context.getEnvironment());
    }

    static void check(ConfigurableEnvironment environment) {
        String url = environment.getProperty("spring.datasource.url", "");
        String database = databaseName(url);
        if (FORBIDDEN_DATABASES.contains(database)) {
            throw new IllegalStateException("Os testes não podem usar o banco '" + database
                    + "' (desenvolvimento/E2E). Use o banco de testes: scripts/criar-ambiente-de-teste.sh");
        }
        String vhost = environment.getProperty("spring.rabbitmq.virtual-host", "/");
        if ("/".equals(vhost)) {
            throw new IllegalStateException("Os testes não podem usar o vhost padrão '/' do RabbitMQ. "
                    + "Use o vhost de testes: scripts/criar-ambiente-de-teste.sh");
        }
    }

    static String databaseName(String jdbcUrl) {
        String withoutQuery = jdbcUrl.split("\\?")[0];
        return withoutQuery.substring(withoutQuery.lastIndexOf('/') + 1);
    }
}
