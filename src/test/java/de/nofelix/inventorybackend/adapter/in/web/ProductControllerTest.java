package de.nofelix.inventorybackend.adapter.in.web;

import de.nofelix.inventorybackend.adapter.in.web.model.ProductRequest;
import de.nofelix.inventorybackend.adapter.in.web.model.ProductResponse;
import de.nofelix.inventorybackend.application.mapper.ProductMapper;
import de.nofelix.inventorybackend.domain.exception.DuplicateSkuException;
import de.nofelix.inventorybackend.domain.exception.ProductNotFoundException;
import de.nofelix.inventorybackend.domain.model.Product;
import de.nofelix.inventorybackend.domain.port.in.CreateProductUseCase;
import de.nofelix.inventorybackend.domain.port.in.CreateProductUseCase.CreateProductCommand;
import de.nofelix.inventorybackend.domain.port.in.DeleteProductUseCase;
import de.nofelix.inventorybackend.domain.port.in.GetProductUseCase;
import de.nofelix.inventorybackend.domain.port.in.UpdateProductUseCase;
import de.nofelix.inventorybackend.domain.port.in.UpdateProductUseCase.UpdateProductCommand;
import de.nofelix.inventorybackend.infrastructure.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for ProductController.
 * 
 * <p>Tests REST endpoints with mocked use cases using WebTestClient.</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ProductController")
class ProductControllerTest {

    private WebTestClient webTestClient;

    @Mock
    private GetProductUseCase getProductUseCase;

    @Mock
    private CreateProductUseCase createProductUseCase;

    @Mock
    private UpdateProductUseCase updateProductUseCase;

    @Mock
    private DeleteProductUseCase deleteProductUseCase;

    @Mock
    private ProductMapper productMapper;

    private Product sampleProduct;
    private ProductResponse sampleResponse;
    private ProductRequest sampleRequest;

    @BeforeEach
    void setUp() {
        ProductController controller = new ProductController(
                getProductUseCase,
                createProductUseCase,
                updateProductUseCase,
                deleteProductUseCase,
                productMapper
        );

        webTestClient = WebTestClient.bindToController(controller)
                .controllerAdvice(new GlobalExceptionHandler())
                .build();

        Instant now = Instant.now();
        OffsetDateTime nowOffset = now.atOffset(ZoneOffset.UTC);

        sampleProduct = Product.builder()
                .id(1L)
                .sku("SKU-001")
                .name("Test Product")
                .description("A test product")
                .quantityOnHand(100)
                .unitPrice(new BigDecimal("29.99"))
                .createdAt(now)
                .updatedAt(now)
                .build();

        sampleResponse = new ProductResponse()
                .id(1L)
                .sku("SKU-001")
                .name("Test Product")
                .description("A test product")
                .quantityOnHand(100)
                .unitPrice(29.99)
                .createdAt(nowOffset)
                .updatedAt(nowOffset);

        sampleRequest = new ProductRequest()
                .sku("SKU-001")
                .name("Test Product")
                .description("A test product")
                .quantityOnHand(100)
                .unitPrice(29.99);
    }

    @Nested
    @DisplayName("GET /api/v1/products/{id}")
    class GetProductByIdTests {

        @Test
        @DisplayName("should return 200 with product when found")
        void shouldReturn200WithProductWhenFound() {
            // given
            when(getProductUseCase.getProductById(1L)).thenReturn(Mono.just(sampleProduct));
            when(productMapper.toResponse(sampleProduct)).thenReturn(sampleResponse);

            // when/then
            webTestClient.get()
                    .uri("/api/v1/products/1")
                    .accept(MediaType.APPLICATION_JSON)
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody(ProductResponse.class)
                    .value(response -> {
                        org.assertj.core.api.Assertions.assertThat(response.getId()).isEqualTo(1L);
                        org.assertj.core.api.Assertions.assertThat(response.getSku()).isEqualTo("SKU-001");
                        org.assertj.core.api.Assertions.assertThat(response.getName()).isEqualTo("Test Product");
                    });

            verify(getProductUseCase).getProductById(1L);
        }

