package com.visco.backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Enables JPA auditing via {@link EnableJpaAuditing}. Activates automatic
 * population of {@code createdDate}, {@code lastModifiedDate}, and similar
 * auditing fields when entities are persisted or updated.
 */
@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {
}
