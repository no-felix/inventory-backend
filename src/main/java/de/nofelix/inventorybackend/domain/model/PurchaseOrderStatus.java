package de.nofelix.inventorybackend.domain.model;

/**
 * Enum representing the status of a purchase order.
 */
public enum PurchaseOrderStatus {
    /**
     * Order has been created but not yet received.
     */
    PENDING,
    
    /**
     * Order has been received and stock has been updated.
     */
    RECEIVED,
    
    /**
     * Order has been cancelled.
     */
    CANCELLED
}