        @Test
        @DisplayName("should return 404 when product not found")
        void shouldReturn404WhenProductNotFound() {
            // given
            when(getProductUseCase.getProductById(999L))
                    .thenReturn(Mono.error(new ProductNotFoundException(999L)));

            // when/then
            webTestClient.get()
                    .uri("/api/v1/products/999")
                    .accept(MediaType.APPLICATION_JSON)
                    .exchange()
                    .expectStatus().isNotFound();
        }
    }

    @Nested
    @DisplayName("GET /api/v1/products")
    class ListProductsTests {

        @Test
        @DisplayName("should return 200 with list of products")
        void shouldReturn200WithListOfProducts() {
            // given
            Product product1 = Product.builder().id(1L).sku("SKU-001").name("Product 1").build();
            Product product2 = Product.builder().id(2L).sku("SKU-002").name("Product 2").build();

            when(getProductUseCase.listProducts(anyInt(), anyInt()))
                    .thenReturn(Flux.just(product1, product2));
            // Use lenient stubbing since Flux elements may or may not be consumed based on test client behavior
            lenient().when(productMapper.toResponse(any(Product.class))).thenAnswer(invocation -> {
                Product p = invocation.getArgument(0);
                return new ProductResponse().id(p.getId()).sku(p.getSku()).name(p.getName());
            });

            // when/then
            webTestClient.get()
                    .uri("/api/v1/products?page=0&size=10")
                    .accept(MediaType.APPLICATION_JSON)
                    .exchange()
                    .expectStatus().isOk();

            verify(getProductUseCase).listProducts(0, 10);
        }

        @Test
        @DisplayName("should return 200 with empty list when no products")
        void shouldReturn200WithEmptyListWhenNoProducts() {
            // given
            when(getProductUseCase.listProducts(anyInt(), anyInt())).thenReturn(Flux.empty());

            // when/then
            webTestClient.get()
                    .uri("/api/v1/products")
                    .accept(MediaType.APPLICATION_JSON)
                    .exchange()
                    .expectStatus().isOk();
        }
    }

    @Nested
    @DisplayName("POST /api/v1/products")
    class CreateProductTests {

        @Test
        @DisplayName("should return 201 when product created successfully")
        void shouldReturn201WhenProductCreatedSuccessfully() {
            // given
            CreateProductCommand command = new CreateProductCommand(
                    "SKU-001", "Test Product", "A test product", 100, new BigDecimal("29.99")
            );

            when(productMapper.toCreateCommand(any(ProductRequest.class))).thenReturn(command);
            when(createProductUseCase.createProduct(any(CreateProductCommand.class)))
                    .thenReturn(Mono.just(sampleProduct));
            when(productMapper.toResponse(sampleProduct)).thenReturn(sampleResponse);

            // when/then
            webTestClient.post()
                    .uri("/api/v1/products")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(sampleRequest)
                    .exchange()
                    .expectStatus().isCreated()
                    .expectBody(ProductResponse.class)
                    .value(response -> {
                        org.assertj.core.api.Assertions.assertThat(response.getId()).isEqualTo(1L);
                        org.assertj.core.api.Assertions.assertThat(response.getSku()).isEqualTo("SKU-001");
                    });
        }

