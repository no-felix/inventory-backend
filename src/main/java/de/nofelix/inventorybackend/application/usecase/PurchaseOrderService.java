package de.nofelix.inventorybackend.application.usecase;

import de.nofelix.inventorybackend.domain.exception.ProductNotFoundException;
import de.nofelix.inventorybackend.domain.exception.PurchaseOrderNotFoundException;
import de.nofelix.inventorybackend.domain.exception.PurchaseOrderNotReceivableException;
import de.nofelix.inventorybackend.domain.model.Product;
import de.nofelix.inventorybackend.domain.model.PurchaseOrder;
import de.nofelix.inventorybackend.domain.model.PurchaseOrderLine;
import de.nofelix.inventorybackend.domain.model.PurchaseOrderStatus;
import de.nofelix.inventorybackend.domain.model.StockMovement;
import de.nofelix.inventorybackend.domain.port.in.CreatePurchaseOrderUseCase;
import de.nofelix.inventorybackend.domain.port.in.GetPurchaseOrderUseCase;
import de.nofelix.inventorybackend.domain.port.in.ReceivePurchaseOrderUseCase;
import de.nofelix.inventorybackend.domain.port.out.ProductRepositoryPort;
import de.nofelix.inventorybackend.domain.port.out.PurchaseOrderRepositoryPort;
import de.nofelix.inventorybackend.domain.port.out.StockMovementRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;

/**
 * Service implementing all purchase order-related use cases.
 * 
 * <p>This service orchestrates the business logic for purchase order operations,
 * including stock updates and movement recording when orders are received.</p>
 */
