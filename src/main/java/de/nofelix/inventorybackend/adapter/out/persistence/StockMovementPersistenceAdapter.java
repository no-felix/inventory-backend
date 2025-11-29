package de.nofelix.inventorybackend.adapter.out.persistence;

import de.nofelix.inventorybackend.adapter.out.persistence.entity.StockMovementEntity;
import de.nofelix.inventorybackend.adapter.out.persistence.repository.StockMovementR2dbcRepository;
import de.nofelix.inventorybackend.domain.model.StockMovement;
import de.nofelix.inventorybackend.domain.model.StockMovementReason;
import de.nofelix.inventorybackend.domain.port.out.StockMovementRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

/**
 * Persistence adapter implementing the StockMovementRepositoryPort.
 * 
 * <p>This adapter translates between domain models and persistence entities,
 * delegating actual persistence operations to the R2DBC repository.</p>
 */
@Component
@RequiredArgsConstructor
public class StockMovementPersistenceAdapter implements StockMovementRepositoryPort {

    private final StockMovementR2dbcRepository repository;

    @Override
    public Mono<StockMovement> save(StockMovement stockMovement) {
        StockMovementEntity entity = toEntity(stockMovement);
        
        // Set timestamp for new entities
        if (entity.getId() == null && entity.getCreatedAt() == null) {
            entity.setCreatedAt(Instant.now());
        }
        
        return repository.save(entity)
                .map(this::toDomain);
    }

    @Override
    public Flux<StockMovement> saveAll(Flux<StockMovement> stockMovements) {
        return stockMovements
                .map(this::toEntity)
                .map(entity -> {
                    if (entity.getId() == null && entity.getCreatedAt() == null) {
                        entity.setCreatedAt(Instant.now());
                    }
                    return entity;
                })
                .collectList()
                .flatMapMany(repository::saveAll)
                .map(this::toDomain);
    }

    @Override
    public Mono<StockMovement> findById(Long id) {
        return repository.findById(id)
                .map(this::toDomain);
    }

    @Override
    public Flux<StockMovement> findAll() {
        return repository.findAll()
                .map(this::toDomain);
    }

    @Override
    public Flux<StockMovement> findByProductId(Long productId) {
        return repository.findByProductIdOrderByCreatedAtDesc(productId)
                .map(this::toDomain);
    }

    @Override
    public Flux<StockMovement> findByReason(StockMovementReason reason) {
        return repository.findByReasonOrderByCreatedAtDesc(reason)
                .map(this::toDomain);
    }

    @Override
    public Flux<StockMovement> findByDateRange(LocalDate from, LocalDate to) {
        Instant fromInstant = from.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant toInstant = to.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
        
        return repository.findByCreatedAtBetweenOrderByCreatedAtDesc(fromInstant, toInstant)
                .map(this::toDomain);
    }

    @Override
    public Flux<StockMovement> findWithFilters(
            Long productId,
            StockMovementReason reason,
            LocalDate from,
            LocalDate to) {
        
        Instant fromInstant = from != null 
                ? from.atStartOfDay(ZoneOffset.UTC).toInstant() 
                : null;
        Instant toInstant = to != null 
                ? to.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant() 
                : null;
        String reasonStr = reason != null ? reason.name() : null;
        
        return repository.findWithFilters(productId, reasonStr, fromInstant, toInstant)
                .map(this::toDomain);
    }

    @Override
    public Flux<StockMovement> findByRelatedEntity(String relatedEntityType, Long relatedEntityId) {
        return repository.findByRelatedEntityTypeAndRelatedEntityId(relatedEntityType, relatedEntityId)
                .map(this::toDomain);
    }

    // ========================================
    // Mapping Methods
    // ========================================

    private StockMovementEntity toEntity(StockMovement movement) {
        return StockMovementEntity.builder()
                .id(movement.getId())
                .productId(movement.getProductId())
                .change(movement.getChange())
                .reason(movement.getReason())
                .relatedEntityType(movement.getRelatedEntityType())
                .relatedEntityId(movement.getRelatedEntityId())
                .performedBy(movement.getPerformedBy())
                .createdAt(movement.getCreatedAt())
                .build();
    }

    private StockMovement toDomain(StockMovementEntity entity) {
        return StockMovement.builder()
                .id(entity.getId())
                .productId(entity.getProductId())
                .change(entity.getChange())
                .reason(entity.getReason())
                .relatedEntityType(entity.getRelatedEntityType())
                .relatedEntityId(entity.getRelatedEntityId())
                .performedBy(entity.getPerformedBy())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
