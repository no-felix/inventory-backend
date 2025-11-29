package de.nofelix.inventorybackend.domain.port.in;

import de.nofelix.inventorybackend.domain.model.StockMovement;
import de.nofelix.inventorybackend.domain.model.StockMovementReason;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;

/**
 * Input port for retrieving stock movements.
 * 
 * <p>This use case defines operations for querying stock movements
 * (inventory audit trail) from the system.</p>
 */
public interface GetStockMovementUseCase {

    /**
     * Retrieves all stock movements with optional filtering.
     *
     * @param productId optional product ID filter
     * @param reason optional reason filter
     * @param from optional start date filter
     * @param to optional end date filter
     * @return flux of stock movements
     */
    Flux<StockMovement> getStockMovements(
            Long productId,
            StockMovementReason reason,
            LocalDate from,
            LocalDate to);

    /**
     * Retrieves all stock movements for a product.
     *
     * @param productId the product ID
     * @return flux of stock movements
     */
    Flux<StockMovement> getStockMovementsByProduct(Long productId);

    /**
     * Retrieves a stock movement by ID.
     *
     * @param id the stock movement ID
     * @return the stock movement wrapped in a Mono
     */
    Mono<StockMovement> getStockMovementById(Long id);
}
