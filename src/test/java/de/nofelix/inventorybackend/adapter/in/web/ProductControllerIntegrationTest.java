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
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
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
@AutoConfigureWebTestClient
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
        void createProduct_withValidRequest_returns201WithCreatedProduct() {
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
        void createProduct_withDuplicateSku_returns409Conflict() {
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
        void getProductById_afterCreation_returns200WithProduct() {
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
        void getProductById_withNonExistentId_returns404() {
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
        void updateProduct_withValidRequest_returns200WithUpdatedProduct() {
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
        void updateProduct_withNonExistentId_returns404() {
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
        void deleteProduct_withExistingId_returns204AndRemovesProduct() {
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

            webTestClient.delete()
                    .uri("/api/v1/products/{id}", created.getId())
                    .exchange()
                    .expectStatus().isNoContent();

            webTestClient.get()
                    .uri("/api/v1/products/{id}", created.getId())
                    .accept(MediaType.APPLICATION_JSON)
                    .exchange()
                    .expectStatus().isNotFound();
        }

        @Test
        @Order(2)
        @DisplayName("should return 404 when deleting non-existent product")
        void deleteProduct_withNonExistentId_returns404() {
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
        void listProducts_withPagination_returns200WithProductList() {
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

            webTestClient.get()
                    .uri("/api/v1/products?page=0&size=10")
                    .accept(MediaType.APPLICATION_JSON)
                    .exchange()
                    .expectStatus().isOk();
        }
    }

    @Nested
    @DisplayName("Validation Error Handling")
    class ValidationTests {

        @Test
        @DisplayName("should return 400 when creating product without SKU")
        void createProduct_withoutSku_returns400WithValidationError() {
            ProductRequest request = new ProductRequest()
                    .name("Product without SKU")
                    .quantityOnHand(10)
                    .unitPrice(9.99);

            webTestClient.post()
                    .uri("/api/v1/products")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .exchange()
                    .expectStatus().isBadRequest()
                    .expectBody()
                    .jsonPath("$.type").isEqualTo("https://api.inventory.example.com/errors/validation-error")
                    .jsonPath("$.title").isEqualTo("Validation Error")
                    .jsonPath("$.status").isEqualTo(400)
                    .jsonPath("$.errors").isArray()
                    .jsonPath("$.errors[?(@.field == 'sku')]").exists();
        }

        @Test
        @DisplayName("should return 400 when creating product without name")
        void createProduct_withoutName_returns400WithValidationError() {
            ProductRequest request = new ProductRequest()
                    .sku("VALID-SKU-" + System.currentTimeMillis())
                    .quantityOnHand(10)
                    .unitPrice(9.99);

            webTestClient.post()
                    .uri("/api/v1/products")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .exchange()
                    .expectStatus().isBadRequest()
                    .expectBody()
                    .jsonPath("$.type").isEqualTo("https://api.inventory.example.com/errors/validation-error")
                    .jsonPath("$.errors[?(@.field == 'name')]").exists();
        }

        @Test
        @DisplayName("should return 400 when creating product with negative quantity")
        void createProduct_withNegativeQuantity_returns400WithValidationError() {
            ProductRequest request = new ProductRequest()
                    .sku("NEGATIVE-QTY-" + System.currentTimeMillis())
                    .name("Product with negative quantity")
                    .quantityOnHand(-5)
                    .unitPrice(9.99);

            webTestClient.post()
                    .uri("/api/v1/products")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .exchange()
                    .expectStatus().isBadRequest()
                    .expectBody()
                    .jsonPath("$.type").isEqualTo("https://api.inventory.example.com/errors/validation-error")
                    .jsonPath("$.errors[?(@.field == 'quantityOnHand')]").exists();
        }

        @Test
        @DisplayName("should return 400 when creating product with negative price")
        void createProduct_withNegativePrice_returns400WithValidationError() {
            ProductRequest request = new ProductRequest()
                    .sku("NEGATIVE-PRICE-" + System.currentTimeMillis())
                    .name("Product with negative price")
                    .quantityOnHand(10)
                    .unitPrice(-9.99);

            webTestClient.post()
                    .uri("/api/v1/products")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .exchange()
                    .expectStatus().isBadRequest()
                    .expectBody()
                    .jsonPath("$.type").isEqualTo("https://api.inventory.example.com/errors/validation-error")
                    .jsonPath("$.errors[?(@.field == 'unitPrice')]").exists();
        }

        @Test
        @DisplayName("should return 400 when creating product with empty SKU")
        void createProduct_withEmptySku_returns400WithValidationError() {
            ProductRequest request = new ProductRequest()
                    .sku("")
                    .name("Product with empty SKU")
                    .quantityOnHand(10)
                    .unitPrice(9.99);

            webTestClient.post()
                    .uri("/api/v1/products")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .exchange()
                    .expectStatus().isBadRequest()
                    .expectBody()
                    .jsonPath("$.type").isEqualTo("https://api.inventory.example.com/errors/validation-error")
                    .jsonPath("$.errors[?(@.field == 'sku')]").exists();
        }

        @Test
        @DisplayName("should return 400 when creating product with SKU exceeding max length")
        void createProduct_withSkuExceedingMaxLength_returns400WithValidationError() {
            ProductRequest request = new ProductRequest()
                    .sku("A".repeat(51)) // Max is 50
                    .name("Product with long SKU")
                    .quantityOnHand(10)
                    .unitPrice(9.99);

            webTestClient.post()
                    .uri("/api/v1/products")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .exchange()
                    .expectStatus().isBadRequest()
                    .expectBody()
                    .jsonPath("$.type").isEqualTo("https://api.inventory.example.com/errors/validation-error")
                    .jsonPath("$.errors[?(@.field == 'sku')]").exists();
        }

        @Test
        @DisplayName("should return 400 when updating product with invalid data")
        void updateProduct_withNegativeQuantity_returns400WithValidationError() {
            String uniqueSku = "VALID-UPDATE-" + System.currentTimeMillis();
            ProductRequest createRequest = new ProductRequest()
                    .sku(uniqueSku)
                    .name("Valid Product")
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

            ProductRequest invalidUpdate = new ProductRequest()
                    .sku(uniqueSku)
                    .name("Updated Product")
                    .quantityOnHand(-100)
                    .unitPrice(9.99);

            webTestClient.put()
                    .uri("/api/v1/products/{id}", created.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(invalidUpdate)
                    .exchange()
                    .expectStatus().isBadRequest()
                    .expectBody()
                    .jsonPath("$.type").isEqualTo("https://api.inventory.example.com/errors/validation-error")
                    .jsonPath("$.errors[?(@.field == 'quantityOnHand')]").exists();
        }

        @Test
        @DisplayName("should return 400 with multiple validation errors")
        void createProduct_withMultipleInvalidFields_returns400WithMultipleErrors() {
            ProductRequest request = new ProductRequest()
                    .quantityOnHand(-10)
                    .unitPrice(-5.0);

            webTestClient.post()
                    .uri("/api/v1/products")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .exchange()
                    .expectStatus().isBadRequest()
                    .expectBody()
                    .jsonPath("$.type").isEqualTo("https://api.inventory.example.com/errors/validation-error")
                    .jsonPath("$.title").isEqualTo("Validation Error")
                    .jsonPath("$.errors").isArray()
                    .jsonPath("$.errors.length()").value(count -> assertThat((Integer) count).isGreaterThanOrEqualTo(4));
        }
    }
}
