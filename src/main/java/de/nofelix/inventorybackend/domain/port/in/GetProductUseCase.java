package de.nofelix.inventorybackend.domain.port.in;

import de.nofelix.inventorybackend.domain.model.Product;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Input port for retrieving products.
 * 
 * <p>This use case defines operations for querying products
 * from the inventory system.</p>
 */
public interface GetProductUseCase {

    /**
     * Retrieves a product by its ID.
     *
     * @param id the product ID
     * @return the product wrapped in a Mono, or empty if not found
     */
    Mono<Product> getProductById(Long id);

    /**
     * Retrieves all products with optional pagination.
     *
     * @param page the page number (0-indexed)
     * @param size the page size
     * @return a Flux of products
     */
    Flux<Product> listProducts(int page, int size);

    /**
     * Retrieves a product by its SKU.
     *
     * @param sku the product SKU
     * @return the product wrapped in a Mono, or empty if not found
     */
    Mono<Product> getProductBySku(String sku);
}
