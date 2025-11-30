package de.nofelix.inventorybackend.infrastructure.cache;

import de.nofelix.inventorybackend.domain.model.Product;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * No-op implementation of product caching when caching is disabled.
 * All operations are no-ops that return immediately.
 */
@Service
@Slf4j
@ConditionalOnProperty(name = "cache.enabled", havingValue = "false", matchIfMissing = true)
public class NoOpProductCacheService implements ProductCache {

    /**
     * No-op cache operation.
     */
    public Mono<Boolean> cacheProduct(Product product) {
        return Mono.just(false);
    }

    /**
     * No-op get operation - always returns empty.
     */
    public Mono<Product> getProduct(Long productId) {
        return Mono.empty();
    }

    /**
     * No-op evict operation.
     */
    public Mono<Boolean> evictProduct(Long productId) {
        return Mono.just(false);
    }

    /**
     * No-op evict all operation.
     */
    public Mono<Long> evictAllProducts() {
        return Mono.just(0L);
    }
}
