package de.nofelix.inventorybackend.domain.port.in;

import de.nofelix.inventorybackend.domain.model.StockMovement;
import de.nofelix.inventorybackend.domain.model.StockMovementReason;
import reactor.core.publisher.Mono;

/**
 * Input port for creating stock movements.
 *
 * <p>This use case allows manual creation of stock movements for
 * adjustments, damages, sales, returns, and transfers. The product's
 * quantity on hand will be automatically updated.</p>
 *
 * <p>Note: PO_RECEIPT movements should be created through the
 * purchase order receive flow, not directly through this use case.</p>
 */
public interface CreateStockMovementUseCase {

    /**
     * Creates a new stock movement and updates the product quantity.
     *
     * @param command the command containing movement details
     * @return the created stock movement
     * @throws de.nofelix.inventorybackend.domain.exception.ProductNotFoundException if product not found
     * @throws IllegalArgumentException if reason is PO_RECEIPT (use PO receive flow instead)
     * @throws IllegalArgumentException if change would result in negative stock
     */
    Mono<StockMovement> createStockMovement(CreateStockMovementCommand command);

    /**
     * Command for creating a stock movement.
     *
     * @param productId the product ID
     * @param change the quantity change (positive for increase, negative for decrease)
     * @param reason the reason for the movement (ADJUSTMENT, DAMAGE, SALE, RETURN, TRANSFER)
     * @param notes optional notes about the movement
     * @param performedBy optional user who performed the action
     */
    record CreateStockMovementCommand(
            Long productId,
            Integer change,
            StockMovementReason reason,
            String notes,
            String performedBy
    ) {
        public CreateStockMovementCommand {
            if (productId == null) {
                throw new IllegalArgumentException("Product ID is required");
            }
            if (change == null || change == 0) {
                throw new IllegalArgumentException("Change must be a non-zero value");
            }
            if (reason == null) {
                throw new IllegalArgumentException("Reason is required");
            }
            if (reason == StockMovementReason.PO_RECEIPT) {
                throw new IllegalArgumentException("PO_RECEIPT movements must be created through the purchase order receive flow");
            }
        }
    }
}
