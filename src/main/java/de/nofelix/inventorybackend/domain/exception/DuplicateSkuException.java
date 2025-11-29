package de.nofelix.inventorybackend.domain.exception;

/**
 * Exception thrown when attempting to create a product with a SKU
 * that already exists.
 */
public class DuplicateSkuException extends RuntimeException {

    private final String sku;

    public DuplicateSkuException(String sku) {
        super("Product with SKU '" + sku + "' already exists");
        this.sku = sku;
    }

    public String getSku() {
        return sku;
    }
}
