package de.nofelix.inventorybackend.domain.exception;

/**
 * Exception thrown when a requested purchase order is not found.
 */
public class PurchaseOrderNotFoundException extends RuntimeException {

    private final Long purchaseOrderId;

    public PurchaseOrderNotFoundException(Long purchaseOrderId) {
        super("Purchase order with ID " + purchaseOrderId + " not found");
        this.purchaseOrderId = purchaseOrderId;
    }

    public PurchaseOrderNotFoundException(String message) {
        super(message);
        this.purchaseOrderId = null;
    }

    public Long getPurchaseOrderId() {
        return purchaseOrderId;
    }
}
