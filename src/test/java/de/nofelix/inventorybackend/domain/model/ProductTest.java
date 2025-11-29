package de.nofelix.inventorybackend.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for the Product domain model.
 * 
 * <p>Tests business logic methods without any external dependencies.</p>
 */
@DisplayName("Product Domain Model")
class ProductTest {

    @Nested
    @DisplayName("increaseStock")
    class IncreaseStockTests {

        @Test
        @DisplayName("should increase quantity by given amount")
        void shouldIncreaseQuantityByGivenAmount() {
            // given
            Product product = createProductWithQuantity(10);

            // when
            product.increaseStock(5);

            // then
            assertThat(product.getQuantityOnHand()).isEqualTo(15);
        }

        @Test
        @DisplayName("should increase from zero quantity")
        void shouldIncreaseFromZeroQuantity() {
            // given
            Product product = createProductWithQuantity(0);

            // when
            product.increaseStock(100);

            // then
            assertThat(product.getQuantityOnHand()).isEqualTo(100);
        }

        @Test
        @DisplayName("should throw exception when quantity is zero")
        void shouldThrowExceptionWhenQuantityIsZero() {
            // given
            Product product = createProductWithQuantity(10);

            // when/then
            assertThatThrownBy(() -> product.increaseStock(0))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Quantity must be positive");
        }

        @Test
        @DisplayName("should throw exception when quantity is negative")
        void shouldThrowExceptionWhenQuantityIsNegative() {
            // given
            Product product = createProductWithQuantity(10);

            // when/then
            assertThatThrownBy(() -> product.increaseStock(-5))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Quantity must be positive");
        }
    }

    @Nested
    @DisplayName("decreaseStock")
    class DecreaseStockTests {

        @Test
        @DisplayName("should decrease quantity by given amount")
        void shouldDecreaseQuantityByGivenAmount() {
            // given
            Product product = createProductWithQuantity(10);

            // when
            product.decreaseStock(3);

            // then
            assertThat(product.getQuantityOnHand()).isEqualTo(7);
        }

        @Test
        @DisplayName("should decrease to zero")
        void shouldDecreaseToZero() {
            // given
            Product product = createProductWithQuantity(5);

            // when
            product.decreaseStock(5);

            // then
            assertThat(product.getQuantityOnHand()).isZero();
        }

        @Test
        @DisplayName("should throw exception when quantity is zero")
        void shouldThrowExceptionWhenQuantityIsZero() {
            // given
            Product product = createProductWithQuantity(10);

            // when/then
            assertThatThrownBy(() -> product.decreaseStock(0))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Quantity must be positive");
        }

        @Test
        @DisplayName("should throw exception when quantity is negative")
        void shouldThrowExceptionWhenQuantityIsNegative() {
            // given
            Product product = createProductWithQuantity(10);

            // when/then
            assertThatThrownBy(() -> product.decreaseStock(-5))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Quantity must be positive");
        }

        @Test
        @DisplayName("should throw exception when insufficient stock")
        void shouldThrowExceptionWhenInsufficientStock() {
            // given
            Product product = createProductWithQuantity(5);

            // when/then
            assertThatThrownBy(() -> product.decreaseStock(10))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Insufficient stock")
                    .hasMessageContaining("Available: 5")
                    .hasMessageContaining("Requested: 10");
        }
    }

    @Nested
    @DisplayName("calculateTotalValue")
    class CalculateTotalValueTests {

        @Test
        @DisplayName("should calculate total value correctly")
        void shouldCalculateTotalValueCorrectly() {
            // given
            Product product = Product.builder()
                    .quantityOnHand(10)
                    .unitPrice(new BigDecimal("25.50"))
                    .build();

            // when
            BigDecimal totalValue = product.calculateTotalValue();

            // then
            assertThat(totalValue).isEqualByComparingTo(new BigDecimal("255.00"));
        }