@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class PurchaseOrderService implements 
        CreatePurchaseOrderUseCase, 
        GetPurchaseOrderUseCase, 
        ReceivePurchaseOrderUseCase {

    private final PurchaseOrderRepositoryPort purchaseOrderRepository;
    private final ProductRepositoryPort productRepository;
    private final StockMovementRepositoryPort stockMovementRepository;

    // ========================================
    // CreatePurchaseOrderUseCase Implementation
    // ========================================

    @Override
    public Mono<PurchaseOrder> createPurchaseOrder(CreatePurchaseOrderCommand command) {
        log.info("Creating purchase order from supplier: {}", command.supplierName());
        
        // Validate all products exist first
        return validateProductsExist(command.lines())
                .then(Mono.defer(() -> {
                    // Create the purchase order
                    PurchaseOrder order = PurchaseOrder.builder()
                            .supplierName(command.supplierName())
                            .status(PurchaseOrderStatus.PENDING)
                            .createdAt(Instant.now())
                            .build();
                    
                    return purchaseOrderRepository.save(order);
                }))
                .flatMap(savedOrder -> {
                    // Create and save all lines
                    List<PurchaseOrderLine> lines = command.lines().stream()
                            .map(lineItem -> PurchaseOrderLine.builder()
                                    .purchaseOrderId(savedOrder.getId())
                                    .productId(lineItem.productId())
                                    .quantity(lineItem.quantity())
                                    .unitPrice(lineItem.unitPrice())
                                    .build())
                            .toList();
                    
                    return purchaseOrderRepository.saveLines(Flux.fromIterable(lines))
                            .collectList()
                            .map(savedLines -> {
                                savedOrder.setLines(savedLines);
                                return savedOrder;
                            });
                })
                .flatMap(orderWithLines -> {
                    // If received flag is set, immediately process the order
                    if (command.received()) {
                        return processOrderReceipt(orderWithLines);
                    }
                    return Mono.just(orderWithLines);
                })
                .doOnSuccess(order -> log.info("Created purchase order with ID: {}", order.getId()));
    }

    // ========================================
    // GetPurchaseOrderUseCase Implementation
    // ========================================

    @Override
    @Transactional(readOnly = true)
    public Mono<PurchaseOrder> getPurchaseOrderById(Long id) {
        log.debug("Getting purchase order by ID: {}", id);
        return purchaseOrderRepository.findByIdWithLines(id)
                .flatMap(this::enrichOrderWithProductDetails)
                .switchIfEmpty(Mono.error(new PurchaseOrderNotFoundException(id)));
    }

    @Override
    @Transactional(readOnly = true)
    public Flux<PurchaseOrder> getAllPurchaseOrders() {
        log.debug("Getting all purchase orders");
        return purchaseOrderRepository.findAll()
                .flatMap(order -> purchaseOrderRepository.findLinesByPurchaseOrderId(order.getId())
                        .collectList()
                        .map(lines -> {
                            order.setLines(lines);
                            return order;
                        }));
    }

    @Override
    @Transactional(readOnly = true)
    public Flux<PurchaseOrder> getPurchaseOrders(int page, int size) {
        log.debug("Getting purchase orders - page: {}, size: {}", page, size);
        return purchaseOrderRepository.findAll(page, size)
                .flatMap(order -> purchaseOrderRepository.findLinesByPurchaseOrderId(order.getId())
                        .collectList()
                        .map(lines -> {
                            order.setLines(lines);
                            return order;
                        }));
    }

    @Override
    @Transactional(readOnly = true)
    public Flux<PurchaseOrder> getPurchaseOrdersByStatus(PurchaseOrderStatus status) {
        log.debug("Getting purchase orders by status: {}", status);
        return purchaseOrderRepository.findByStatus(status);
    }

    // ========================================
    // ReceivePurchaseOrderUseCase Implementation
    // ========================================

    @Override
    public Mono<PurchaseOrder> receivePurchaseOrder(Long id) {
        log.info("Receiving purchase order with ID: {}", id);
        
        return purchaseOrderRepository.findByIdWithLines(id)
                .switchIfEmpty(Mono.error(new PurchaseOrderNotFoundException(id)))
                .flatMap(order -> {
                    if (!order.canBeReceived()) {
                        return Mono.error(new PurchaseOrderNotReceivableException(
                                id, "Order is not in PENDING status. Current status: " + order.getStatus()));
                    }
                    return processOrderReceipt(order);
                })
                .doOnSuccess(order -> log.info("Received purchase order with ID: {}", id));
    }

    // ========================================
    // Private Helper Methods
    // ========================================

    /**
     * Validates that all products in the order lines exist.
     */
    private Mono<Void> validateProductsExist(List<CreatePurchaseOrderCommand.LineItem> lines) {
        return Flux.fromIterable(lines)
                .flatMap(line -> productRepository.existsById(line.productId())
                        .flatMap(exists -> {
                            if (!exists) {
                                return Mono.error(new ProductNotFoundException(line.productId()));
                            }
                            return Mono.empty();
                        }))
                .then();
    }

    /**
     * Processes a purchase order receipt: updates stock levels and creates movements.
     */
    private Mono<PurchaseOrder> processOrderReceipt(PurchaseOrder order) {
        log.debug("Processing receipt for order ID: {}", order.getId());
        
        return Flux.fromIterable(order.getLines())
                .flatMap(line -> updateProductStock(line, order.getId()))
                .then(Mono.defer(() -> {
                    order.markAsReceived();
                    return purchaseOrderRepository.save(order);
                }))
                .map(savedOrder -> {
                    savedOrder.setLines(order.getLines());
                    return savedOrder;
                });
    }

    /**
     * Updates product stock and creates a stock movement for a single line.
     */
    private Mono<Void> updateProductStock(PurchaseOrderLine line, Long orderId) {
        return productRepository.findById(line.getProductId())
                .flatMap(product -> {
                    // Update the product's stock
                    product.increaseStock(line.getQuantity());
                    return productRepository.save(product);
                })
                .flatMap(savedProduct -> {
                    // Create a stock movement record
                    StockMovement movement = StockMovement.forPurchaseOrderReceipt(
                            line.getProductId(),
                            line.getQuantity(),
                            orderId,
                            null // TODO: Get current user when auth is implemented
                    );
                    return stockMovementRepository.save(movement);
                })
                .then();
    }

    /**
     * Enriches order lines with product details (SKU, name).
     */
    private Mono<PurchaseOrder> enrichOrderWithProductDetails(PurchaseOrder order) {
        return Flux.fromIterable(order.getLines())
                .flatMap(line -> productRepository.findById(line.getProductId())
                        .map(product -> {
                            line.setProductSku(product.getSku());
                            line.setProductName(product.getName());
                            return line;
                        })
                        .defaultIfEmpty(line))
                .collectList()
                .map(lines -> {
                    order.setLines(lines);
                    return order;
                });
    }
}
