package de.nofelix.inventorybackend.adapter.out.persistence;

import de.nofelix.inventorybackend.adapter.out.persistence.entity.PurchaseOrderEntity;
import de.nofelix.inventorybackend.adapter.out.persistence.entity.PurchaseOrderLineEntity;
import de.nofelix.inventorybackend.adapter.out.persistence.repository.PurchaseOrderLineR2dbcRepository;
import de.nofelix.inventorybackend.adapter.out.persistence.repository.PurchaseOrderR2dbcRepository;
import de.nofelix.inventorybackend.domain.model.PurchaseOrder;
import de.nofelix.inventorybackend.domain.model.PurchaseOrderLine;
import de.nofelix.inventorybackend.domain.model.PurchaseOrderStatus;
import de.nofelix.inventorybackend.domain.port.out.PurchaseOrderRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.ArrayList;

/**
 * Persistence adapter implementing the PurchaseOrderRepositoryPort.
 * 
 * <p>This adapter translates between domain models and persistence entities,
 * delegating actual persistence operations to the R2DBC repositories.</p>
 */
@Component
@RequiredArgsConstructor
public class PurchaseOrderPersistenceAdapter implements PurchaseOrderRepositoryPort {

    private final PurchaseOrderR2dbcRepository orderRepository;
    private final PurchaseOrderLineR2dbcRepository lineRepository;

    @Override
    public Mono<PurchaseOrder> save(PurchaseOrder purchaseOrder) {
        PurchaseOrderEntity entity = toEntity(purchaseOrder);
        
        // Set timestamps for new entities
        if (entity.getId() == null) {
            entity.setCreatedAt(Instant.now());
            if (entity.getStatus() == null) {
                entity.setStatus(PurchaseOrderStatus.PENDING);
            }
        }
        
        return orderRepository.save(entity)
                .map(this::toDomain);
    }

    @Override
    public Mono<PurchaseOrderLine> saveLine(PurchaseOrderLine line) {
        return lineRepository.save(toLineEntity(line))
                .map(this::toLineDomain);
    }

    @Override
    public Flux<PurchaseOrderLine> saveLines(Flux<PurchaseOrderLine> lines) {
        return lines.map(this::toLineEntity)
                .collectList()
                .flatMapMany(lineRepository::saveAll)
                .map(this::toLineDomain);
    }

    @Override
    public Mono<PurchaseOrder> findById(Long id) {
        return orderRepository.findById(id)
                .map(this::toDomain);
    }

    @Override
    public Mono<PurchaseOrder> findByIdWithLines(Long id) {
        return orderRepository.findById(id)
                .flatMap(entity -> 
                    lineRepository.findByPurchaseOrderId(id)
                            .map(this::toLineDomain)
                            .collectList()
                            .map(lines -> {
                                PurchaseOrder order = toDomain(entity);
                                order.setLines(lines);
                                return order;
                            })
                );
    }

    @Override
    public Flux<PurchaseOrderLine> findLinesByPurchaseOrderId(Long purchaseOrderId) {
        return lineRepository.findByPurchaseOrderId(purchaseOrderId)
                .map(this::toLineDomain);
    }

    @Override
    public Flux<PurchaseOrder> findAll() {
        return orderRepository.findAllByOrderByCreatedAtDesc()
                .map(this::toDomain);
    }

    @Override
    public Flux<PurchaseOrder> findByStatus(PurchaseOrderStatus status) {
        return orderRepository.findByStatus(status)
                .map(this::toDomain);
    }

    @Override
    public Flux<PurchaseOrder> findAll(int page, int size) {
        return orderRepository.findAllByOrderByCreatedAtDesc()
                .skip((long) page * size)
                .take(size)
                .map(this::toDomain);
    }

    @Override
    public Mono<Boolean> existsById(Long id) {
        return orderRepository.existsById(id);
    }

    // ========================================
    // Mapping Methods - Purchase Order
    // ========================================

    private PurchaseOrderEntity toEntity(PurchaseOrder order) {
        return PurchaseOrderEntity.builder()
                .id(order.getId())
                .supplierName(order.getSupplierName())
                .status(order.getStatus())
                .createdAt(order.getCreatedAt())
                .receivedAt(order.getReceivedAt())
                .build();
    }

    private PurchaseOrder toDomain(PurchaseOrderEntity entity) {
        return PurchaseOrder.builder()
                .id(entity.getId())
                .supplierName(entity.getSupplierName())
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .receivedAt(entity.getReceivedAt())
                .lines(new ArrayList<>())
                .build();
    }

    // ========================================
    // Mapping Methods - Purchase Order Line
    // ========================================

    private PurchaseOrderLineEntity toLineEntity(PurchaseOrderLine line) {
        return PurchaseOrderLineEntity.builder()
                .id(line.getId())
                .purchaseOrderId(line.getPurchaseOrderId())
                .productId(line.getProductId())
                .quantity(line.getQuantity())
                .unitPrice(line.getUnitPrice())
                .build();
    }

    private PurchaseOrderLine toLineDomain(PurchaseOrderLineEntity entity) {
        return PurchaseOrderLine.builder()
                .id(entity.getId())
                .purchaseOrderId(entity.getPurchaseOrderId())
                .productId(entity.getProductId())
                .quantity(entity.getQuantity())
                .unitPrice(entity.getUnitPrice())
                .build();
    }
}
