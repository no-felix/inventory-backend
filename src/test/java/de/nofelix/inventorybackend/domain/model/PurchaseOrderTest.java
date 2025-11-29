package de.nofelix.inventorybackend.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for the PurchaseOrder domain model.
 */
@DisplayName("PurchaseOrder Domain Model")
class PurchaseOrderTest {

    @Nested
    @DisplayName("calculateTotalAmount")
    class CalculateTotalAmountTests {

        @Test
        @DisplayName("should calculate total from all lines")
        void calculateTotalAmount_withMultipleLines_returnsSumOfAllLineTotals() {
            // given
            PurchaseOrder order = createOrderWithLines(
                    createLine(1L, 10, new BigDecimal("5.00")),   // 50.00
                    createLine(2L, 5, new BigDecimal("20.00"))    // 100.00
            );

            // when
            BigDecimal total = order.calculateTotalAmount();

            // then
            assertThat(total).isEqualByComparingTo(new BigDecimal("150.00"));
        }

        @Test
        @DisplayName("should return zero for empty lines")
        void calculateTotalAmount_withNoLines_returnsZero() {
            // given
            PurchaseOrder order = PurchaseOrder.builder()
                    .supplierName("Test Supplier")
                    .status(PurchaseOrderStatus.PENDING)
                    .lines(new ArrayList<>())
                    .build();

            // when
            BigDecimal total = order.calculateTotalAmount();

            // then
            assertThat(total).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("should return zero for null lines")
        void calculateTotalAmount_withNullLines_returnsZero() {
            // given
            PurchaseOrder order = PurchaseOrder.builder()
                    .supplierName("Test Supplier")
                    .status(PurchaseOrderStatus.PENDING)
                    .lines(null)
                    .build();

            // when
            BigDecimal total = order.calculateTotalAmount();

            // then
            assertThat(total).isEqualByComparingTo(BigDecimal.ZERO);
        }
    }

    @Nested
    @DisplayName("addLine")
    class AddLineTests {

