package de.nofelix.inventorybackend.adapter.out.persistence.repository;

import de.nofelix.inventorybackend.adapter.out.persistence.entity.PurchaseOrderEntity;
import de.nofelix.inventorybackend.domain.model.PurchaseOrderStatus;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

/**
 * Spring Data R2DBC repository for PurchaseOrderEntity.
 */
@Repository
public interface PurchaseOrderR2dbcRepository extends R2dbcRepository<PurchaseOrderEntity, Long> {

    /**
     * Finds all purchase orders by status.
     *
     * @param status the order status
     * @return flux of purchase order entities
     */
    Flux<PurchaseOrderEntity> findByStatus(PurchaseOrderStatus status);

    /**
     * Finds all purchase orders ordered by created date descending.
     *
     * @return flux of purchase order entities
     */
    Flux<PurchaseOrderEntity> findAllByOrderByCreatedAtDesc();
}
