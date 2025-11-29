package de.nofelix.inventorybackend.adapter.out.persistence;

import de.nofelix.inventorybackend.adapter.out.persistence.entity.ProductEntity;
import de.nofelix.inventorybackend.adapter.out.persistence.repository.ProductR2dbcRepository;
import de.nofelix.inventorybackend.domain.model.Product;
import de.nofelix.inventorybackend.domain.port.out.ProductRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;

/**
 * Persistence adapter implementing the ProductRepositoryPort.
 * 
 * <p>This adapter translates between domain models and persistence entities,
 * delegating actual persistence operations to the R2DBC repository.</p>
 */
@Component
@RequiredArgsConstructor
public class ProductPersistenceAdapter implements ProductRepositoryPort {

    private final ProductR2dbcRepository repository;

    @Override
    public Mono<Product> save(Product product) {
        ProductEntity entity = toEntity(product);
        
        // Set timestamps for new entities
        if (entity.getId() == null) {
            entity.setCreatedAt(Instant.now());
        }
        entity.setUpdatedAt(Instant.now());
        
        return repository.save(entity)
                .map(this::toDomain);
    }

    @Override
    public Mono<Product> findById(Long id) {
        return repository.findById(id)
                .map(this::toDomain);
    }

    @Override
    public Mono<Product> findBySku(String sku) {
        return repository.findBySku(sku)
                .map(this::toDomain);
    }

    @Override
    public Flux<Product> findAll(int page, int size) {
        return repository.findAll()
                .skip((long) page * size)
                .take(size)
                .map(this::toDomain);
    }

    @Override
    public Flux<Product> findAll() {
        return repository.findAll()
                .map(this::toDomain);
    }

    @Override
    public Mono<Void> deleteById(Long id) {
        return repository.deleteById(id);
    }

    @Override
    public Mono<Boolean> existsBySku(String sku) {
        return repository.existsBySku(sku);
    }

    @Override
    public Mono<Boolean> existsById(Long id) {
        return repository.existsById(id);
    }

    // ========================================
    // Mapping Methods
    // ========================================

    private ProductEntity toEntity(Product product) {
        return ProductEntity.builder()
                .id(product.getId())
                .sku(product.getSku())
                .name(product.getName())
                .description(product.getDescription())
                .quantityOnHand(product.getQuantityOnHand())
                .unitPrice(product.getUnitPrice())
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .build();
    }

    private Product toDomain(ProductEntity entity) {
        return Product.builder()
                .id(entity.getId())
                .sku(entity.getSku())
                .name(entity.getName())
                .description(entity.getDescription())
                .quantityOnHand(entity.getQuantityOnHand())
                .unitPrice(entity.getUnitPrice())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
