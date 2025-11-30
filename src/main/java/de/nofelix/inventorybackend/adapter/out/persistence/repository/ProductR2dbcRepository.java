package de.nofelix.inventorybackend.adapter.out.persistence.repository;

import de.nofelix.inventorybackend.adapter.out.persistence.entity.ProductEntity;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Spring Data R2DBC repository for ProductEntity.
 */
@Repository
public interface ProductR2dbcRepository extends R2dbcRepository<ProductEntity, Long> {

    /**
     * Finds a product by its SKU.
     *
     * @param sku the product SKU
     * @return the product entity wrapped in a Mono
     */
    Mono<ProductEntity> findBySku(String sku);

    /**
     * Checks if a product with the given SKU exists.
     *
     * @param sku the product SKU
     * @return true if exists
     */
    Mono<Boolean> existsBySku(String sku);

    /**
     * Finds all active products.
     *
     * @return a Flux of active product entities
     */
    Flux<ProductEntity> findByActiveTrue();

    /**
     * Checks if an active product with the given SKU exists.
     *
     * @param sku the product SKU
     * @return true if an active product with the SKU exists
     */
    Mono<Boolean> existsBySkuAndActiveTrue(String sku);
}
