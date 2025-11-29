package de.nofelix.inventorybackend.application.usecase;

import de.nofelix.inventorybackend.domain.model.Product;
import de.nofelix.inventorybackend.domain.model.StockMovement;
import de.nofelix.inventorybackend.domain.model.StockMovementReason;
import de.nofelix.inventorybackend.domain.port.out.ProductRepositoryPort;
import de.nofelix.inventorybackend.domain.port.out.StockMovementRepositoryPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Unit tests for MetricsService.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("MetricsService")
class MetricsServiceTest {

    @Mock
    private ProductRepositoryPort productRepository;

    @Mock
    private StockMovementRepositoryPort stockMovementRepository;

    @InjectMocks
    private MetricsService metricsService;

    @Nested
    @DisplayName("getInventorySummary")
    class GetInventorySummaryTests {

        @Test
        @DisplayName("should return correct summary for products")
        void getInventorySummary_withProducts_returnsCorrectSummary() {
            // given
            var product1 = Product.builder()
                    .id(1L)
                    .sku("SKU-001")
                    .name("Product 1")
                    .quantityOnHand(100)
                    .unitPrice(BigDecimal.valueOf(10.00))
                    .build();
            var product2 = Product.builder()
                    .id(2L)
                    .sku("SKU-002")
                    .name("Product 2")
                    .quantityOnHand(5) // Low stock
                    .unitPrice(BigDecimal.valueOf(20.00))
                    .build();
            var product3 = Product.builder()
                    .id(3L)
                    .sku("SKU-003")
                    .name("Product 3")
                    .quantityOnHand(50)
                    .unitPrice(BigDecimal.valueOf(15.00))
                    .build();

            when(productRepository.findAll()).thenReturn(Flux.just(product1, product2, product3));

            // when/then
            StepVerifier.create(metricsService.getInventorySummary())
                    .assertNext(summary -> {
                        assertThat(summary.productCount()).isEqualTo(3);
                        assertThat(summary.totalItems()).isEqualTo(155); // 100 + 5 + 50
                        assertThat(summary.lowStockCount()).isEqualTo(1); // Only product2 <= 10
                        // 100*10 + 5*20 + 50*15 = 1000 + 100 + 750 = 1850
                        assertThat(summary.totalValue()).isEqualByComparingTo(BigDecimal.valueOf(1850.00));
                    })
                    .verifyComplete();
        }

        @Test
        @DisplayName("should return zero values for empty inventory")
        void getInventorySummary_withNoProducts_returnsZeroValues() {
            // given
            when(productRepository.findAll()).thenReturn(Flux.empty());

            // when/then
            StepVerifier.create(metricsService.getInventorySummary())
                    .assertNext(summary -> {
                        assertThat(summary.productCount()).isZero();
                        assertThat(summary.totalItems()).isZero();
                        assertThat(summary.lowStockCount()).isZero();
                        assertThat(summary.totalValue()).isEqualByComparingTo(BigDecimal.ZERO);
                    })
                    .verifyComplete();
        }

        @Test
        @DisplayName("should handle null values in products")
        void getInventorySummary_withNullValues_handlesGracefully() {
            // given
            var product = Product.builder()
                    .id(1L)
                    .sku("SKU-001")
                    .name("Product 1")
                    .quantityOnHand(null)
                    .unitPrice(null)
                    .build();

            when(productRepository.findAll()).thenReturn(Flux.just(product));

            // when/then
            StepVerifier.create(metricsService.getInventorySummary())
                    .assertNext(summary -> {
                        assertThat(summary.productCount()).isEqualTo(1);
                        assertThat(summary.totalItems()).isZero();
                        assertThat(summary.totalValue()).isEqualByComparingTo(BigDecimal.ZERO);
                    })
                    .verifyComplete();
        }
    }

    @Nested
    @DisplayName("getStockLevels")
    class GetStockLevelsTests {

        @Test
        @DisplayName("should return stock levels for all products")
        void getStockLevels_withProducts_returnsAllLevels() {
            // given
            var product1 = Product.builder()
                    .id(1L)
                    .sku("SKU-001")
                    .name("Product 1")
                    .quantityOnHand(100)
                    .unitPrice(BigDecimal.valueOf(10.00))
                    .build();
            var product2 = Product.builder()
                    .id(2L)
                    .sku("SKU-002")
                    .name("Product 2")
                    .quantityOnHand(50)
                    .unitPrice(BigDecimal.valueOf(20.00))
                    .build();

            when(productRepository.findAll()).thenReturn(Flux.just(product1, product2));

            // when/then
            StepVerifier.create(metricsService.getStockLevels().collectList())
                    .assertNext(levels -> {
                        assertThat(levels).hasSize(2);
                        
                        var level1 = levels.stream().filter(l -> l.sku().equals("SKU-001")).findFirst().orElseThrow();
                        assertThat(level1.name()).isEqualTo("Product 1");
                        assertThat(level1.quantity()).isEqualTo(100);
                        assertThat(level1.unitPrice()).isEqualByComparingTo(BigDecimal.valueOf(10.00));
                        assertThat(level1.totalValue()).isEqualByComparingTo(BigDecimal.valueOf(1000.00));
                        
                        var level2 = levels.stream().filter(l -> l.sku().equals("SKU-002")).findFirst().orElseThrow();
                        assertThat(level2.name()).isEqualTo("Product 2");
                        assertThat(level2.quantity()).isEqualTo(50);
                        assertThat(level2.totalValue()).isEqualByComparingTo(BigDecimal.valueOf(1000.00));
                    })
                    .verifyComplete();
        }

