package de.nofelix.inventorybackend.domain.port.in;

import de.nofelix.inventorybackend.domain.model.PurchaseOrder;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.List;

/**
 * Input port for creating purchase orders.
 * 
 * <p>This use case defines the operation for creating a new purchase order
 * in the inventory system. If the order is marked as received, it will also
 * update product stock levels and create stock movement records.</p>
 */
public interface CreatePurchaseOrderUseCase {

    /**
     * Creates a new purchase order.
     *
     * @param command the command containing purchase order data
     * @return the created purchase order wrapped in a Mono
     */
    Mono<PurchaseOrder> createPurchaseOrder(CreatePurchaseOrderCommand command);

    /**
     * Command object for creating a purchase order.
     */
    record CreatePurchaseOrderCommand(
            String supplierName,
            List<LineItem> lines,
            boolean received
    ) {
        /**
         * Line item for a purchase order.
         */
        public record LineItem(
                Long productId,
                Integer quantity,
                BigDecimal unitPrice
        ) {}
    }
}
