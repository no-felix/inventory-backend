package de.nofelix.inventorybackend.domain.exception;

/**
 * Exception thrown when an operation would result in negative stock.
 */
public class InsufficientStockException extends RuntimeException {

    private final Long productId;
    private final int currentQuantity;
    private final int requestedChange;

    public InsufficientStockException(Long productId, int currentQuantity, int requestedChange) {
        super(String.format(
                "Insufficient stock for product %d: current quantity is %d, cannot decrease by %d",
                productId, currentQuantity, Math.abs(requestedChange)));
        this.productId = productId;
        this.currentQuantity = currentQuantity;
        this.requestedChange = requestedChange;
    }

    public Long getProductId() {
        return productId;
    }

    public int getCurrentQuantity() {
        return currentQuantity;
    }

    public int getRequestedChange() {
        return requestedChange;
    }
}
