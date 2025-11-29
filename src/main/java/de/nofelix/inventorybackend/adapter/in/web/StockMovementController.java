package de.nofelix.inventorybackend.adapter.in.web;

import de.nofelix.inventorybackend.adapter.in.web.api.StockMovementsApi;
import de.nofelix.inventorybackend.adapter.in.web.model.StockMovementReason;
import de.nofelix.inventorybackend.adapter.in.web.model.StockMovementResponse;
import de.nofelix.inventorybackend.application.mapper.StockMovementMapper;
import de.nofelix.inventorybackend.domain.port.in.GetStockMovementUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;

/**
 * REST controller implementing the StockMovementsApi interface.
 * 
 * <p>This controller handles HTTP requests for stock movement (audit trail)
 * operations and delegates business logic to the appropriate use cases.</p>
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class StockMovementController implements StockMovementsApi {

    private final GetStockMovementUseCase getStockMovementUseCase;
    private final StockMovementMapper stockMovementMapper;

    @Override
    public Mono<ResponseEntity<Flux<StockMovementResponse>>> listStockMovements(
            Integer page,
            Integer size,
            Long productId,
            StockMovementReason reason,
            LocalDate from,
            LocalDate to,
            ServerWebExchange exchange) {
        log.debug("Received request to list stock movements - page: {}, size: {}, productId: {}, reason: {}, from: {}, to: {}", 
                page, size, productId, reason, from, to);
        
        Flux<StockMovementResponse> movements = getStockMovementUseCase.getStockMovements(
                        productId,
                        reason != null ? stockMovementMapper.toDomainReason(reason) : null,
                        from,
                        to)
                .skip((long) page * size)
                .take(size)
                .map(stockMovementMapper::toResponse);
        
        return Mono.just(ResponseEntity.ok(movements));
    }

    @Override
    public Mono<ResponseEntity<Flux<StockMovementResponse>>> getStockMovementsByProduct(
            Long productId,
            ServerWebExchange exchange) {
        log.debug("Received request to get stock movements for product ID: {}", productId);
        
        Flux<StockMovementResponse> movements = getStockMovementUseCase.getStockMovementsByProduct(productId)
                .map(stockMovementMapper::toResponse);
        
        return Mono.just(ResponseEntity.ok(movements));
    }
}
