package de.nofelix.inventorybackend.domain.port.in;

import de.nofelix.inventorybackend.domain.model.Product;
import reactor.core.publisher.Mono;

/**
 * Input port for creating products.
 * 
 * <p>This use case defines the operation for creating a new product
 * in the inventory system.</p>
 */
public interface CreateProductUseCase {

    /**
     * Creates a new product.
     *
     * @param command the command containing product data
     * @return the created product wrapped in a Mono
     */
    Mono<Product> createProduct(CreateProductCommand command);

    /**
     * Command object for creating a product.
     */
    record CreateProductCommand(
            String sku,
            String name,
            String description,
            Integer quantityOnHand,
            java.math.BigDecimal unitPrice
    ) {}
}
