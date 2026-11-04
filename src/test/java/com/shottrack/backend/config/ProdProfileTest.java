package com.shottrack.backend.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** S07: perfil prod — contexto sobe com as variáveis, Swagger e demais endpoints do actuator não existem, nada tem credencial padrão. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("prod")
@TestPropertySource(properties = {
        "DB_URL=jdbc:postgresql://localhost:5432/shottrack_unit_test",
        "DB_USERNAME=shottrack", "DB_PASSWORD=shottrack",
        "RABBITMQ_HOST=localhost", "RABBITMQ_USERNAME=shottrack", "RABBITMQ_PASSWORD=shottrack",
        "RABBITMQ_VHOST=shottrack_unit_test",
        "SMTP_HOST=localhost", "SMTP_PORT=1025", "SMTP_USERNAME=smtp", "SMTP_PASSWORD=smtp",
        "FRONTEND_URL=https://app.exemplo.com",
        // ADR-0026: em prod o diretório das fotos precisa existir (criado no inicializador estático abaixo)
        "APP_STORAGE_LOCAL_DIR=${java.io.tmpdir}/shottrack-uploads-prod-test",
        // o Mailpit local não fala STARTTLS; só o handshake do health do mail dependeria disso
        "spring.mail.properties.mail.smtp.starttls.enable=false",
        "spring.mail.properties.mail.smtp.starttls.required=false"
})
class ProdProfileTest {

    private static final String[] REQUIRED = {"DB_URL", "DB_USERNAME", "DB_PASSWORD", "RABBITMQ_HOST", "RABBITMQ_USERNAME",
            "RABBITMQ_PASSWORD", "SMTP_HOST", "SMTP_PORT", "SMTP_USERNAME", "SMTP_PASSWORD", "FRONTEND_URL", "APP_STORAGE_LOCAL_DIR"};

    static {
        try {
            java.nio.file.Files.createDirectories(java.nio.file.Path.of(System.getProperty("java.io.tmpdir"), "shottrack-uploads-prod-test"));
        } catch (IOException e) {
            throw new java.io.UncheckedIOException(e);
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldHideSwaggerAndOpenApiDocs() throws Exception {
        mockMvc.perform(get("/v3/api-docs")).andExpect(status().isNotFound());
        mockMvc.perform(get("/swagger-ui/index.html")).andExpect(status().isNotFound());
        mockMvc.perform(get("/swagger-ui.html")).andExpect(status().isNotFound());
    }

    @Test
    void shouldExposeOnlyActuatorHealth() throws Exception {
        // fora do health nada é público (anyRequest autenticado) e, mesmo logado, o endpoint não está exposto
        mockMvc.perform(get("/actuator/info")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/actuator/env")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
    }

    /** Sem a variável não há valor padrão: o placeholder não resolve e a aplicação falha ao subir. */
    @ParameterizedTest
    @ValueSource(strings = {"DB_URL", "DB_USERNAME", "DB_PASSWORD", "RABBITMQ_HOST", "RABBITMQ_USERNAME", "RABBITMQ_PASSWORD",
            "SMTP_HOST", "SMTP_PORT", "SMTP_USERNAME", "SMTP_PASSWORD", "FRONTEND_URL", "APP_STORAGE_LOCAL_DIR"})
    void shouldFailWithoutTheRequiredVariable(String missing) throws IOException {
        var environment = prodEnvironmentWithout(missing);

        assertThatThrownBy(() -> {
            environment.getProperty("spring.datasource.url");
            environment.getProperty("spring.datasource.username");
            environment.getProperty("spring.datasource.password");
            environment.getProperty("spring.rabbitmq.host");
            environment.getProperty("spring.rabbitmq.username");
            environment.getProperty("spring.rabbitmq.password");
            environment.getProperty("spring.mail.host");
            environment.getProperty("spring.mail.port");
            environment.getProperty("spring.mail.username");
            environment.getProperty("spring.mail.password");
            environment.getProperty("app.frontend-url");
            environment.getProperty("app.storage.local-dir");
        }).isInstanceOf(IllegalArgumentException.class).hasMessageContaining(missing);
    }

    @Test
    void shouldResolveEverythingWhenAllVariablesArePresent() throws IOException {
        var environment = prodEnvironmentWithout(null);

        assertThat(environment.getProperty("spring.datasource.url")).isEqualTo("jdbc:postgresql://x/db");
        assertThat(environment.getProperty("app.storage.local-dir")).isEqualTo("v");
        assertThat(environment.getProperty("app.storage.require-existing-dir")).isEqualTo("true");
        assertThat(environment.getProperty("springdoc.api-docs.enabled")).isEqualTo("false");
        assertThat(environment.getProperty("springdoc.swagger-ui.enabled")).isEqualTo("false");
        assertThat(environment.getProperty("management.endpoints.web.exposure.include")).isEqualTo("health");
    }

    private static StandardEnvironment prodEnvironmentWithout(String missing) throws IOException {
        var environment = new StandardEnvironment();
        Map<String, Object> variables = new LinkedHashMap<>();
        for (String name : REQUIRED) {
            if (!name.equals(missing)) {
                variables.put(name, name.equals("DB_URL") ? "jdbc:postgresql://x/db" : "v");
            }
        }
        environment.getPropertySources().addFirst(new MapPropertySource("variaveis", variables));
        new YamlPropertySourceLoader().load("prod", new ClassPathResource("application-prod.yml"))
                .forEach(environment.getPropertySources()::addLast);
        return environment;
    }
}
