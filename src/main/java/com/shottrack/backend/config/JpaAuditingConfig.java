package com.shottrack.backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Habilita createdAt/updatedAt automáticos (@CreatedDate/@LastModifiedDate) em
 * AbstractBaseEntity. createdBy/updatedBy ficam null até existir um AuditorAware
 * (depende do login, que ainda não existe — é o próximo use case).
 */
@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {
}
