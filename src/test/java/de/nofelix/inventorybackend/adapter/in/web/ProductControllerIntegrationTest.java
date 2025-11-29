package de.nofelix.inventorybackend.adapter.in.web;

import de.nofelix.inventorybackend.TestcontainersConfiguration;
import de.nofelix.inventorybackend.adapter.in.web.model.ProductRequest;
import de.nofelix.inventorybackend.adapter.in.web.model.ProductResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for Product API endpoints.
 * 
 * <p>Tests full stack with real database using Testcontainers PostgreSQL.</p>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
@DisplayName("Product API Integration Tests")
class ProductControllerIntegrationTest {

    @Autowired
    private WebTestClient webTestClient;

    private ProductRequest createProductRequest;

    @BeforeEach
    void setUp() {
        createProductRequest = new ProductRequest()
                .sku("INT-TEST-SKU-" + System.currentTimeMillis())
                .name("Integration Test Product")
                .description("A product for integration testing")
                .quantityOnHand(50)
                .unitPrice(24.99);
    }

    @Nested
    @DisplayName("Create and Read Operations")
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class CreateAndReadTests {

        @Test
        @Order(1)
        @DisplayName("should create a new product")
        void shouldCreateNewProduct() {
            webTestClient.post()
                    .uri("/api/v1/products")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(createProductRequest)
                    .exchange()
                    .expectStatus().isCreated()
                    .expectBody(ProductResponse.class)
                    .value(response -> {
                        assertThat(response.getId()).isNotNull();
                        assertThat(response.getSku()).isEqualTo(createProductRequest.getSku());
                        assertThat(response.getName()).isEqualTo("Integration Test Product");
                        assertThat(response.getQuantityOnHand()).isEqualTo(50);
                        assertThat(response.getUnitPrice()).isEqualTo(24.99);
                        assertThat(response.getCreatedAt()).isNotNull();
                        assertThat(response.getUpdatedAt()).isNotNull();
                    });
        }

        @Test
        @Order(2)
        @DisplayName("should fail to create product with duplicate SKU")
        void shouldFailToCreateProductWithDuplicateSku() {
            // First, create the product
            String uniqueSku = "DUPLICATE-TEST-" + System.currentTimeMillis();
            ProductRequest request = new ProductRequest()
                    .sku(uniqueSku)
                    .name("First Product")
                    .quantityOnHand(10)
                    .unitPrice(9.99);

            webTestClient.post()
                    .uri("/api/v1/products")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .exchange()
                    .expectStatus().isCreated();

            // Try to create another with the same SKU
            ProductRequest duplicateRequest = new ProductRequest()
                    .sku(uniqueSku)
                    .name("Second Product")
                    .quantityOnHand(20)
                    .unitPrice(19.99);

            webTestClient.post()
                    .uri("/api/v1/products")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(duplicateRequest)
                    .exchange()
                    .expectStatus().isEqualTo(409);
        }

        @Test
        @Order(3)
        @DisplayName("should get product by ID after creation")
        void shouldGetProductByIdAfterCreation() {
            // Create a product first
            String uniqueSku = "GET-BY-ID-TEST-" + System.currentTimeMillis();
            ProductRequest request = new ProductRequest()
                    .sku(uniqueSku)
                    .name("Get By ID Test Product")
                    .quantityOnHand(25)
                    .unitPrice(49.99);

            ProductResponse created = webTestClient.post()
                    .uri("/api/v1/products")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .exchange()
                    .expectStatus().isCreated()
                    .expectBody(ProductResponse.class)
                    .returnResult()
                    .getResponseBody();

            assertThat(created).isNotNull();

            // Now fetch by ID
            webTestClient.get()
                    .uri("/api/v1/products/{id}", created.getId())
                    .accept(MediaType.APPLICATION_JSON)
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody(ProductResponse.class)
                    .value(response -> {
                        assertThat(response.getId()).isEqualTo(created.getId());
                        assertThat(response.getSku()).isEqualTo(uniqueSku);
                        assertThat(response.getName()).isEqualTo("Get By ID Test Product");
                    });
        }

        @Test
        @Order(4)
        @DisplayName("should return 404 when getting non-existent product")
        void shouldReturn404WhenGettingNonExistentProduct() {
            webTestClient.get()
                    .uri("/api/v1/products/{id}", 999999L)
                    .accept(MediaType.APPLICATION_JSON)
                    .exchange()
                    .expectStatus().isNotFound();
        }
    }

