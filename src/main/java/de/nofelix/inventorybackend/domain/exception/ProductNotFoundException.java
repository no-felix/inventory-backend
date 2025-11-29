package de.nofelix.inventorybackend.domain.exception;

/**
 * Exception thrown when a requested product is not found.
 */
public class ProductNotFoundException extends RuntimeException {

    private final Long productId;

    public ProductNotFoundException(Long productId) {
        super("Product with ID " + productId + " not found");
        this.productId = productId;
    }

    public ProductNotFoundException(String message) {
        super(message);
        this.productId = null;
    }

    public Long getProductId() {
        return productId;
    }
}
