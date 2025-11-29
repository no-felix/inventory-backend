package de.nofelix.inventorybackend.adapter.out.persistence;

import de.nofelix.inventorybackend.TestcontainersConfiguration;
import de.nofelix.inventorybackend.domain.model.Product;
import de.nofelix.inventorybackend.domain.model.StockMovement;
import de.nofelix.inventorybackend.domain.model.StockMovementReason;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for StockMovementPersistenceAdapter.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
@DisplayName("StockMovementPersistenceAdapter")
class StockMovementPersistenceAdapterTest {

    @Autowired
    private StockMovementPersistenceAdapter adapter;

    @Autowired
    private ProductPersistenceAdapter productAdapter;

    private Long testProductId;

    @BeforeEach
    void setUp() {
        // Create a test product for movements
        var product = Product.builder()
                .sku("SM-TEST-SKU-" + System.currentTimeMillis())
                .name("Stock Movement Test Product")
                .quantityOnHand(100)
                .unitPrice(BigDecimal.valueOf(10.00))
                .build();

        testProductId = productAdapter.save(product)
                .map(Product::getId)
                .block();
    }

    @Nested
    @DisplayName("save")
    class SaveTests {

        @Test
        @DisplayName("should save new stock movement with generated id and timestamp")
        void save_newMovement_setsIdAndTimestamp() {
            // given
            var movement = StockMovement.builder()
                    .productId(testProductId)
                    .change(10)
                    .reason(StockMovementReason.PO_RECEIPT)
                    .relatedEntityType("PurchaseOrder")
                    .relatedEntityId(1L)
                    .performedBy("system")
                    .build();

            // when/then
            StepVerifier.create(adapter.save(movement))
                    .assertNext(saved -> {
                        assertThat(saved.getId()).isNotNull();
                        assertThat(saved.getProductId()).isEqualTo(testProductId);
                        assertThat(saved.getChange()).isEqualTo(10);
                        assertThat(saved.getReason()).isEqualTo(StockMovementReason.PO_RECEIPT);
                        assertThat(saved.getCreatedAt()).isNotNull();
                    })
                    .verifyComplete();
        }

        @Test
        @DisplayName("should save stock movement with negative change")
        void save_negativeChange_savesSuccessfully() {
            // given
            var movement = StockMovement.builder()
                    .productId(testProductId)
                    .change(-5)
                    .reason(StockMovementReason.ADJUSTMENT)
                    .performedBy("admin")
                    .build();

            // when/then
            StepVerifier.create(adapter.save(movement))
                    .assertNext(saved -> {
                        assertThat(saved.getChange()).isEqualTo(-5);
                        assertThat(saved.getReason()).isEqualTo(StockMovementReason.ADJUSTMENT);
                    })
                    .verifyComplete();
        }
    }

    @Nested
    @DisplayName("saveAll")
    class SaveAllTests {

        @Test
        @DisplayName("should save multiple stock movements")
        void saveAll_multipleMovements_savesAll() {
            // given
            var movements = Flux.just(
                    StockMovement.builder()
                            .productId(testProductId)
                            .change(10)
                            .reason(StockMovementReason.PO_RECEIPT)
                            .performedBy("system")
                            .build(),
                    StockMovement.builder()
                            .productId(testProductId)
                            .change(5)
                            .reason(StockMovementReason.PO_RECEIPT)
                            .performedBy("system")
                            .build()
            );

            // when/then
            StepVerifier.create(adapter.saveAll(movements).collectList())
                    .assertNext(saved -> {
                        assertThat(saved).hasSize(2);
                        assertThat(saved).allMatch(m -> m.getId() != null);
                        assertThat(saved).allMatch(m -> m.getCreatedAt() != null);
                    })
                    .verifyComplete();
        }
    }

    @Nested
    @DisplayName("findById")
    class FindByIdTests {

