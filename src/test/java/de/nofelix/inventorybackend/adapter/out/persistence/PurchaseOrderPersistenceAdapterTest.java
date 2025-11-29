package de.nofelix.inventorybackend.adapter.out.persistence;

import de.nofelix.inventorybackend.TestcontainersConfiguration;
import de.nofelix.inventorybackend.domain.model.PurchaseOrder;
import de.nofelix.inventorybackend.domain.model.PurchaseOrderLine;
import de.nofelix.inventorybackend.domain.model.PurchaseOrderStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for PurchaseOrderPersistenceAdapter.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
@DisplayName("PurchaseOrderPersistenceAdapter")
class PurchaseOrderPersistenceAdapterTest {

    @Autowired
    private PurchaseOrderPersistenceAdapter adapter;

    @Autowired
    private ProductPersistenceAdapter productAdapter;

    private Long testProductId;

    @BeforeEach
    void setUp() {
        // Create a test product for the order lines
        var product = de.nofelix.inventorybackend.domain.model.Product.builder()
                .sku("PO-TEST-SKU-" + System.currentTimeMillis())
                .name("Test Product")
                .quantityOnHand(100)
                .unitPrice(BigDecimal.valueOf(10.00))
                .build();

        testProductId = productAdapter.save(product)
                .map(de.nofelix.inventorybackend.domain.model.Product::getId)
                .block();
    }

    @Nested
    @DisplayName("save")
    class SaveTests {

        @Test
        @DisplayName("should save new purchase order with generated id and timestamps")
        void save_newOrder_setsIdAndTimestamps() {
            // given
            var order = PurchaseOrder.builder()
                    .supplierName("Test Supplier")
                    .status(PurchaseOrderStatus.PENDING)
                    .lines(new ArrayList<>())
                    .build();

            // when/then
            StepVerifier.create(adapter.save(order))
                    .assertNext(saved -> {
                        assertThat(saved.getId()).isNotNull();
                        assertThat(saved.getSupplierName()).isEqualTo("Test Supplier");
                        assertThat(saved.getStatus()).isEqualTo(PurchaseOrderStatus.PENDING);
                        assertThat(saved.getCreatedAt()).isNotNull();
                    })
                    .verifyComplete();
        }

        @Test
        @DisplayName("should update existing purchase order")
        void save_existingOrder_updatesFields() {
            // given
            var order = PurchaseOrder.builder()
                    .supplierName("Original Supplier")
                    .status(PurchaseOrderStatus.PENDING)
                    .lines(new ArrayList<>())
                    .build();

            var saved = adapter.save(order).block();
            assertThat(saved).isNotNull();

            var updatedOrder = PurchaseOrder.builder()
                    .id(saved.getId())
                    .supplierName("Updated Supplier")
                    .status(PurchaseOrderStatus.RECEIVED)
                    .receivedAt(Instant.now())
                    .createdAt(saved.getCreatedAt())
                    .version(saved.getVersion())
                    .lines(new ArrayList<>())
                    .build();

            // when/then
            StepVerifier.create(adapter.save(updatedOrder))
                    .assertNext(updated -> {
                        assertThat(updated.getId()).isEqualTo(saved.getId());
                        assertThat(updated.getSupplierName()).isEqualTo("Updated Supplier");
                        assertThat(updated.getStatus()).isEqualTo(PurchaseOrderStatus.RECEIVED);
                        assertThat(updated.getReceivedAt()).isNotNull();
                    })
                    .verifyComplete();
        }
    }

    @Nested
    @DisplayName("saveLine")
    class SaveLineTests {

        @Test
        @DisplayName("should save purchase order line")
        void saveLine_validLine_savesSuccessfully() {
            // given
            var order = adapter.save(PurchaseOrder.builder()
                    .supplierName("Line Test Supplier")
                    .status(PurchaseOrderStatus.PENDING)
                    .lines(new ArrayList<>())
                    .build()).block();
            assertThat(order).isNotNull();

            var line = PurchaseOrderLine.builder()
                    .purchaseOrderId(order.getId())
                    .productId(testProductId)
                    .quantity(10)
                    .unitPrice(BigDecimal.valueOf(25.00))
                    .build();

            // when/then
            StepVerifier.create(adapter.saveLine(line))
                    .assertNext(saved -> {
                        assertThat(saved.getId()).isNotNull();
                        assertThat(saved.getPurchaseOrderId()).isEqualTo(order.getId());
                        assertThat(saved.getProductId()).isEqualTo(testProductId);
                        assertThat(saved.getQuantity()).isEqualTo(10);
                        assertThat(saved.getUnitPrice()).isEqualByComparingTo(BigDecimal.valueOf(25.00));
                    })
                    .verifyComplete();
        }
    }

