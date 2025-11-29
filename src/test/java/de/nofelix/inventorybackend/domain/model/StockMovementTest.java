package de.nofelix.inventorybackend.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for the StockMovement domain model.
 */
@DisplayName("StockMovement Domain Model")
class StockMovementTest {

    @Nested
    @DisplayName("Factory Methods")
    class FactoryMethodTests {

        @Test
        @DisplayName("should create movement for purchase order receipt")
        void forPurchaseOrderReceipt_withValidData_createsMovement() {
            // when
            StockMovement movement = StockMovement.forPurchaseOrderReceipt(
                    1L, 50, 100L, "admin");

            // then
            assertThat(movement.getProductId()).isEqualTo(1L);
            assertThat(movement.getChange()).isEqualTo(50);
            assertThat(movement.getReason()).isEqualTo(StockMovementReason.PO_RECEIPT);
            assertThat(movement.getRelatedEntityType()).isEqualTo("PURCHASE_ORDER");
            assertThat(movement.getRelatedEntityId()).isEqualTo(100L);
            assertThat(movement.getPerformedBy()).isEqualTo("admin");
            assertThat(movement.getCreatedAt()).isNotNull();
        }

        @Test
        @DisplayName("should create movement for adjustment")
        void forAdjustment_withPositiveChange_createsMovement() {
            // when
            StockMovement movement = StockMovement.forAdjustment(1L, 25, "manager");

            // then
            assertThat(movement.getProductId()).isEqualTo(1L);
            assertThat(movement.getChange()).isEqualTo(25);
            assertThat(movement.getReason()).isEqualTo(StockMovementReason.ADJUSTMENT);
            assertThat(movement.getRelatedEntityType()).isNull();
            assertThat(movement.getRelatedEntityId()).isNull();
            assertThat(movement.getPerformedBy()).isEqualTo("manager");
        }

        @Test
        @DisplayName("should create movement for adjustment with negative change")
        void forAdjustment_withNegativeChange_createsMovement() {
            // when
            StockMovement movement = StockMovement.forAdjustment(1L, -10, "manager");

            // then
            assertThat(movement.getChange()).isEqualTo(-10);
            assertThat(movement.getReason()).isEqualTo(StockMovementReason.ADJUSTMENT);
        }

        @Test
        @DisplayName("should create movement for sale with negative change")
        void forSale_withValidData_createsMovementWithNegativeChange() {
            // when
            StockMovement movement = StockMovement.forSale(1L, 5, 200L, "cashier");

            // then
            assertThat(movement.getProductId()).isEqualTo(1L);
            assertThat(movement.getChange()).isEqualTo(-5); // Negative for sales
            assertThat(movement.getReason()).isEqualTo(StockMovementReason.SALE);
            assertThat(movement.getRelatedEntityType()).isEqualTo("SALE_ORDER");
            assertThat(movement.getRelatedEntityId()).isEqualTo(200L);
            assertThat(movement.getPerformedBy()).isEqualTo("cashier");
        }
    }

    @Nested
    @DisplayName("isIncrease")
    class IsIncreaseTests {

        @Test
        @DisplayName("should return true for positive change")
        void isIncrease_withPositiveChange_returnsTrue() {
            // given
            StockMovement movement = StockMovement.builder()
                    .change(10)
                    .build();

            // when/then
            assertThat(movement.isIncrease()).isTrue();
        }

        @Test
        @DisplayName("should return false for negative change")
        void isIncrease_withNegativeChange_returnsFalse() {
            // given
            StockMovement movement = StockMovement.builder()
                    .change(-10)
                    .build();

            // when/then
            assertThat(movement.isIncrease()).isFalse();
        }

        @Test
        @DisplayName("should return false for zero change")
        void isIncrease_withZeroChange_returnsFalse() {
            // given
            StockMovement movement = StockMovement.builder()
                    .change(0)
                    .build();

            // when/then
            assertThat(movement.isIncrease()).isFalse();
        }

        @Test
        @DisplayName("should return false for null change")
        void isIncrease_withNullChange_returnsFalse() {
            // given
            StockMovement movement = StockMovement.builder()
                    .change(null)
                    .build();

            // when/then
            assertThat(movement.isIncrease()).isFalse();
        }
    }

