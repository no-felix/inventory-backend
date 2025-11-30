package de.nofelix.inventorybackend.infrastructure.cache;

import de.nofelix.inventorybackend.domain.model.Product;
import reactor.core.publisher.Mono;

/**
 * Port interface for product caching operations.
 * Implementations can be Redis-based or no-op depending on configuration.
 */
public interface ProductCache {

    /**
     * Cache a product by its ID.
     *
     * @param product the product to cache
     * @return Mono indicating success
     */
    Mono<Boolean> cacheProduct(Product product);

    /**
     * Retrieve a cached product by its ID.
     *
     * @param productId the product ID
     * @return Mono containing the product if found, empty otherwise
     */
    Mono<Product> getProduct(Long productId);

    /**
     * Evict a product from the cache.
     *
     * @param productId the product ID to evict
     * @return Mono indicating success
     */
    Mono<Boolean> evictProduct(Long productId);

    /**
     * Evict all products from the cache.
     *
     * @return Mono with the count of evicted keys
     */
    Mono<Long> evictAllProducts();
}