        @Test
        @DisplayName("should add line to pending order")
        void addLine_toPendingOrder_addsLineSuccessfully() {
            // given
            PurchaseOrder order = PurchaseOrder.builder()
                    .id(1L)
                    .supplierName("Test Supplier")
                    .status(PurchaseOrderStatus.PENDING)
                    .lines(new ArrayList<>())
                    .build();
            PurchaseOrderLine line = createLine(1L, 10, new BigDecimal("5.00"));

            // when
            order.addLine(line);

            // then
            assertThat(order.getLines()).hasSize(1);
            assertThat(order.getLines().get(0).getPurchaseOrderId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("should throw exception when adding line to received order")
        void addLine_toReceivedOrder_throwsException() {
            // given
            PurchaseOrder order = PurchaseOrder.builder()
                    .supplierName("Test Supplier")
                    .status(PurchaseOrderStatus.RECEIVED)
                    .lines(new ArrayList<>())
                    .build();
            PurchaseOrderLine line = createLine(1L, 10, new BigDecimal("5.00"));

            // when/then
            assertThatThrownBy(() -> order.addLine(line))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("non-pending");
        }
    }

    @Nested
    @DisplayName("markAsReceived")
    class MarkAsReceivedTests {

        @Test
        @DisplayName("should mark pending order as received")
        void markAsReceived_pendingOrder_changesStatusAndSetsTimestamp() {
            // given
            PurchaseOrder order = PurchaseOrder.builder()
                    .supplierName("Test Supplier")
                    .status(PurchaseOrderStatus.PENDING)
                    .build();

            // when
            order.markAsReceived();

            // then
            assertThat(order.getStatus()).isEqualTo(PurchaseOrderStatus.RECEIVED);
            assertThat(order.getReceivedAt()).isNotNull();
        }

        @Test
        @DisplayName("should throw exception when marking already received order")
        void markAsReceived_alreadyReceivedOrder_throwsException() {
            // given
            PurchaseOrder order = PurchaseOrder.builder()
                    .supplierName("Test Supplier")
                    .status(PurchaseOrderStatus.RECEIVED)
                    .build();

            // when/then
            assertThatThrownBy(() -> order.markAsReceived())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("PENDING");
        }

        @Test
        @DisplayName("should throw exception when marking cancelled order")
        void markAsReceived_cancelledOrder_throwsException() {
            // given
            PurchaseOrder order = PurchaseOrder.builder()
                    .supplierName("Test Supplier")
                    .status(PurchaseOrderStatus.CANCELLED)
                    .build();

            // when/then
            assertThatThrownBy(() -> order.markAsReceived())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("PENDING");
        }
    }

    @Nested
    @DisplayName("cancel")
    class CancelTests {

        @Test
        @DisplayName("should cancel pending order")
        void cancel_pendingOrder_changesStatusToCancelled() {
            // given
            PurchaseOrder order = PurchaseOrder.builder()
                    .supplierName("Test Supplier")
                    .status(PurchaseOrderStatus.PENDING)
                    .build();

            // when
            order.cancel();

            // then
            assertThat(order.getStatus()).isEqualTo(PurchaseOrderStatus.CANCELLED);
        }

        @Test
        @DisplayName("should throw exception when cancelling received order")
        void cancel_receivedOrder_throwsException() {
            // given
            PurchaseOrder order = PurchaseOrder.builder()
                    .supplierName("Test Supplier")
                    .status(PurchaseOrderStatus.RECEIVED)
                    .build();

            // when/then
            assertThatThrownBy(() -> order.cancel())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("received");
        }
    }

    @Nested
    @DisplayName("canBeReceived")
    class CanBeReceivedTests {

        @Test
        @DisplayName("should return true for pending order")
        void canBeReceived_pendingOrder_returnsTrue() {
            // given
            PurchaseOrder order = PurchaseOrder.builder()
                    .status(PurchaseOrderStatus.PENDING)
                    .build();

            // when/then
            assertThat(order.canBeReceived()).isTrue();
        }

        @Test
        @DisplayName("should return false for received order")
        void canBeReceived_receivedOrder_returnsFalse() {
            // given
            PurchaseOrder order = PurchaseOrder.builder()
                    .status(PurchaseOrderStatus.RECEIVED)
                    .build();

            // when/then
            assertThat(order.canBeReceived()).isFalse();
        }

        @Test
        @DisplayName("should return false for cancelled order")
        void canBeReceived_cancelledOrder_returnsFalse() {
            // given
            PurchaseOrder order = PurchaseOrder.builder()
                    .status(PurchaseOrderStatus.CANCELLED)
                    .build();

            // when/then
            assertThat(order.canBeReceived()).isFalse();
        }
    }

    @Nested
    @DisplayName("validate")
    class ValidateTests {

        @Test
        @DisplayName("should pass for valid order")
        void validate_validOrder_doesNotThrow() {
            // given
            PurchaseOrder order = createOrderWithLines(
                    createLine(1L, 10, new BigDecimal("5.00"))
            );

            // when/then
            order.validate(); // Should not throw
        }

        @Test
        @DisplayName("should throw for missing supplier name")
        void validate_missingSupplierName_throwsException() {
            // given
            PurchaseOrder order = PurchaseOrder.builder()
                    .supplierName(null)
                    .lines(new ArrayList<>())
                    .build();

            // when/then
            assertThatThrownBy(() -> order.validate())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Supplier name");
        }

        @Test
        @DisplayName("should throw for blank supplier name")
        void validate_blankSupplierName_throwsException() {
            // given
            PurchaseOrder order = PurchaseOrder.builder()
                    .supplierName("   ")
                    .lines(new ArrayList<>())
                    .build();

            // when/then
            assertThatThrownBy(() -> order.validate())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Supplier name");
        }

        @Test
        @DisplayName("should throw for empty lines")
        void validate_emptyLines_throwsException() {
            // given
            PurchaseOrder order = PurchaseOrder.builder()
                    .supplierName("Test Supplier")
                    .lines(new ArrayList<>())
                    .build();

            // when/then
            assertThatThrownBy(() -> order.validate())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("at least one line");
        }
    }

    // ========================================
    // Helper Methods
    // ========================================

    private PurchaseOrder createOrderWithLines(PurchaseOrderLine... lines) {
        return PurchaseOrder.builder()
                .id(1L)
                .supplierName("Test Supplier")
                .status(PurchaseOrderStatus.PENDING)
                .lines(new ArrayList<>(java.util.Arrays.asList(lines)))
                .createdAt(Instant.now())
                .build();
    }

    private PurchaseOrderLine createLine(Long productId, Integer quantity, BigDecimal unitPrice) {
        return PurchaseOrderLine.builder()
                .id(1L)
                .purchaseOrderId(1L)
                .productId(productId)
                .quantity(quantity)
                .unitPrice(unitPrice)
                .build();
    }
}
