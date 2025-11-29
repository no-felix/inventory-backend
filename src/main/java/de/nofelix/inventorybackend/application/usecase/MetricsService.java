package de.nofelix.inventorybackend.application.usecase;

import de.nofelix.inventorybackend.domain.model.StockMovementReason;
import de.nofelix.inventorybackend.domain.port.in.GetMetricsUseCase;
import de.nofelix.inventorybackend.domain.port.out.ProductRepositoryPort;
import de.nofelix.inventorybackend.domain.port.out.StockMovementRepositoryPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;

/**
 * Service implementing inventory metrics use cases.
 */
@Slf4j
@Service
@Transactional(readOnly = true)
public class MetricsService implements GetMetricsUseCase {

    private final int lowStockThreshold;
    private final ProductRepositoryPort productRepository;
    private final StockMovementRepositoryPort stockMovementRepository;

    public MetricsService(
            @Value("${inventory.low-stock-threshold:10}") int lowStockThreshold,
            ProductRepositoryPort productRepository,
            StockMovementRepositoryPort stockMovementRepository) {
        this.lowStockThreshold = lowStockThreshold;
        this.productRepository = productRepository;
        this.stockMovementRepository = stockMovementRepository;
    }

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
                            .filter(p -> p.getQuantityOnHand() != null && p.getQuantityOnHand() <= lowStockThreshold)
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

    @Override
    public Flux<LowStockAlert> getLowStockAlerts() {
        log.debug("Fetching low stock alerts (threshold: {})", lowStockThreshold);

        return productRepository.findAll()
                .filter(product -> product.getQuantityOnHand() != null 
                        && product.getQuantityOnHand() <= lowStockThreshold)
                .map(product -> new LowStockAlert(
                        product.getId(),
                        product.getSku(),
                        product.getName(),
                        product.getQuantityOnHand(),
                        lowStockThreshold,
                        lowStockThreshold - product.getQuantityOnHand()
                ))
                .sort(Comparator.comparing(LowStockAlert::deficit).reversed());
    }

    @Override
    public Flux<SlowMovingItem> getSlowMovingItems(int days) {
        log.debug("Fetching slow-moving items (no movement in {} days)", days);

        return productRepository.findAll()
                .flatMap(product -> stockMovementRepository.findLatestByProductId(product.getId())
                        .map(movement -> {
                            LocalDate lastMovementDate = movement.getCreatedAt()
                                    .atZone(ZoneOffset.UTC)
                                    .toLocalDate();
                            int daysSinceMovement = (int) ChronoUnit.DAYS.between(lastMovementDate, LocalDate.now());
                            BigDecimal totalValue = calculateTotalValue(product.getUnitPrice(), product.getQuantityOnHand());

                            return new SlowMovingItem(
                                    product.getId(),
                                    product.getSku(),
                                    product.getName(),
                                    product.getQuantityOnHand() != null ? product.getQuantityOnHand() : 0,
                                    totalValue,
                                    lastMovementDate,
                                    daysSinceMovement
                            );
                        })
                        .filter(item -> item.daysSinceLastMovement() >= days)
                        .switchIfEmpty(Mono.defer(() -> {
                            // Product has no movements at all - consider it slow-moving
                            BigDecimal totalValue = calculateTotalValue(product.getUnitPrice(), product.getQuantityOnHand());
                            LocalDate createdDate = product.getCreatedAt() != null
                                    ? product.getCreatedAt().atZone(ZoneOffset.UTC).toLocalDate()
                                    : LocalDate.now();
                            int daysSinceCreation = (int) ChronoUnit.DAYS.between(createdDate, LocalDate.now());
                            
                            if (daysSinceCreation >= days) {
                                return Mono.just(new SlowMovingItem(
                                        product.getId(),
                                        product.getSku(),
                                        product.getName(),
                                        product.getQuantityOnHand() != null ? product.getQuantityOnHand() : 0,
                                        totalValue,
                                        null,
                                        daysSinceCreation
                                ));
                            }
                            return Mono.empty();
                        }))
                )
                .sort(Comparator.comparing(SlowMovingItem::daysSinceLastMovement).reversed());
    }

    @Override
    public Flux<ValuationByPriceRange> getValuationByPriceRange() {
        log.debug("Calculating inventory valuation by price range");

        return productRepository.findAll()
                .collectList()
                .flatMapMany(products -> {
                    // Define price ranges
                    var under10 = products.stream()
                            .filter(p -> p.getUnitPrice() != null && p.getUnitPrice().compareTo(BigDecimal.TEN) < 0)
                            .toList();
                    var from10to50 = products.stream()
                            .filter(p -> p.getUnitPrice() != null 
                                    && p.getUnitPrice().compareTo(BigDecimal.TEN) >= 0
                                    && p.getUnitPrice().compareTo(BigDecimal.valueOf(50)) < 0)
                            .toList();
                    var from50to100 = products.stream()
                            .filter(p -> p.getUnitPrice() != null 
                                    && p.getUnitPrice().compareTo(BigDecimal.valueOf(50)) >= 0
                                    && p.getUnitPrice().compareTo(BigDecimal.valueOf(100)) < 0)
                            .toList();
                    var over100 = products.stream()
                            .filter(p -> p.getUnitPrice() != null 
                                    && p.getUnitPrice().compareTo(BigDecimal.valueOf(100)) >= 0)
                            .toList();

                    return Flux.just(
                            createValuationByRange("Under $10", under10),
                            createValuationByRange("$10 - $50", from10to50),
                            createValuationByRange("$50 - $100", from50to100),
                            createValuationByRange("Over $100", over100)
                    );
                });
    }

    private ValuationByPriceRange createValuationByRange(String range, java.util.List<de.nofelix.inventorybackend.domain.model.Product> products) {
        int productCount = products.size();
        int totalQuantity = products.stream()
                .mapToInt(p -> p.getQuantityOnHand() != null ? p.getQuantityOnHand() : 0)
                .sum();
        BigDecimal totalValue = products.stream()
                .map(p -> calculateTotalValue(p.getUnitPrice(), p.getQuantityOnHand()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new ValuationByPriceRange(range, productCount, totalQuantity, totalValue);
    }

    private BigDecimal calculateTotalValue(BigDecimal unitPrice, Integer quantity) {
        if (unitPrice == null || quantity == null) {
            return BigDecimal.ZERO;
        }
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }
}