        @Test
        @DisplayName("should find stock movement by id")
        void findById_existingId_returnsMovement() {
            // given
            var movement = adapter.save(StockMovement.builder()
                    .productId(testProductId)
                    .change(15)
                    .reason(StockMovementReason.ADJUSTMENT)
                    .performedBy("test")
                    .build()).block();
            assertThat(movement).isNotNull();

            // when/then
            StepVerifier.create(adapter.findById(movement.getId()))
                    .assertNext(found -> {
                        assertThat(found.getId()).isEqualTo(movement.getId());
                        assertThat(found.getChange()).isEqualTo(15);
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
    @DisplayName("findByProductId")
    class FindByProductIdTests {

        @Test
        @DisplayName("should find movements by product id")
        void findByProductId_existingProductId_returnsMovements() {
            // given
            adapter.save(StockMovement.builder()
                    .productId(testProductId)
                    .change(20)
                    .reason(StockMovementReason.PO_RECEIPT)
                    .performedBy("system")
                    .build()).block();

            // when/then
            StepVerifier.create(adapter.findByProductId(testProductId).collectList())
                    .assertNext(movements -> {
                        assertThat(movements).isNotEmpty();
                        assertThat(movements).allMatch(m -> m.getProductId().equals(testProductId));
                    })
                    .verifyComplete();
        }
    }

    @Nested
    @DisplayName("findByReason")
    class FindByReasonTests {

        @Test
        @DisplayName("should find movements by reason")
        void findByReason_existingReason_returnsMovements() {
            // given
            adapter.save(StockMovement.builder()
                    .productId(testProductId)
                    .change(10)
                    .reason(StockMovementReason.ADJUSTMENT)
                    .performedBy("admin")
                    .build()).block();

            // when/then
            StepVerifier.create(adapter.findByReason(StockMovementReason.ADJUSTMENT).collectList())
                    .assertNext(movements -> {
                        assertThat(movements).isNotEmpty();
                        assertThat(movements).allMatch(m -> m.getReason() == StockMovementReason.ADJUSTMENT);
                    })
                    .verifyComplete();
        }
    }

    @Nested
    @DisplayName("findByDateRange")
    class FindByDateRangeTests {

        @Test
        @DisplayName("should find movements within date range")
        void findByDateRange_withinRange_returnsMovements() {
            // given
            adapter.save(StockMovement.builder()
                    .productId(testProductId)
                    .change(10)
                    .reason(StockMovementReason.PO_RECEIPT)
                    .performedBy("system")
                    .build()).block();

            var today = LocalDate.now();
            var yesterday = today.minusDays(1);
            var tomorrow = today.plusDays(1);

            // when/then
            StepVerifier.create(adapter.findByDateRange(yesterday, tomorrow).collectList())
                    .assertNext(movements -> {
                        assertThat(movements).isNotEmpty();
                    })
                    .verifyComplete();
        }
    }

    @Nested
    @DisplayName("findWithFilters")
    class FindWithFiltersTests {

        @Test
        @DisplayName("should find movements with product id filter")
        void findWithFilters_byProductId_returnsFilteredMovements() {
            // given
            adapter.save(StockMovement.builder()
                    .productId(testProductId)
                    .change(10)
                    .reason(StockMovementReason.PO_RECEIPT)
                    .performedBy("system")
                    .build()).block();

            // when/then
            StepVerifier.create(adapter.findWithFilters(testProductId, null, null, null).collectList())
                    .assertNext(movements -> {
                        assertThat(movements).isNotEmpty();
                        assertThat(movements).allMatch(m -> m.getProductId().equals(testProductId));
                    })
                    .verifyComplete();
        }

        @Test
        @DisplayName("should find movements with reason filter")
        void findWithFilters_byReason_returnsFilteredMovements() {
            // given
            adapter.save(StockMovement.builder()
                    .productId(testProductId)
                    .change(10)
                    .reason(StockMovementReason.ADJUSTMENT)
                    .performedBy("admin")
                    .build()).block();

            // when/then
            StepVerifier.create(adapter.findWithFilters(null, StockMovementReason.ADJUSTMENT, null, null).collectList())
                    .assertNext(movements -> {
                        assertThat(movements).isNotEmpty();
                        assertThat(movements).allMatch(m -> m.getReason() == StockMovementReason.ADJUSTMENT);
                    })
                    .verifyComplete();
        }
    }

    @Nested
    @DisplayName("findByRelatedEntity")
    class FindByRelatedEntityTests {

        @Test
        @DisplayName("should find movements by related entity")
        void findByRelatedEntity_existingEntity_returnsMovements() {
            // given
            adapter.save(StockMovement.builder()
                    .productId(testProductId)
                    .change(10)
                    .reason(StockMovementReason.PO_RECEIPT)
                    .relatedEntityType("PurchaseOrder")
                    .relatedEntityId(42L)
                    .performedBy("system")
                    .build()).block();

            // when/then
            StepVerifier.create(adapter.findByRelatedEntity("PurchaseOrder", 42L).collectList())
                    .assertNext(movements -> {
                        assertThat(movements).isNotEmpty();
                        assertThat(movements).allMatch(m -> 
                                "PurchaseOrder".equals(m.getRelatedEntityType()) && 
                                m.getRelatedEntityId().equals(42L));
                    })
                    .verifyComplete();
        }
    }
}
