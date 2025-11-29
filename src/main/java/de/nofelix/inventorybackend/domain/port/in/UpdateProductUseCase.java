package de.nofelix.inventorybackend.domain.port.in;

import de.nofelix.inventorybackend.domain.model.Product;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

/**
 * Input port for updating products.
 * 
 * <p>This use case defines the operation for updating an existing product
 * in the inventory system.</p>
 */
public interface UpdateProductUseCase {

    /**
     * Updates an existing product.
     *
     * @param id the product ID
     * @param command the command containing updated product data
     * @return the updated product wrapped in a Mono
     */
    Mono<Product> updateProduct(Long id, UpdateProductCommand command);

    /**
     * Command object for updating a product.
     */
    record UpdateProductCommand(
            String sku,
            String name,
            String description,
            Integer quantityOnHand,
            BigDecimal unitPrice
    ) {}
}
