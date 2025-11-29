package de.nofelix.inventorybackend;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Test configuration providing Testcontainers for integration tests.
 * 
 * <p>Uses PostgreSQLContainer with @ServiceConnection which automatically
 * configures both JDBC (for Flyway) and R2DBC connection details.</p>
 * 
 * <p>Requires Docker Desktop with TCP daemon exposed on port 2375,
 * or proper Docker socket access via named pipes on Windows.</p>
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

    @Bean
    @ServiceConnection
    @SuppressWarnings("resource") // Container lifecycle managed by Spring Boot Testcontainers
    PostgreSQLContainer<?> postgresContainer() {
        return new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine"));
    }
}
