package de.nofelix.inventorybackend.domain.port.in;

import de.nofelix.inventorybackend.domain.model.PurchaseOrder;
import reactor.core.publisher.Mono;

/**
 * Input port for receiving (marking as received) a purchase order.
 * 
 * <p>This use case defines the operation for marking a pending purchase order
 * as received. This will update product stock levels and create stock movement records.</p>
 */
public interface ReceivePurchaseOrderUseCase {

    /**
     * Marks a purchase order as received and updates stock levels.
     *
     * @param id the purchase order ID
     * @return the updated purchase order wrapped in a Mono
     */
    Mono<PurchaseOrder> receivePurchaseOrder(Long id);
}
