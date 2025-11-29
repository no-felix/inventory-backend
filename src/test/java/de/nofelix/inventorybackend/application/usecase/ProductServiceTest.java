package de.nofelix.inventorybackend.application.usecase;

import de.nofelix.inventorybackend.domain.exception.DuplicateSkuException;
import de.nofelix.inventorybackend.domain.exception.ProductNotFoundException;
import de.nofelix.inventorybackend.domain.model.Product;
import de.nofelix.inventorybackend.domain.port.in.CreateProductUseCase.CreateProductCommand;
import de.nofelix.inventorybackend.domain.port.in.UpdateProductUseCase.UpdateProductCommand;
import de.nofelix.inventorybackend.domain.port.out.ProductRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for ProductService.
 * 
 * <p>Tests use case implementations with mocked repository port.</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ProductService")
class ProductServiceTest {

    @Mock
    private ProductRepositoryPort productRepository;

    @InjectMocks
    private ProductService productService;

    @Captor
    private ArgumentCaptor<Product> productCaptor;

    private Product sampleProduct;

    @BeforeEach
    void setUp() {
        sampleProduct = Product.builder()
                .id(1L)
                .sku("SKU-001")
                .name("Test Product")
                .description("A test product")
                .quantityOnHand(100)
                .unitPrice(new BigDecimal("29.99"))
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    @Nested
    @DisplayName("getProductById")
    class GetProductByIdTests {

        @Test
        @DisplayName("should return product when found")
        void shouldReturnProductWhenFound() {
            // given
            when(productRepository.findById(1L)).thenReturn(Mono.just(sampleProduct));

            // when/then
            StepVerifier.create(productService.getProductById(1L))
                    .assertNext(product -> {
                        assertThat(product.getId()).isEqualTo(1L);
                        assertThat(product.getSku()).isEqualTo("SKU-001");
                        assertThat(product.getName()).isEqualTo("Test Product");
                    })
                    .verifyComplete();

            verify(productRepository).findById(1L);
        }

        @Test
        @DisplayName("should throw ProductNotFoundException when not found")
        void shouldThrowProductNotFoundExceptionWhenNotFound() {
            // given
            when(productRepository.findById(999L)).thenReturn(Mono.empty());

            // when/then
            StepVerifier.create(productService.getProductById(999L))
                    .expectErrorSatisfies(error -> {
                        assertThat(error).isInstanceOf(ProductNotFoundException.class);
                        assertThat(((ProductNotFoundException) error).getProductId()).isEqualTo(999L);
                    })
                    .verify();
        }
    }

    @Nested
    @DisplayName("getProductBySku")
    class GetProductBySkuTests {

        @Test
        @DisplayName("should return product when found by SKU")
        void shouldReturnProductWhenFoundBySku() {
            // given
            when(productRepository.findBySku("SKU-001")).thenReturn(Mono.just(sampleProduct));

            // when/then
            StepVerifier.create(productService.getProductBySku("SKU-001"))
                    .assertNext(product -> {
                        assertThat(product.getSku()).isEqualTo("SKU-001");
                    })
                    .verifyComplete();
        }

        @Test
        @DisplayName("should throw ProductNotFoundException when SKU not found")
        void shouldThrowProductNotFoundExceptionWhenSkuNotFound() {
            // given
            when(productRepository.findBySku("INVALID-SKU")).thenReturn(Mono.empty());

            // when/then
            StepVerifier.create(productService.getProductBySku("INVALID-SKU"))
                    .expectErrorSatisfies(error -> {
                        assertThat(error).isInstanceOf(ProductNotFoundException.class);
                        assertThat(error.getMessage()).contains("INVALID-SKU");
                    })
                    .verify();
        }
    }

    @Nested
    @DisplayName("listProducts")
    class ListProductsTests {

        @Test
        @DisplayName("should return paginated products")
        void shouldReturnPaginatedProducts() {
            // given
            Product product1 = Product.builder().id(1L).sku("SKU-001").name("Product 1").build();
            Product product2 = Product.builder().id(2L).sku("SKU-002").name("Product 2").build();
            
            when(productRepository.findAll(0, 10)).thenReturn(Flux.just(product1, product2));

            // when/then
            StepVerifier.create(productService.listProducts(0, 10))
                    .assertNext(product -> assertThat(product.getSku()).isEqualTo("SKU-001"))
                    .assertNext(product -> assertThat(product.getSku()).isEqualTo("SKU-002"))
                    .verifyComplete();
        }

        @Test
        @DisplayName("should return empty flux when no products exist")
        void shouldReturnEmptyFluxWhenNoProductsExist() {
            // given
            when(productRepository.findAll(0, 10)).thenReturn(Flux.empty());

            // when/then
            StepVerifier.create(productService.listProducts(0, 10))
                    .verifyComplete();
        }
    }

    @Nested
    @DisplayName("createProduct")
    class CreateProductTests {

        @Test
        @DisplayName("should create product when SKU is unique")
        void shouldCreateProductWhenSkuIsUnique() {
            // given
            CreateProductCommand command = new CreateProductCommand(
                    "NEW-SKU",
                    "New Product",
                    "Description",
                    50,
                    new BigDecimal("19.99")
            );

            when(productRepository.existsBySku("NEW-SKU")).thenReturn(Mono.just(false));
            when(productRepository.save(any(Product.class))).thenAnswer(invocation -> {
                Product saved = invocation.getArgument(0);
                return Mono.just(Product.builder()
                        .id(1L)
                        .sku(saved.getSku())
                        .name(saved.getName())
                        .description(saved.getDescription())
                        .quantityOnHand(saved.getQuantityOnHand())
                        .unitPrice(saved.getUnitPrice())
                        .createdAt(Instant.now())
                        .build());
            });

            // when/then
            StepVerifier.create(productService.createProduct(command))
                    .assertNext(product -> {
                        assertThat(product.getId()).isEqualTo(1L);
                        assertThat(product.getSku()).isEqualTo("NEW-SKU");
                        assertThat(product.getName()).isEqualTo("New Product");
                        assertThat(product.getQuantityOnHand()).isEqualTo(50);
                    })
                    .verifyComplete();

            verify(productRepository).save(productCaptor.capture());
            Product capturedProduct = productCaptor.getValue();
            assertThat(capturedProduct.getSku()).isEqualTo("NEW-SKU");
            assertThat(capturedProduct.getName()).isEqualTo("New Product");
        }

        @Test
        @DisplayName("should throw DuplicateSkuException when SKU exists")
        void shouldThrowDuplicateSkuExceptionWhenSkuExists() {
            // given
            CreateProductCommand command = new CreateProductCommand(
                    "EXISTING-SKU",
                    "New Product",
                    "Description",
                    50,
                    new BigDecimal("19.99")
            );

            when(productRepository.existsBySku("EXISTING-SKU")).thenReturn(Mono.just(true));

            // when/then
            StepVerifier.create(productService.createProduct(command))
                    .expectErrorSatisfies(error -> {
                        assertThat(error).isInstanceOf(DuplicateSkuException.class);
                        assertThat(((DuplicateSkuException) error).getSku()).isEqualTo("EXISTING-SKU");
                    })
                    .verify();

            verify(productRepository, never()).save(any(Product.class));
        }
    }

    @Nested
    @DisplayName("updateProduct")
    class UpdateProductTests {

        @Test
        @DisplayName("should update product when found")
        void shouldUpdateProductWhenFound() {
            // given
            UpdateProductCommand command = new UpdateProductCommand(
                    "SKU-001",
                    "Updated Product",
                    "Updated description",
                    150,
                    new BigDecimal("39.99")
            );

            when(productRepository.findById(1L)).thenReturn(Mono.just(sampleProduct));
            when(productRepository.save(any(Product.class))).thenAnswer(invocation -> 
                    Mono.just(invocation.getArgument(0)));

            // when/then
            StepVerifier.create(productService.updateProduct(1L, command))
                    .assertNext(product -> {
                        assertThat(product.getName()).isEqualTo("Updated Product");
                        assertThat(product.getDescription()).isEqualTo("Updated description");
                        assertThat(product.getQuantityOnHand()).isEqualTo(150);
                        assertThat(product.getUnitPrice()).isEqualByComparingTo(new BigDecimal("39.99"));
                    })
                    .verifyComplete();
        }

        @Test
        @DisplayName("should throw ProductNotFoundException when updating non-existent product")
        void shouldThrowProductNotFoundExceptionWhenUpdatingNonExistent() {
            // given
            UpdateProductCommand command = new UpdateProductCommand(
                    "SKU-999",
                    "Updated Product",
                    "Updated description",
                    150,
                    new BigDecimal("39.99")
            );

            when(productRepository.findById(999L)).thenReturn(Mono.empty());

            // when/then
            StepVerifier.create(productService.updateProduct(999L, command))
                    .expectError(ProductNotFoundException.class)
                    .verify();

            verify(productRepository, never()).save(any(Product.class));
        }

        @Test
        @DisplayName("should throw DuplicateSkuException when changing SKU to existing one")
        void shouldThrowDuplicateSkuExceptionWhenChangingSkuToExisting() {
            // given
            UpdateProductCommand command = new UpdateProductCommand(
                    "EXISTING-SKU",  // Different from sampleProduct's SKU
                    "Updated Product",
                    "Updated description",
                    150,
                    new BigDecimal("39.99")
            );

            when(productRepository.findById(1L)).thenReturn(Mono.just(sampleProduct));
            when(productRepository.existsBySku("EXISTING-SKU")).thenReturn(Mono.just(true));

            // when/then
            StepVerifier.create(productService.updateProduct(1L, command))
                    .expectErrorSatisfies(error -> {
                        assertThat(error).isInstanceOf(DuplicateSkuException.class);
                        assertThat(((DuplicateSkuException) error).getSku()).isEqualTo("EXISTING-SKU");
                    })
                    .verify();

            verify(productRepository, never()).save(any(Product.class));
        }

        @Test
        @DisplayName("should allow keeping same SKU on update")
        void shouldAllowKeepingSameSkuOnUpdate() {
            // given
            UpdateProductCommand command = new UpdateProductCommand(
                    "SKU-001",  // Same as sampleProduct's SKU
                    "Updated Product",
                    "Updated description",
                    150,
                    new BigDecimal("39.99")
            );

            when(productRepository.findById(1L)).thenReturn(Mono.just(sampleProduct));
            when(productRepository.save(any(Product.class))).thenAnswer(invocation -> 
                    Mono.just(invocation.getArgument(0)));

            // when/then
            StepVerifier.create(productService.updateProduct(1L, command))
                    .assertNext(product -> {
                        assertThat(product.getSku()).isEqualTo("SKU-001");
                        assertThat(product.getName()).isEqualTo("Updated Product");
                    })
                    .verifyComplete();

            // Should NOT check existsBySku when SKU is unchanged
            verify(productRepository, never()).existsBySku(anyString());
        }
    }

    @Nested
    @DisplayName("deleteProduct")
    class DeleteProductTests {

        @Test
        @DisplayName("should delete product when exists")
        void shouldDeleteProductWhenExists() {
            // given
            when(productRepository.existsById(1L)).thenReturn(Mono.just(true));
            when(productRepository.deleteById(1L)).thenReturn(Mono.empty());

            // when/then
            StepVerifier.create(productService.deleteProduct(1L))
                    .verifyComplete();

            verify(productRepository).deleteById(1L);
        }

        @Test
        @DisplayName("should throw ProductNotFoundException when deleting non-existent product")
        void shouldThrowProductNotFoundExceptionWhenDeletingNonExistent() {
            // given
            when(productRepository.existsById(999L)).thenReturn(Mono.just(false));

            // when/then
            StepVerifier.create(productService.deleteProduct(999L))
                    .expectError(ProductNotFoundException.class)
                    .verify();

            verify(productRepository, never()).deleteById(anyLong());
        }
    }
}