    @Nested
    @DisplayName("findById")
    class FindByIdTests {

        @Test
        @DisplayName("should find purchase order by id")
        void findById_existingId_returnsOrder() {
            // given
            var order = adapter.save(PurchaseOrder.builder()
                    .supplierName("Find Test Supplier")
                    .status(PurchaseOrderStatus.PENDING)
                    .lines(new ArrayList<>())
                    .build()).block();
            assertThat(order).isNotNull();

            // when/then
            StepVerifier.create(adapter.findById(order.getId()))
                    .assertNext(found -> {
                        assertThat(found.getId()).isEqualTo(order.getId());
                        assertThat(found.getSupplierName()).isEqualTo("Find Test Supplier");
                    })
                    .verifyComplete();
        }

        @Test
        @DisplayName("should return empty for non-existent id")
        void findById_nonExistentId_returnsEmpty() {
            // when/then
            StepVerifier.create(adapter.findById(999999L))
                    .verifyComplete();
        }
    }

    @Nested
    @DisplayName("findByIdWithLines")
    class FindByIdWithLinesTests {

        @Test
        @DisplayName("should find purchase order with lines")
        void findByIdWithLines_orderWithLines_returnsOrderWithLines() {
            // given
            var order = adapter.save(PurchaseOrder.builder()
                    .supplierName("Lines Test Supplier")
                    .status(PurchaseOrderStatus.PENDING)
                    .lines(new ArrayList<>())
                    .build()).block();
            assertThat(order).isNotNull();

            adapter.saveLine(PurchaseOrderLine.builder()
                    .purchaseOrderId(order.getId())
                    .productId(testProductId)
                    .quantity(5)
                    .unitPrice(BigDecimal.valueOf(10.00))
                    .build()).block();

            // when/then
            StepVerifier.create(adapter.findByIdWithLines(order.getId()))
                    .assertNext(found -> {
                        assertThat(found.getId()).isEqualTo(order.getId());
                        assertThat(found.getLines()).hasSize(1);
                        assertThat(found.getLines().get(0).getQuantity()).isEqualTo(5);
                    })
                    .verifyComplete();
        }
    }

    @Nested
    @DisplayName("findByStatus")
    class FindByStatusTests {

        @Test
        @DisplayName("should find orders by status")
        void findByStatus_pendingStatus_returnsPendingOrders() {
            // given
            adapter.save(PurchaseOrder.builder()
                    .supplierName("Status Test Supplier")
                    .status(PurchaseOrderStatus.PENDING)
                    .lines(new ArrayList<>())
                    .build()).block();

            // when/then
            StepVerifier.create(adapter.findByStatus(PurchaseOrderStatus.PENDING).collectList())
                    .assertNext(orders -> {
                        assertThat(orders).isNotEmpty();
                        assertThat(orders).allMatch(o -> o.getStatus() == PurchaseOrderStatus.PENDING);
                    })
                    .verifyComplete();
        }
    }

    @Nested
    @DisplayName("existsById")
    class ExistsByIdTests {

        @Test
        @DisplayName("should return true for existing order")
        void existsById_existingId_returnsTrue() {
            // given
            var order = adapter.save(PurchaseOrder.builder()
                    .supplierName("Exists Test Supplier")
                    .status(PurchaseOrderStatus.PENDING)
                    .lines(new ArrayList<>())
                    .build()).block();
            assertThat(order).isNotNull();

            // when/then
            StepVerifier.create(adapter.existsById(order.getId()))
                    .expectNext(true)
                    .verifyComplete();
        }

        @Test
        @DisplayName("should return false for non-existent order")
        void existsById_nonExistentId_returnsFalse() {
            // when/then
            StepVerifier.create(adapter.existsById(999999L))
                    .expectNext(false)
                    .verifyComplete();
        }
    }
}
