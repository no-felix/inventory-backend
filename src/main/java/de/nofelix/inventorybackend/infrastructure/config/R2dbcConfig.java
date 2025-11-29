package de.nofelix.inventorybackend.infrastructure.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.r2dbc.config.EnableR2dbcAuditing;

/**
 * Configuration for R2DBC auditing.
 * 
 * <p>Enables automatic population of @CreatedDate and @LastModifiedDate fields.</p>
 */
@Configuration
@EnableR2dbcAuditing
public class R2dbcConfig {
}