        @Test
        @DisplayName("should return empty flux for no products")
        void getStockLevels_withNoProducts_returnsEmpty() {
            // given
            when(productRepository.findAll()).thenReturn(Flux.empty());

            // when/then
            StepVerifier.create(metricsService.getStockLevels())
                    .verifyComplete();
        }
    }

    @Nested
    @DisplayName("getReceiptsTimeSeries")
    class GetReceiptsTimeSeriesTests {

        @Test
        @DisplayName("should return time series for PO receipts")
        void getReceiptsTimeSeries_withMovements_returnsTimeSeries() {
            // given
            var from = LocalDate.of(2025, 1, 1);
            var to = LocalDate.of(2025, 1, 31);
            
            var movement1 = StockMovement.builder()
                    .id(1L)
                    .productId(1L)
                    .change(50)
                    .reason(StockMovementReason.PO_RECEIPT)
                    .relatedEntityType("PurchaseOrder")
                    .relatedEntityId(1L)
                    .createdAt(Instant.parse("2025-01-15T10:00:00Z"))
                    .build();
            var movement2 = StockMovement.builder()
                    .id(2L)
                    .productId(2L)
                    .change(30)
                    .reason(StockMovementReason.PO_RECEIPT)
                    .relatedEntityType("PurchaseOrder")
                    .relatedEntityId(1L) // Same order
                    .createdAt(Instant.parse("2025-01-15T10:00:00Z"))
                    .build();
            var movement3 = StockMovement.builder()
                    .id(3L)
                    .productId(1L)
                    .change(20)
                    .reason(StockMovementReason.PO_RECEIPT)
                    .relatedEntityType("PurchaseOrder")
                    .relatedEntityId(2L)
                    .createdAt(Instant.parse("2025-01-20T10:00:00Z"))
                    .build();
            // This should be excluded (ADJUSTMENT, not PO_RECEIPT)
            var movement4 = StockMovement.builder()
                    .id(4L)
                    .productId(1L)
                    .change(10)
                    .reason(StockMovementReason.ADJUSTMENT)
                    .createdAt(Instant.parse("2025-01-15T11:00:00Z"))
                    .build();

            when(stockMovementRepository.findByDateRange(from, to))
                    .thenReturn(Flux.just(movement1, movement2, movement3, movement4));

            // when/then
            StepVerifier.create(metricsService.getReceiptsTimeSeries(from, to).collectList())
                    .assertNext(series -> {
                        assertThat(series).hasSize(2);
                        
                        var day15 = series.stream()
                                .filter(s -> s.date().equals(LocalDate.of(2025, 1, 15)))
                                .findFirst().orElseThrow();
                        assertThat(day15.totalQuantity()).isEqualTo(80); // 50 + 30
                        assertThat(day15.orderCount()).isEqualTo(1); // Both from same PO
                        
                        var day20 = series.stream()
                                .filter(s -> s.date().equals(LocalDate.of(2025, 1, 20)))
                                .findFirst().orElseThrow();
                        assertThat(day20.totalQuantity()).isEqualTo(20);
                        assertThat(day20.orderCount()).isEqualTo(1);
                    })
                    .verifyComplete();
        }

        @Test
        @DisplayName("should return empty for no movements")
        void getReceiptsTimeSeries_withNoMovements_returnsEmpty() {
            // given
            var from = LocalDate.of(2025, 1, 1);
            var to = LocalDate.of(2025, 1, 31);
            
            when(stockMovementRepository.findByDateRange(from, to)).thenReturn(Flux.empty());

            // when/then
            StepVerifier.create(metricsService.getReceiptsTimeSeries(from, to))
                    .verifyComplete();
        }

        @Test
        @DisplayName("should exclude negative movements")
        void getReceiptsTimeSeries_withNegativeMovements_excludesThem() {
            // given
            var from = LocalDate.of(2025, 1, 1);
            var to = LocalDate.of(2025, 1, 31);
            
            var movement1 = StockMovement.builder()
                    .id(1L)
                    .productId(1L)
                    .change(-50) // Negative - should be excluded
                    .reason(StockMovementReason.PO_RECEIPT)
                    .relatedEntityId(1L)
                    .createdAt(Instant.parse("2025-01-15T10:00:00Z"))
                    .build();

            when(stockMovementRepository.findByDateRange(from, to))
                    .thenReturn(Flux.just(movement1));

            // when/then
            StepVerifier.create(metricsService.getReceiptsTimeSeries(from, to))
                    .verifyComplete(); // Should be empty as negative movement is filtered
        }
    }
}