    @Nested
    @DisplayName("isDecrease")
    class IsDecreaseTests {

        @Test
        @DisplayName("should return true for negative change")
        void isDecrease_withNegativeChange_returnsTrue() {
            // given
            StockMovement movement = StockMovement.builder()
                    .change(-10)
                    .build();

            // when/then
            assertThat(movement.isDecrease()).isTrue();
        }

        @Test
        @DisplayName("should return false for positive change")
        void isDecrease_withPositiveChange_returnsFalse() {
            // given
            StockMovement movement = StockMovement.builder()
                    .change(10)
                    .build();

            // when/then
            assertThat(movement.isDecrease()).isFalse();
        }

        @Test
        @DisplayName("should return false for zero change")
        void isDecrease_withZeroChange_returnsFalse() {
            // given
            StockMovement movement = StockMovement.builder()
                    .change(0)
                    .build();

            // when/then
            assertThat(movement.isDecrease()).isFalse();
        }

        @Test
        @DisplayName("should return false for null change")
        void isDecrease_withNullChange_returnsFalse() {
            // given
            StockMovement movement = StockMovement.builder()
                    .change(null)
                    .build();

            // when/then
            assertThat(movement.isDecrease()).isFalse();
        }
    }

    @Nested
    @DisplayName("getAbsoluteChange")
    class GetAbsoluteChangeTests {

        @Test
        @DisplayName("should return absolute value for positive change")
        void getAbsoluteChange_withPositiveChange_returnsPositive() {
            // given
            StockMovement movement = StockMovement.builder()
                    .change(15)
                    .build();

            // when/then
            assertThat(movement.getAbsoluteChange()).isEqualTo(15);
        }

        @Test
        @DisplayName("should return absolute value for negative change")
        void getAbsoluteChange_withNegativeChange_returnsPositive() {
            // given
            StockMovement movement = StockMovement.builder()
                    .change(-15)
                    .build();

            // when/then
            assertThat(movement.getAbsoluteChange()).isEqualTo(15);
        }

        @Test
        @DisplayName("should return zero for zero change")
        void getAbsoluteChange_withZeroChange_returnsZero() {
            // given
            StockMovement movement = StockMovement.builder()
                    .change(0)
                    .build();

            // when/then
            assertThat(movement.getAbsoluteChange()).isEqualTo(0);
        }

        @Test
        @DisplayName("should return zero for null change")
        void getAbsoluteChange_withNullChange_returnsZero() {
            // given
            StockMovement movement = StockMovement.builder()
                    .change(null)
                    .build();

            // when/then
            assertThat(movement.getAbsoluteChange()).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("Builder")
    class BuilderTests {

        @Test
        @DisplayName("should build movement with all fields")
        void builder_withAllFields_createsCompleteMovement() {
            // given
            Instant now = Instant.now();

            // when
            StockMovement movement = StockMovement.builder()
                    .id(1L)
                    .productId(100L)
                    .productSku("SKU-001")
                    .productName("Test Product")
                    .change(50)
                    .reason(StockMovementReason.PO_RECEIPT)
                    .relatedEntityType("PURCHASE_ORDER")
                    .relatedEntityId(200L)
                    .performedBy("admin")
                    .createdAt(now)
                    .build();

            // then
            assertThat(movement.getId()).isEqualTo(1L);
            assertThat(movement.getProductId()).isEqualTo(100L);
            assertThat(movement.getProductSku()).isEqualTo("SKU-001");
            assertThat(movement.getProductName()).isEqualTo("Test Product");
            assertThat(movement.getChange()).isEqualTo(50);
            assertThat(movement.getReason()).isEqualTo(StockMovementReason.PO_RECEIPT);
            assertThat(movement.getRelatedEntityType()).isEqualTo("PURCHASE_ORDER");
            assertThat(movement.getRelatedEntityId()).isEqualTo(200L);
            assertThat(movement.getPerformedBy()).isEqualTo("admin");
            assertThat(movement.getCreatedAt()).isEqualTo(now);
        }
    }
}
