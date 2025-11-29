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
}
