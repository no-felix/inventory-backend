package de.nofelix.inventorybackend.domain.model;

/**
 * Enum representing the reason for a stock movement.
 */
public enum StockMovementReason {
    /**
     * Stock increased due to purchase order receipt.
     */
    PO_RECEIPT,
    
    /**
     * Manual stock adjustment.
     */
    ADJUSTMENT,
    
    /**
     * Stock decreased due to a sale.
     */
    SALE,
    
    /**
     * Stock increased due to a return.
     */
    RETURN,
    
    /**
     * Stock decreased due to damage.
     */
    DAMAGE,
    
    /**
     * Stock moved between locations.
     */
    TRANSFER
}
