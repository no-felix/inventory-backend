package de.nofelix.inventorybackend.domain.port.in;

import reactor.core.publisher.Mono;

/**
 * Input port for deleting products.
 * 
 * <p>This use case defines the operation for deleting a product
 * from the inventory system.</p>
 */
public interface DeleteProductUseCase {

    /**
     * Deletes a product by its ID.
     *
     * @param id the product ID
     * @return a Mono that completes when deletion is done
     */
    Mono<Void> deleteProduct(Long id);
}
