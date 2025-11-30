package de.nofelix.inventorybackend.infrastructure.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Redis cache configuration for reactive caching.
 * Uses Spring Boot's auto-configured ReactiveStringRedisTemplate.
 */
@Configuration
public class RedisCacheConfig {

    @Value("${cache.product.ttl:300}")
    private long productTtl;

    @Value("${cache.metrics.ttl:60}")
    private long metricsTtl;

    /**
     * Provides an ObjectMapper for JSON serialization in cache services.
     */
    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }

    /**
     * Returns the TTL for product cache entries in seconds.
     */
    public long getProductTtl() {
        return productTtl;
    }

    /**
     * Returns the TTL for metrics cache entries in seconds.
     */
    public long getMetricsTtl() {
        return metricsTtl;
    }
}
