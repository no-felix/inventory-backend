package de.nofelix.inventorybackend.application.usecase;

import de.nofelix.inventorybackend.domain.exception.ProductNotFoundException;
import de.nofelix.inventorybackend.domain.model.StockMovement;
import de.nofelix.inventorybackend.domain.model.StockMovementReason;
import de.nofelix.inventorybackend.domain.port.in.GetStockMovementUseCase;
import de.nofelix.inventorybackend.domain.port.out.ProductRepositoryPort;
import de.nofelix.inventorybackend.domain.port.out.StockMovementRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;

/**
 * Service implementing stock movement-related use cases.
 * 
 * <p>This service provides read access to stock movements (audit trail).
 * Stock movements are created automatically by other services (e.g., PurchaseOrderService)
 * when inventory changes occur.</p>
 */
@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class StockMovementService implements GetStockMovementUseCase {

    private final StockMovementRepositoryPort stockMovementRepository;
    private final ProductRepositoryPort productRepository;

    @Override
    public Flux<StockMovement> getStockMovements(
            Long productId,
            StockMovementReason reason,
            LocalDate from,
            LocalDate to) {
        log.debug("Getting stock movements with filters - productId: {}, reason: {}, from: {}, to: {}", 
                productId, reason, from, to);
        
        return stockMovementRepository.findWithFilters(productId, reason, from, to)
                .flatMap(this::enrichWithProductDetails);
    }

    @Override
    public Flux<StockMovement> getStockMovementsByProduct(Long productId) {
        log.debug("Getting stock movements for product ID: {}", productId);
        
        return productRepository.existsById(productId)
                .flatMapMany(exists -> {
                    if (!exists) {
                        return Flux.error(new ProductNotFoundException(productId));
                    }
                    return stockMovementRepository.findByProductId(productId)
                            .flatMap(this::enrichWithProductDetails);
                });
    }

    @Override
    public Mono<StockMovement> getStockMovementById(Long id) {
        log.debug("Getting stock movement by ID: {}", id);
        return stockMovementRepository.findById(id)
                .flatMap(this::enrichWithProductDetails);
    }

    /**
     * Enriches a stock movement with product details (SKU, name).
     */
    private Mono<StockMovement> enrichWithProductDetails(StockMovement movement) {
        return productRepository.findById(movement.getProductId())
                .map(product -> {
                    movement.setProductSku(product.getSku());
                    movement.setProductName(product.getName());
                    return movement;
                })
                .defaultIfEmpty(movement);
    }
}