        @Test
        @DisplayName("should return zero when quantity is null")
        void shouldReturnZeroWhenQuantityIsNull() {
            // given
            Product product = Product.builder()
                    .quantityOnHand(null)
                    .unitPrice(new BigDecimal("10.00"))
                    .build();

            // when
            BigDecimal totalValue = product.calculateTotalValue();

            // then
            assertThat(totalValue).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("should return zero when unit price is null")
        void shouldReturnZeroWhenUnitPriceIsNull() {
            // given
            Product product = Product.builder()
                    .quantityOnHand(10)
                    .unitPrice(null)
                    .build();

            // when
            BigDecimal totalValue = product.calculateTotalValue();

            // then
            assertThat(totalValue).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("should return zero when quantity is zero")
        void shouldReturnZeroWhenQuantityIsZero() {
            // given
            Product product = Product.builder()
                    .quantityOnHand(0)
                    .unitPrice(new BigDecimal("100.00"))
                    .build();

            // when
            BigDecimal totalValue = product.calculateTotalValue();

            // then
            assertThat(totalValue).isEqualByComparingTo(BigDecimal.ZERO);
        }
    }

    @Nested
    @DisplayName("isLowStock")
    class IsLowStockTests {

        @Test
        @DisplayName("should return true when quantity equals threshold")
        void shouldReturnTrueWhenQuantityEqualsThreshold() {
            // given
            Product product = createProductWithQuantity(5);

            // when/then
            assertThat(product.isLowStock(5)).isTrue();
        }

        @Test
        @DisplayName("should return true when quantity is below threshold")
        void shouldReturnTrueWhenQuantityIsBelowThreshold() {
            // given
            Product product = createProductWithQuantity(3);

            // when/then
            assertThat(product.isLowStock(5)).isTrue();
        }

        @Test
        @DisplayName("should return false when quantity is above threshold")
        void shouldReturnFalseWhenQuantityIsAboveThreshold() {
            // given
            Product product = createProductWithQuantity(10);

            // when/then
            assertThat(product.isLowStock(5)).isFalse();
        }

        @Test
        @DisplayName("should return true when quantity is zero")
        void shouldReturnTrueWhenQuantityIsZero() {
            // given
            Product product = createProductWithQuantity(0);

            // when/then
            assertThat(product.isLowStock(5)).isTrue();
        }

        @Test
        @DisplayName("should return false when quantity is null")
        void shouldReturnFalseWhenQuantityIsNull() {
            // given
            Product product = Product.builder()
                    .quantityOnHand(null)
                    .build();

            // when/then
            assertThat(product.isLowStock(5)).isFalse();
        }
    }

    @Nested
    @DisplayName("Builder")
    class BuilderTests {

        @Test
        @DisplayName("should create product with all fields")
        void shouldCreateProductWithAllFields() {
            // given
            Instant now = Instant.now();

            // when
            Product product = Product.builder()
                    .id(1L)
                    .sku("SKU-001")
                    .name("Test Product")
                    .description("A test product description")
                    .quantityOnHand(100)
                    .unitPrice(new BigDecimal("49.99"))
                    .createdAt(now)
                    .updatedAt(now)
                    .build();

            // then
            assertThat(product.getId()).isEqualTo(1L);
            assertThat(product.getSku()).isEqualTo("SKU-001");
            assertThat(product.getName()).isEqualTo("Test Product");
            assertThat(product.getDescription()).isEqualTo("A test product description");
            assertThat(product.getQuantityOnHand()).isEqualTo(100);
            assertThat(product.getUnitPrice()).isEqualByComparingTo(new BigDecimal("49.99"));
            assertThat(product.getCreatedAt()).isEqualTo(now);
            assertThat(product.getUpdatedAt()).isEqualTo(now);
        }

        @Test
        @DisplayName("should create product with minimal fields")
        void shouldCreateProductWithMinimalFields() {
            // when
            Product product = Product.builder()
                    .sku("SKU-001")
                    .name("Test Product")
                    .build();

            // then
            assertThat(product.getId()).isNull();
            assertThat(product.getSku()).isEqualTo("SKU-001");
            assertThat(product.getName()).isEqualTo("Test Product");
            assertThat(product.getDescription()).isNull();
            assertThat(product.getQuantityOnHand()).isNull();
            assertThat(product.getUnitPrice()).isNull();
        }
    }

    // ========================================
    // Test Helpers
    // ========================================

    private Product createProductWithQuantity(int quantity) {
        return Product.builder()
                .id(1L)
                .sku("SKU-001")
                .name("Test Product")
                .quantityOnHand(quantity)
                .unitPrice(new BigDecimal("10.00"))
                .build();
    }
}
