package de.nofelix.inventorybackend.adapter.out.persistence.repository;

import de.nofelix.inventorybackend.adapter.out.persistence.entity.PurchaseOrderLineEntity;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Spring Data R2DBC repository for PurchaseOrderLineEntity.
 */
@Repository
public interface PurchaseOrderLineR2dbcRepository extends R2dbcRepository<PurchaseOrderLineEntity, Long> {

    /**
     * Finds all lines for a purchase order.
     *
     * @param purchaseOrderId the purchase order ID
     * @return flux of line entities
     */
    Flux<PurchaseOrderLineEntity> findByPurchaseOrderId(Long purchaseOrderId);

    /**
     * Deletes all lines for a purchase order.
     *
     * @param purchaseOrderId the purchase order ID
     * @return mono completing when deletion is done
     */
    Mono<Void> deleteByPurchaseOrderId(Long purchaseOrderId);
}
