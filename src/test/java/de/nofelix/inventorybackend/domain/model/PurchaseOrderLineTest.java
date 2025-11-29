package de.nofelix.inventorybackend.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for the PurchaseOrderLine domain model.
 */
@DisplayName("PurchaseOrderLine Domain Model")
class PurchaseOrderLineTest {

    @Nested
    @DisplayName("calculateLineTotal")
    class CalculateLineTotalTests {

        @Test
        @DisplayName("should calculate total correctly")
        void calculateLineTotal_withValidData_returnsCorrectTotal() {
            // given
            PurchaseOrderLine line = PurchaseOrderLine.builder()
                    .quantity(10)
                    .unitPrice(new BigDecimal("5.50"))
                    .build();

            // when
            BigDecimal total = line.calculateLineTotal();

            // then
            assertThat(total).isEqualByComparingTo(new BigDecimal("55.00"));
        }

        @Test
        @DisplayName("should return zero for null quantity")
        void calculateLineTotal_withNullQuantity_returnsZero() {
            // given
            PurchaseOrderLine line = PurchaseOrderLine.builder()
                    .quantity(null)
                    .unitPrice(new BigDecimal("5.50"))
                    .build();

            // when
            BigDecimal total = line.calculateLineTotal();

            // then
            assertThat(total).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("should return zero for null unit price")
        void calculateLineTotal_withNullUnitPrice_returnsZero() {
            // given
            PurchaseOrderLine line = PurchaseOrderLine.builder()
                    .quantity(10)
                    .unitPrice(null)
                    .build();

            // when
            BigDecimal total = line.calculateLineTotal();

            // then
            assertThat(total).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("should handle large quantities")
        void calculateLineTotal_withLargeQuantity_returnsCorrectTotal() {
            // given
            PurchaseOrderLine line = PurchaseOrderLine.builder()
                    .quantity(1000000)
                    .unitPrice(new BigDecimal("99.99"))
                    .build();

            // when
            BigDecimal total = line.calculateLineTotal();

            // then
            assertThat(total).isEqualByComparingTo(new BigDecimal("99990000.00"));
        }
    }

    @Nested
    @DisplayName("validate")
    class ValidateTests {

        @Test
        @DisplayName("should pass for valid line")
        void validate_validLine_doesNotThrow() {
            // given
            PurchaseOrderLine line = PurchaseOrderLine.builder()
                    .productId(1L)
                    .quantity(10)
                    .unitPrice(new BigDecimal("5.00"))
                    .build();

            // when/then
            line.validate(); // Should not throw
        }

        @Test
        @DisplayName("should throw for missing product ID")
        void validate_missingProductId_throwsException() {
            // given
            PurchaseOrderLine line = PurchaseOrderLine.builder()
                    .productId(null)
                    .quantity(10)
                    .unitPrice(new BigDecimal("5.00"))
                    .build();

            // when/then
            assertThatThrownBy(() -> line.validate())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Product ID");
        }

        @Test
        @DisplayName("should throw for null quantity")
        void validate_nullQuantity_throwsException() {
            // given
            PurchaseOrderLine line = PurchaseOrderLine.builder()
                    .productId(1L)
                    .quantity(null)
                    .unitPrice(new BigDecimal("5.00"))
                    .build();

            // when/then
            assertThatThrownBy(() -> line.validate())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Quantity");
        }

        @Test
        @DisplayName("should throw for zero quantity")
        void validate_zeroQuantity_throwsException() {
            // given
            PurchaseOrderLine line = PurchaseOrderLine.builder()
                    .productId(1L)
                    .quantity(0)
                    .unitPrice(new BigDecimal("5.00"))
                    .build();

            // when/then
            assertThatThrownBy(() -> line.validate())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Quantity");
        }

        @Test
        @DisplayName("should throw for negative quantity")
        void validate_negativeQuantity_throwsException() {
            // given
            PurchaseOrderLine line = PurchaseOrderLine.builder()
                    .productId(1L)
                    .quantity(-5)
                    .unitPrice(new BigDecimal("5.00"))
                    .build();

            // when/then
            assertThatThrownBy(() -> line.validate())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Quantity");
        }

        @Test
        @DisplayName("should throw for null unit price")
        void validate_nullUnitPrice_throwsException() {
            // given
            PurchaseOrderLine line = PurchaseOrderLine.builder()
                    .productId(1L)
                    .quantity(10)
                    .unitPrice(null)
                    .build();

            // when/then
            assertThatThrownBy(() -> line.validate())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Unit price");
        }

        @Test
        @DisplayName("should throw for negative unit price")
        void validate_negativeUnitPrice_throwsException() {
            // given
            PurchaseOrderLine line = PurchaseOrderLine.builder()
                    .productId(1L)
                    .quantity(10)
                    .unitPrice(new BigDecimal("-5.00"))
                    .build();

            // when/then
            assertThatThrownBy(() -> line.validate())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Unit price");
        }

        @Test
        @DisplayName("should allow zero unit price")
        void validate_zeroUnitPrice_doesNotThrow() {
            // given
            PurchaseOrderLine line = PurchaseOrderLine.builder()
                    .productId(1L)
                    .quantity(10)
                    .unitPrice(BigDecimal.ZERO)
                    .build();

            // when/then
            line.validate(); // Should not throw - zero price is valid (free items)
        }
    }
}
