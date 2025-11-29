package de.nofelix.inventorybackend;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/**
 * Integration test that verifies the application context loads successfully.
 * 
 * <p>Uses Testcontainers for PostgreSQL database to ensure real database
 * connectivity during context loading.</p>
 * 
 * <p>Note: This test is temporarily disabled due to Testcontainers/Docker Desktop
 * compatibility issues on Windows.</p>
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Disabled("Temporarily disabled: Testcontainers requires Docker Desktop configuration")
class InventoryBackendApplicationTests {

    @Test
    void contextLoads() {
    }

}