    @Nested
    @DisplayName("Update Operations")
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class UpdateTests {

        @Test
        @Order(1)
        @DisplayName("should update existing product")
        void shouldUpdateExistingProduct() {
            // Create a product first
            String uniqueSku = "UPDATE-TEST-" + System.currentTimeMillis();
            ProductRequest createRequest = new ProductRequest()
                    .sku(uniqueSku)
                    .name("Original Name")
                    .description("Original description")
                    .quantityOnHand(30)
                    .unitPrice(29.99);

            ProductResponse created = webTestClient.post()
                    .uri("/api/v1/products")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(createRequest)
                    .exchange()
                    .expectStatus().isCreated()
                    .expectBody(ProductResponse.class)
                    .returnResult()
                    .getResponseBody();

            assertThat(created).isNotNull();

            // Update the product
            ProductRequest updateRequest = new ProductRequest()
                    .sku(uniqueSku)
                    .name("Updated Name")
                    .description("Updated description")
                    .quantityOnHand(100)
                    .unitPrice(39.99);

            webTestClient.put()
                    .uri("/api/v1/products/{id}", created.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(updateRequest)
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody(ProductResponse.class)
                    .value(response -> {
                        assertThat(response.getId()).isEqualTo(created.getId());
                        assertThat(response.getSku()).isEqualTo(uniqueSku);
                        assertThat(response.getName()).isEqualTo("Updated Name");
                        assertThat(response.getDescription()).isEqualTo("Updated description");
                        assertThat(response.getQuantityOnHand()).isEqualTo(100);
                        assertThat(response.getUnitPrice()).isEqualTo(39.99);
                    });
        }

        @Test
        @Order(2)
        @DisplayName("should return 404 when updating non-existent product")
        void shouldReturn404WhenUpdatingNonExistentProduct() {
            ProductRequest updateRequest = new ProductRequest()
                    .sku("NON-EXISTENT-SKU")
                    .name("Updated Name")
                    .quantityOnHand(100)
                    .unitPrice(39.99);

            webTestClient.put()
                    .uri("/api/v1/products/{id}", 999999L)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(updateRequest)
                    .exchange()
                    .expectStatus().isNotFound();
        }
    }

    @Nested
    @DisplayName("Delete Operations")
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class DeleteTests {

        @Test
        @Order(1)
        @DisplayName("should delete existing product")
        void shouldDeleteExistingProduct() {
            // Create a product first
            String uniqueSku = "DELETE-TEST-" + System.currentTimeMillis();
            ProductRequest createRequest = new ProductRequest()
                    .sku(uniqueSku)
                    .name("Product to Delete")
                    .quantityOnHand(10)
                    .unitPrice(9.99);

            ProductResponse created = webTestClient.post()
                    .uri("/api/v1/products")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(createRequest)
                    .exchange()
                    .expectStatus().isCreated()
                    .expectBody(ProductResponse.class)
                    .returnResult()
                    .getResponseBody();

            assertThat(created).isNotNull();

            // Delete the product
            webTestClient.delete()
                    .uri("/api/v1/products/{id}", created.getId())
                    .exchange()
                    .expectStatus().isNoContent();

            // Verify it's gone
            webTestClient.get()
                    .uri("/api/v1/products/{id}", created.getId())
                    .accept(MediaType.APPLICATION_JSON)
                    .exchange()
                    .expectStatus().isNotFound();
        }

        @Test
        @Order(2)
        @DisplayName("should return 404 when deleting non-existent product")
        void shouldReturn404WhenDeletingNonExistentProduct() {
            webTestClient.delete()
                    .uri("/api/v1/products/{id}", 999999L)
                    .exchange()
                    .expectStatus().isNotFound();
        }
    }

    @Nested
    @DisplayName("List Operations")
    class ListTests {

        @Test
        @DisplayName("should list products with pagination")
        void shouldListProductsWithPagination() {
            // Create a few products
            for (int i = 0; i < 3; i++) {
                ProductRequest request = new ProductRequest()
                        .sku("LIST-TEST-" + System.currentTimeMillis() + "-" + i)
                        .name("List Test Product " + i)
                        .quantityOnHand(10 + i)
                        .unitPrice(9.99 + i);

                webTestClient.post()
                        .uri("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(request)
                        .exchange()
                        .expectStatus().isCreated();
            }

            // List products
            webTestClient.get()
                    .uri("/api/v1/products?page=0&size=10")
                    .accept(MediaType.APPLICATION_JSON)
                    .exchange()
                    .expectStatus().isOk();
        }
    }
}
