package de.nofelix.inventorybackend.domain.port.in;

import de.nofelix.inventorybackend.domain.model.PurchaseOrder;
import de.nofelix.inventorybackend.domain.model.PurchaseOrderStatus;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Input port for retrieving purchase orders.
 * 
 * <p>This use case defines operations for querying purchase orders
 * from the inventory system.</p>
 */
public interface GetPurchaseOrderUseCase {

    /**
     * Retrieves a purchase order by ID with its line items.
     *
     * @param id the purchase order ID
     * @return the purchase order wrapped in a Mono
     */
    Mono<PurchaseOrder> getPurchaseOrderById(Long id);

    /**
     * Retrieves all purchase orders.
     *
     * @return flux of purchase orders
     */
    Flux<PurchaseOrder> getAllPurchaseOrders();

    /**
     * Retrieves purchase orders with pagination.
     *
     * @param page the page number (0-indexed)
     * @param size the page size
     * @return flux of purchase orders
     */
    Flux<PurchaseOrder> getPurchaseOrders(int page, int size);

    /**
     * Retrieves purchase orders by status.
     *
     * @param status the order status
     * @return flux of purchase orders
     */
    Flux<PurchaseOrder> getPurchaseOrdersByStatus(PurchaseOrderStatus status);
}
