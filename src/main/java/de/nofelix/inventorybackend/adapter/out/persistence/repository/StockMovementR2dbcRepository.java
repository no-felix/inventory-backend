package de.nofelix.inventorybackend.adapter.out.persistence.repository;

import de.nofelix.inventorybackend.adapter.out.persistence.entity.StockMovementEntity;
import de.nofelix.inventorybackend.domain.model.StockMovementReason;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

import java.time.Instant;

/**
 * Spring Data R2DBC repository for StockMovementEntity.
 */
@Repository
public interface StockMovementR2dbcRepository extends R2dbcRepository<StockMovementEntity, Long> {

    /**
     * Finds all stock movements for a product.
     *
     * @param productId the product ID
     * @return flux of stock movement entities
     */
    Flux<StockMovementEntity> findByProductIdOrderByCreatedAtDesc(Long productId);

    /**
     * Finds stock movements by reason.
     *
     * @param reason the movement reason
     * @return flux of stock movement entities
     */
    Flux<StockMovementEntity> findByReasonOrderByCreatedAtDesc(StockMovementReason reason);

    /**
     * Finds stock movements within a date range.
     *
     * @param from start date (inclusive)
     * @param to end date (inclusive)
     * @return flux of stock movement entities
     */
    Flux<StockMovementEntity> findByCreatedAtBetweenOrderByCreatedAtDesc(Instant from, Instant to);

    /**
     * Finds stock movements for a related entity.
     *
     * @param relatedEntityType the entity type
     * @param relatedEntityId the entity ID
     * @return flux of stock movement entities
     */
    Flux<StockMovementEntity> findByRelatedEntityTypeAndRelatedEntityId(
            String relatedEntityType, Long relatedEntityId);

    /**
     * Finds stock movements with optional filtering.
     *
     * @param productId optional product ID filter
     * @param reason optional reason filter
     * @param from optional start date filter
     * @param to optional end date filter
     * @return flux of stock movement entities
     */
    @Query("""
        SELECT * FROM stock_movements
        WHERE (:productId IS NULL OR product_id = :productId)
          AND (:reason IS NULL OR reason = :reason)
          AND (:from IS NULL OR created_at >= :from)
          AND (:to IS NULL OR created_at <= :to)
        ORDER BY created_at DESC
        """)
    Flux<StockMovementEntity> findWithFilters(
            Long productId, 
            String reason, 
            Instant from, 
            Instant to);
}
