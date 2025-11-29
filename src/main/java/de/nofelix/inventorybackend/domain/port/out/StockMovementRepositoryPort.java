package de.nofelix.inventorybackend.domain.port.out;

import de.nofelix.inventorybackend.domain.model.StockMovement;
import de.nofelix.inventorybackend.domain.model.StockMovementReason;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;

/**
 * Output port for stock movement persistence operations.
 * 
 * <p>This port defines the contract for persisting and retrieving
 * stock movements. It is implemented by the persistence adapter.</p>
 */
public interface StockMovementRepositoryPort {

    /**
     * Saves a stock movement.
     *
     * @param stockMovement the stock movement to save
     * @return the saved stock movement with generated ID
     */
    Mono<StockMovement> save(StockMovement stockMovement);

    /**
     * Saves multiple stock movements.
     *
     * @param stockMovements the stock movements to save
     * @return flux of saved movements
     */
    Flux<StockMovement> saveAll(Flux<StockMovement> stockMovements);

    /**
     * Finds a stock movement by its ID.
     *
     * @param id the stock movement ID
     * @return the stock movement wrapped in a Mono, or empty if not found
     */
    Mono<StockMovement> findById(Long id);

    /**
     * Retrieves all stock movements.
     *
     * @return a Flux of all stock movements
     */
    Flux<StockMovement> findAll();

    /**
     * Retrieves all stock movements for a product.
     *
     * @param productId the product ID
     * @return a Flux of stock movements
     */
    Flux<StockMovement> findByProductId(Long productId);

    /**
     * Retrieves stock movements by reason.
     *
     * @param reason the movement reason
     * @return a Flux of stock movements
     */
    Flux<StockMovement> findByReason(StockMovementReason reason);

    /**
     * Retrieves stock movements within a date range.
     *
     * @param from start date (inclusive)
     * @param to end date (inclusive)
     * @return a Flux of stock movements
     */
    Flux<StockMovement> findByDateRange(LocalDate from, LocalDate to);

    /**
     * Retrieves stock movements with optional filters.
     *
     * @param productId optional product ID filter
     * @param reason optional reason filter
     * @param from optional start date filter
     * @param to optional end date filter
     * @return a Flux of stock movements
     */
    Flux<StockMovement> findWithFilters(
            Long productId,
            StockMovementReason reason,
            LocalDate from,
            LocalDate to);

    /**
     * Retrieves stock movements for a related entity.
     *
     * @param relatedEntityType the entity type
     * @param relatedEntityId the entity ID
     * @return a Flux of stock movements
     */
    Flux<StockMovement> findByRelatedEntity(String relatedEntityType, Long relatedEntityId);
}
