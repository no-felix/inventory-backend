package de.nofelix.inventorybackend.domain.port.out;

import de.nofelix.inventorybackend.domain.model.Product;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Output port for product persistence operations.
 * 
 * <p>This port defines the contract for persisting and retrieving
 * products. It is implemented by the persistence adapter.</p>
 */
public interface ProductRepositoryPort {

    /**
     * Saves a product (create or update).
     *
     * @param product the product to save
     * @return the saved product with generated ID
     */
    Mono<Product> save(Product product);

    /**
     * Finds a product by its ID.
     *
     * @param id the product ID
     * @return the product wrapped in a Mono, or empty if not found
     */
    Mono<Product> findById(Long id);

    /**
     * Finds a product by its SKU.
     *
     * @param sku the product SKU
     * @return the product wrapped in a Mono, or empty if not found
     */
    Mono<Product> findBySku(String sku);

    /**
     * Retrieves all products with pagination.
     *
     * @param page the page number (0-indexed)
     * @param size the page size
     * @return a Flux of products
     */
    Flux<Product> findAll(int page, int size);

    /**
     * Retrieves all products.
     *
     * @return a Flux of all products
     */
    Flux<Product> findAll();

    /**
     * Deletes a product by its ID.
     *
     * @param id the product ID
     * @return a Mono that completes when deletion is done
     */
    Mono<Void> deleteById(Long id);

    /**
     * Checks if a product with the given SKU exists.
     *
     * @param sku the product SKU
     * @return true if exists, false otherwise
     */
    Mono<Boolean> existsBySku(String sku);

    /**
     * Checks if a product with the given ID exists.
     *
     * @param id the product ID
     * @return true if exists, false otherwise
     */
    Mono<Boolean> existsById(Long id);
}
