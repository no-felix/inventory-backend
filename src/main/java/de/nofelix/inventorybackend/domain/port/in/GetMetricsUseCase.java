package de.nofelix.inventorybackend.domain.port.in;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Use case for retrieving inventory metrics.
 */
public interface GetMetricsUseCase {

    /**
     * Get a summary of the current inventory state.
     *
     * @return inventory summary including product count, total items, low stock count, and total value
     */
    Mono<InventorySummary> getInventorySummary();

    /**
     * Get current stock levels for all products.
     *
     * @return flux of stock level information for each product
     */
    Flux<StockLevel> getStockLevels();

    /**
     * Get time series data of received stock quantities.
     *
     * @param from start date (inclusive)
     * @param to end date (inclusive)
     * @return flux of daily receipt summaries
     */
    Flux<ReceiptsTimeSeries> getReceiptsTimeSeries(LocalDate from, LocalDate to);

    /**
     * Get products with low stock (below configured threshold).
     *
     * @return flux of low stock alert information
     */
    Flux<LowStockAlert> getLowStockAlerts();

    /**
     * Get slow-moving items (no stock movement in specified days).
     *
     * @param days number of days with no movement to be considered slow-moving
     * @return flux of slow-moving item information
     */
    Flux<SlowMovingItem> getSlowMovingItems(int days);

    /**
     * Get inventory valuation grouped by price range.
     *
     * @return flux of valuation breakdown by price range
     */
    Flux<ValuationByPriceRange> getValuationByPriceRange();

    /**
     * Inventory summary data.
     */
    record InventorySummary(
            int productCount,
            int totalItems,
            int lowStockCount,
            BigDecimal totalValue
    ) {}

    /**
     * Stock level data for a single product.
     */
    record StockLevel(
            String sku,
            String name,
            int quantity,
            BigDecimal unitPrice,
            BigDecimal totalValue
    ) {}

    /**
     * Daily receipts summary for time series charts.
     */
    record ReceiptsTimeSeries(
            LocalDate date,
            int totalQuantity,
            int orderCount
    ) {}

    /**
     * Low stock alert data for a product below threshold.
     */
    record LowStockAlert(
            Long productId,
            String sku,
            String name,
            int currentQuantity,
            int threshold,
            int deficit
    ) {}

    /**
     * Slow-moving item data for products with no recent activity.
     */
    record SlowMovingItem(
            Long productId,
            String sku,
            String name,
            int quantityOnHand,
            BigDecimal totalValue,
            LocalDate lastMovementDate,
            int daysSinceLastMovement
    ) {}

    /**
     * Inventory valuation breakdown by price range.
     */
    record ValuationByPriceRange(
            String priceRange,
            int productCount,
            int totalQuantity,
            BigDecimal totalValue
    ) {}
}
