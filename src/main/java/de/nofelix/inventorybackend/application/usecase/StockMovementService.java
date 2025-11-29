package de.nofelix.inventorybackend.application.usecase;

import de.nofelix.inventorybackend.domain.exception.InsufficientStockException;
import de.nofelix.inventorybackend.domain.exception.ProductNotFoundException;
import de.nofelix.inventorybackend.domain.model.StockMovement;
import de.nofelix.inventorybackend.domain.model.StockMovementReason;
import de.nofelix.inventorybackend.domain.port.in.CreateStockMovementUseCase;
import de.nofelix.inventorybackend.domain.port.in.GetStockMovementUseCase;
import de.nofelix.inventorybackend.domain.port.out.ProductRepositoryPort;
import de.nofelix.inventorybackend.domain.port.out.StockMovementRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Service implementing stock movement-related use cases.
 *
 * <p>This service provides read access to stock movements (audit trail)
 * and allows manual creation of stock movements for adjustments, damages,
 * sales, returns, and transfers.</p>
 */
@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class StockMovementService implements GetStockMovementUseCase, CreateStockMovementUseCase {

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

    @Override
    @Transactional
    public Mono<StockMovement> createStockMovement(CreateStockMovementCommand command) {
        log.info("Creating stock movement for product {} with change {} (reason: {})",
                command.productId(), command.change(), command.reason());

        return productRepository.findById(command.productId())
                .switchIfEmpty(Mono.error(new ProductNotFoundException(command.productId())))
                .flatMap(product -> {
                    int newQuantity = product.getQuantityOnHand() + command.change();

                    // Validate no negative stock
                    if (newQuantity < 0) {
                        return Mono.error(new InsufficientStockException(
                                product.getId(),
                                product.getQuantityOnHand(),
                                command.change()));
                    }

                    // Update product quantity
                    product.setQuantityOnHand(newQuantity);

                    return productRepository.save(product)
                            .then(Mono.defer(() -> {
                                // Create the stock movement
                                StockMovement movement = StockMovement.builder()
                                        .productId(command.productId())
                                        .change(command.change())
                                        .reason(command.reason())
                                        .performedBy(command.performedBy())
                                        .createdAt(Instant.now())
                                        .build();

                                return stockMovementRepository.save(movement);
                            }));
                })
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
