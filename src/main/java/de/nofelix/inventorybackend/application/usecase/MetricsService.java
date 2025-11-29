package de.nofelix.inventorybackend.application.usecase;

import de.nofelix.inventorybackend.domain.model.StockMovementReason;
import de.nofelix.inventorybackend.domain.port.in.GetMetricsUseCase;
import de.nofelix.inventorybackend.domain.port.out.ProductRepositoryPort;
import de.nofelix.inventorybackend.domain.port.out.StockMovementRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;

/**
 * Service implementing inventory metrics use cases.
 */
@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class MetricsService implements GetMetricsUseCase {

    private static final int LOW_STOCK_THRESHOLD = 10;

    private final ProductRepositoryPort productRepository;
    private final StockMovementRepositoryPort stockMovementRepository;

    @Override
    public Mono<InventorySummary> getInventorySummary() {
        log.debug("Calculating inventory summary");
        
        return productRepository.findAll()
                .collectList()
                .map(products -> {
                    int productCount = products.size();
                    int totalItems = products.stream()
                            .mapToInt(p -> p.getQuantityOnHand() != null ? p.getQuantityOnHand() : 0)
                            .sum();
                    int lowStockCount = (int) products.stream()
                            .filter(p -> p.getQuantityOnHand() != null && p.getQuantityOnHand() <= LOW_STOCK_THRESHOLD)
                            .count();
                    BigDecimal totalValue = products.stream()
                            .map(p -> {
                                if (p.getUnitPrice() == null || p.getQuantityOnHand() == null) {
                                    return BigDecimal.ZERO;
                                }
                                return p.getUnitPrice().multiply(BigDecimal.valueOf(p.getQuantityOnHand()));
                            })
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    
                    return new InventorySummary(productCount, totalItems, lowStockCount, totalValue);
                })
                .doOnSuccess(summary -> log.info("Inventory summary: {} products, {} items, {} low stock, ${} value",
                        summary.productCount(), summary.totalItems(), summary.lowStockCount(), summary.totalValue()));
    }

    @Override
    public Flux<StockLevel> getStockLevels() {
        log.debug("Fetching stock levels for all products");
        
        return productRepository.findAll()
                .map(product -> {
                    BigDecimal totalValue = BigDecimal.ZERO;
                    if (product.getUnitPrice() != null && product.getQuantityOnHand() != null) {
                        totalValue = product.getUnitPrice().multiply(BigDecimal.valueOf(product.getQuantityOnHand()));
                    }
                    return new StockLevel(
                            product.getSku(),
                            product.getName(),
                            product.getQuantityOnHand() != null ? product.getQuantityOnHand() : 0,
                            product.getUnitPrice() != null ? product.getUnitPrice() : BigDecimal.ZERO,
                            totalValue
                    );
                });
    }

    @Override
    public Flux<ReceiptsTimeSeries> getReceiptsTimeSeries(LocalDate from, LocalDate to) {
        log.debug("Fetching receipts time series from {} to {}", from, to);
        
        return stockMovementRepository.findByDateRange(from, to)
                .filter(movement -> movement.getReason() == StockMovementReason.PO_RECEIPT)
                .filter(movement -> movement.getChange() > 0)
                .groupBy(movement -> movement.getCreatedAt().atZone(java.time.ZoneOffset.UTC).toLocalDate())
                .flatMap(groupedFlux -> groupedFlux.collectList()
                        .map(movements -> {
                            LocalDate date = groupedFlux.key();
                            int totalQuantity = movements.stream()
                                    .mapToInt(m -> m.getChange())
                                    .sum();
                            // Count distinct purchase orders
                            int orderCount = (int) movements.stream()
                                    .filter(m -> m.getRelatedEntityId() != null)
                                    .map(m -> m.getRelatedEntityId())
                                    .distinct()
                                    .count();
                            return new ReceiptsTimeSeries(date, totalQuantity, orderCount);
                        }))
                .sort(Comparator.comparing(ReceiptsTimeSeries::date));
    }
}