        @Test
        @DisplayName("should return 409 when SKU already exists")
        void shouldReturn409WhenSkuAlreadyExists() {
            // given
            CreateProductCommand command = new CreateProductCommand(
                    "EXISTING-SKU", "Test Product", "A test product", 100, new BigDecimal("29.99")
            );

            ProductRequest request = new ProductRequest()
                    .sku("EXISTING-SKU")
                    .name("Test Product")
                    .quantityOnHand(100)
                    .unitPrice(29.99);

            when(productMapper.toCreateCommand(any(ProductRequest.class))).thenReturn(command);
            when(createProductUseCase.createProduct(any(CreateProductCommand.class)))
                    .thenReturn(Mono.error(new DuplicateSkuException("EXISTING-SKU")));

            // when/then
            webTestClient.post()
                    .uri("/api/v1/products")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .exchange()
                    .expectStatus().isEqualTo(409);
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/products/{id}")
    class UpdateProductTests {

        @Test
        @DisplayName("should return 200 when product updated successfully")
        void shouldReturn200WhenProductUpdatedSuccessfully() {
            // given
            UpdateProductCommand command = new UpdateProductCommand(
                    "SKU-001", "Updated Product", "Updated description", 150, new BigDecimal("39.99")
            );

            Product updatedProduct = Product.builder()
                    .id(1L)
                    .sku("SKU-001")
                    .name("Updated Product")
                    .description("Updated description")
                    .quantityOnHand(150)
                    .unitPrice(new BigDecimal("39.99"))
                    .build();

            ProductResponse updatedResponse = new ProductResponse()
                    .id(1L)
                    .sku("SKU-001")
                    .name("Updated Product")
                    .description("Updated description")
                    .quantityOnHand(150)
                    .unitPrice(39.99);

            when(productMapper.toUpdateCommand(any(ProductRequest.class))).thenReturn(command);
            when(updateProductUseCase.updateProduct(eq(1L), any(UpdateProductCommand.class)))
                    .thenReturn(Mono.just(updatedProduct));
            when(productMapper.toResponse(updatedProduct)).thenReturn(updatedResponse);

            ProductRequest updateRequest = new ProductRequest()
                    .sku("SKU-001")
                    .name("Updated Product")
                    .description("Updated description")
                    .quantityOnHand(150)
                    .unitPrice(39.99);

            // when/then
            webTestClient.put()
                    .uri("/api/v1/products/1")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(updateRequest)
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody(ProductResponse.class)
                    .value(response -> {
                        org.assertj.core.api.Assertions.assertThat(response.getName()).isEqualTo("Updated Product");
                        org.assertj.core.api.Assertions.assertThat(response.getQuantityOnHand()).isEqualTo(150);
                    });
        }

        @Test
        @DisplayName("should return 404 when updating non-existent product")
        void shouldReturn404WhenUpdatingNonExistentProduct() {
            // given
            UpdateProductCommand command = new UpdateProductCommand(
                    "SKU-001", "Updated Product", "Updated description", 150, new BigDecimal("39.99")
            );

            when(productMapper.toUpdateCommand(any(ProductRequest.class))).thenReturn(command);
            when(updateProductUseCase.updateProduct(eq(999L), any(UpdateProductCommand.class)))
                    .thenReturn(Mono.error(new ProductNotFoundException(999L)));

            // when/then
            webTestClient.put()
                    .uri("/api/v1/products/999")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(sampleRequest)
                    .exchange()
                    .expectStatus().isNotFound();
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/products/{id}")
    class DeleteProductTests {

        @Test
        @DisplayName("should return 204 when product deleted successfully")
        void shouldReturn204WhenProductDeletedSuccessfully() {
            // given
            when(deleteProductUseCase.deleteProduct(1L)).thenReturn(Mono.empty());

            // when/then
            webTestClient.delete()
                    .uri("/api/v1/products/1")
                    .exchange()
                    .expectStatus().isNoContent();

            verify(deleteProductUseCase).deleteProduct(1L);
        }

        @Test
        @DisplayName("should return 404 when deleting non-existent product")
        void shouldReturn404WhenDeletingNonExistentProduct() {
            // given
            when(deleteProductUseCase.deleteProduct(999L))
                    .thenReturn(Mono.error(new ProductNotFoundException(999L)));

            // when/then
            webTestClient.delete()
                    .uri("/api/v1/products/999")
                    .exchange()
                    .expectStatus().isNotFound();
        }
    }
}
