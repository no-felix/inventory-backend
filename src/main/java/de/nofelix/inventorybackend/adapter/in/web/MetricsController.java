package de.nofelix.inventorybackend.adapter.in.web;

import de.nofelix.inventorybackend.adapter.in.web.api.MetricsApi;
import de.nofelix.inventorybackend.adapter.in.web.model.InventorySummaryResponse;
import de.nofelix.inventorybackend.adapter.in.web.model.LowStockAlertResponse;
import de.nofelix.inventorybackend.adapter.in.web.model.ReceiptsTimeSeriesResponse;
import de.nofelix.inventorybackend.adapter.in.web.model.SlowMovingItemResponse;
import de.nofelix.inventorybackend.adapter.in.web.model.StockLevelResponse;
import de.nofelix.inventorybackend.adapter.in.web.model.ValuationByPriceRangeResponse;
import de.nofelix.inventorybackend.application.mapper.MetricsMapper;
import de.nofelix.inventorybackend.domain.port.in.GetMetricsUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;

/**
 * REST controller for inventory metrics endpoints.
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class MetricsController implements MetricsApi {

    private final GetMetricsUseCase getMetricsUseCase;
    private final MetricsMapper metricsMapper;

    @Override
    public Mono<ResponseEntity<InventorySummaryResponse>> getInventorySummary(ServerWebExchange exchange) {
        log.info("GET /api/v1/metrics/inventory-summary");
        
        return getMetricsUseCase.getInventorySummary()
                .map(metricsMapper::toResponse)
                .map(ResponseEntity::ok);
    }

    @Override
    public Mono<ResponseEntity<Flux<StockLevelResponse>>> getStockLevels(ServerWebExchange exchange) {
        log.info("GET /api/v1/metrics/stock-levels");
        
        Flux<StockLevelResponse> stockLevels = getMetricsUseCase.getStockLevels()
                .map(metricsMapper::toResponse);
        
        return Mono.just(ResponseEntity.ok(stockLevels));
    }

    @Override
    public Mono<ResponseEntity<Flux<ReceiptsTimeSeriesResponse>>> getReceiptsTimeSeries(
            LocalDate from,
            LocalDate to,
            ServerWebExchange exchange) {
        log.info("GET /api/v1/metrics/receipts from={} to={}", from, to);
        
        Flux<ReceiptsTimeSeriesResponse> timeSeries = getMetricsUseCase.getReceiptsTimeSeries(from, to)
                .map(metricsMapper::toResponse);
        
        return Mono.just(ResponseEntity.ok(timeSeries));
    }

    @Override
    public Mono<ResponseEntity<Flux<LowStockAlertResponse>>> getLowStockAlerts(ServerWebExchange exchange) {
        log.info("GET /api/v1/metrics/low-stock-alerts");

        Flux<LowStockAlertResponse> alerts = getMetricsUseCase.getLowStockAlerts()
                .map(metricsMapper::toResponse);

        return Mono.just(ResponseEntity.ok(alerts));
    }

    @Override
    public Mono<ResponseEntity<Flux<SlowMovingItemResponse>>> getSlowMovingItems(
            Integer days,
            ServerWebExchange exchange) {
        int daysThreshold = days != null ? days : 30;
        log.info("GET /api/v1/metrics/slow-moving-items days={}", daysThreshold);

        Flux<SlowMovingItemResponse> items = getMetricsUseCase.getSlowMovingItems(daysThreshold)
                .map(metricsMapper::toResponse);

        return Mono.just(ResponseEntity.ok(items));
    }

    @Override
    public Mono<ResponseEntity<Flux<ValuationByPriceRangeResponse>>> getValuationByPriceRange(ServerWebExchange exchange) {
        log.info("GET /api/v1/metrics/valuation-by-price-range");

        Flux<ValuationByPriceRangeResponse> valuation = getMetricsUseCase.getValuationByPriceRange()
                .map(metricsMapper::toResponse);

        return Mono.just(ResponseEntity.ok(valuation));
    }
}
