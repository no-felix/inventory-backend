package de.nofelix.inventorybackend.domain.port.out;

import de.nofelix.inventorybackend.domain.model.PurchaseOrder;
import de.nofelix.inventorybackend.domain.model.PurchaseOrderLine;
import de.nofelix.inventorybackend.domain.model.PurchaseOrderStatus;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Output port for purchase order persistence operations.
 * 
 * <p>This port defines the contract for persisting and retrieving
 * purchase orders. It is implemented by the persistence adapter.</p>
 */
public interface PurchaseOrderRepositoryPort {

    /**
     * Saves a purchase order (create or update).
     *
     * @param purchaseOrder the purchase order to save
     * @return the saved purchase order with generated ID
     */
    Mono<PurchaseOrder> save(PurchaseOrder purchaseOrder);

    /**
     * Saves a purchase order line.
     *
     * @param line the line to save
     * @return the saved line with generated ID
     */
    Mono<PurchaseOrderLine> saveLine(PurchaseOrderLine line);

    /**
     * Saves multiple purchase order lines.
     *
     * @param lines the lines to save
     * @return flux of saved lines
     */
    Flux<PurchaseOrderLine> saveLines(Flux<PurchaseOrderLine> lines);

    /**
     * Finds a purchase order by its ID.
     *
     * @param id the purchase order ID
     * @return the purchase order wrapped in a Mono, or empty if not found
     */
    Mono<PurchaseOrder> findById(Long id);

    /**
     * Finds a purchase order by ID with its lines loaded.
     *
     * @param id the purchase order ID
     * @return the purchase order with lines, or empty if not found
     */
    Mono<PurchaseOrder> findByIdWithLines(Long id);

    /**
     * Finds all lines for a purchase order.
     *
     * @param purchaseOrderId the purchase order ID
     * @return flux of lines
     */
    Flux<PurchaseOrderLine> findLinesByPurchaseOrderId(Long purchaseOrderId);

    /**
     * Retrieves all purchase orders.
     *
     * @return a Flux of all purchase orders
     */
    Flux<PurchaseOrder> findAll();

    /**
     * Retrieves purchase orders by status.
     *
     * @param status the order status
     * @return a Flux of purchase orders
     */
    Flux<PurchaseOrder> findByStatus(PurchaseOrderStatus status);

    /**
     * Retrieves all purchase orders with pagination.
     *
     * @param page the page number (0-indexed)
     * @param size the page size
     * @return a Flux of purchase orders
     */
    Flux<PurchaseOrder> findAll(int page, int size);

    /**
     * Checks if a purchase order with the given ID exists.
     *
     * @param id the purchase order ID
     * @return true if exists, false otherwise
     */
    Mono<Boolean> existsById(Long id);
}
