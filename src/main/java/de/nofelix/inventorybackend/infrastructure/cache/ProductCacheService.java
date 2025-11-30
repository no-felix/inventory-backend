package de.nofelix.inventorybackend.infrastructure.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.nofelix.inventorybackend.domain.model.Product;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;

/**
 * Service for caching Product entities in Redis.
 * Uses String serialization with Jackson for JSON conversion.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "cache.enabled", havingValue = "true")
public class ProductCacheService implements ProductCache {

    private static final String PRODUCT_KEY_PREFIX = "product:";

    private final ReactiveStringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @Value("${cache.ttl.product-cache:300}")
    private long productTtlSeconds;

    /**
     * Cache a product by its ID.
     *
     * @param product the product to cache
     * @return Mono indicating completion
     */
    public Mono<Boolean> cacheProduct(Product product) {
        if (product == null || product.getId() == null) {
            return Mono.just(false);
        }

        String key = buildKey(product.getId());
        try {
            String json = objectMapper.writeValueAsString(product);
            return redisTemplate.opsForValue()
                    .set(key, json, Duration.ofSeconds(productTtlSeconds))
                    .doOnSuccess(success -> log.debug("Cached product with id: {}", product.getId()))
                    .doOnError(error -> log.warn("Failed to cache product {}: {}", product.getId(), error.getMessage()));
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize product {}: {}", product.getId(), e.getMessage());
            return Mono.just(false);
        }
    }

    /**
     * Retrieve a cached product by its ID.
     *
     * @param productId the product ID
     * @return Mono containing the product if found, empty otherwise
     */
    public Mono<Product> getProduct(Long productId) {
        if (productId == null) {
            return Mono.empty();
        }

        String key = buildKey(productId);
        return redisTemplate.opsForValue()
                .get(key)
                .flatMap(json -> {
                    try {
                        Product product = objectMapper.readValue(json, Product.class);
                        log.debug("Cache hit for product id: {}", productId);
                        return Mono.just(product);
                    } catch (JsonProcessingException e) {
                        log.warn("Failed to deserialize product {}: {}", productId, e.getMessage());
                        return Mono.empty();
                    }
                })
                .doOnSubscribe(s -> log.debug("Looking up cache for product id: {}", productId));
    }

    /**
     * Evict a product from the cache.
     *
     * @param productId the product ID to evict
     * @return Mono indicating success
     */
    public Mono<Boolean> evictProduct(Long productId) {
        if (productId == null) {
            return Mono.just(false);
        }

        String key = buildKey(productId);
        return redisTemplate.opsForValue()
                .delete(key)
                .doOnSuccess(deleted -> log.debug("Evicted product from cache: {}", productId));
    }

    /**
     * Evict all products from the cache.
     *
     * @return Mono with the count of evicted keys
     */
    public Mono<Long> evictAllProducts() {
        return redisTemplate.keys(PRODUCT_KEY_PREFIX + "*")
                .collectList()
                .flatMap(keys -> {
                    if (keys.isEmpty()) {
                        return Mono.just(0L);
                    }
                    return redisTemplate.delete(keys.toArray(new String[0]));
                })
                .doOnSuccess(count -> log.debug("Evicted {} products from cache", count));
    }

    private String buildKey(Long productId) {
        return PRODUCT_KEY_PREFIX + productId;
    }
}
