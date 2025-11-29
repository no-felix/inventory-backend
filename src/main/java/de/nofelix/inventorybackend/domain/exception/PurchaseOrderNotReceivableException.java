package de.nofelix.inventorybackend.domain.exception;

/**
 * Exception thrown when a purchase order cannot be received.
 */
public class PurchaseOrderNotReceivableException extends RuntimeException {

    private final Long purchaseOrderId;

    public PurchaseOrderNotReceivableException(Long purchaseOrderId, String reason) {
        super("Purchase order with ID " + purchaseOrderId + " cannot be received: " + reason);
        this.purchaseOrderId = purchaseOrderId;
    }

    public PurchaseOrderNotReceivableException(String message) {
        super(message);
        this.purchaseOrderId = null;
    }

    public Long getPurchaseOrderId() {
        return purchaseOrderId;
    }
}
