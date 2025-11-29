package de.nofelix.inventorybackend.adapter.in.web;

import de.nofelix.inventorybackend.adapter.in.web.api.PurchaseOrdersApi;
import de.nofelix.inventorybackend.adapter.in.web.model.PurchaseOrderRequest;
import de.nofelix.inventorybackend.adapter.in.web.model.PurchaseOrderResponse;
import de.nofelix.inventorybackend.adapter.in.web.model.PurchaseOrderStatus;
import de.nofelix.inventorybackend.application.mapper.PurchaseOrderMapper;
import de.nofelix.inventorybackend.domain.port.in.CreatePurchaseOrderUseCase;
import de.nofelix.inventorybackend.domain.port.in.GetPurchaseOrderUseCase;
import de.nofelix.inventorybackend.domain.port.in.ReceivePurchaseOrderUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * REST controller implementing the PurchaseOrdersApi interface.
 * 
 * <p>This controller handles HTTP requests for purchase order operations
 * and delegates business logic to the appropriate use cases.</p>
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class PurchaseOrderController implements PurchaseOrdersApi {

    private final CreatePurchaseOrderUseCase createPurchaseOrderUseCase;
    private final GetPurchaseOrderUseCase getPurchaseOrderUseCase;
    private final ReceivePurchaseOrderUseCase receivePurchaseOrderUseCase;
    private final PurchaseOrderMapper purchaseOrderMapper;

    @Override
    public Mono<ResponseEntity<PurchaseOrderResponse>> createPurchaseOrder(
            Mono<PurchaseOrderRequest> purchaseOrderRequest,
            ServerWebExchange exchange) {
        log.debug("Received request to create purchase order");
        
        return purchaseOrderRequest
                .map(purchaseOrderMapper::toCreateCommand)
                .flatMap(createPurchaseOrderUseCase::createPurchaseOrder)
                .map(purchaseOrderMapper::toResponse)
                .map(response -> ResponseEntity.status(HttpStatus.CREATED).body(response));
    }

    @Override
    public Mono<ResponseEntity<PurchaseOrderResponse>> getPurchaseOrderById(
            Long id,
            ServerWebExchange exchange) {
        log.debug("Received request to get purchase order with ID: {}", id);
        
        return getPurchaseOrderUseCase.getPurchaseOrderById(id)
                .map(purchaseOrderMapper::toResponse)
                .map(ResponseEntity::ok);
    }

    @Override
    public Mono<ResponseEntity<Flux<PurchaseOrderResponse>>> listPurchaseOrders(
            Integer page,
            Integer size,
            PurchaseOrderStatus status,
            ServerWebExchange exchange) {
        log.debug("Received request to list purchase orders - page: {}, size: {}, status: {}", page, size, status);
        
        Flux<PurchaseOrderResponse> orders;
        
        if (status != null) {
            orders = getPurchaseOrderUseCase.getPurchaseOrdersByStatus(
                            purchaseOrderMapper.toDomainStatus(status))
                    .map(purchaseOrderMapper::toResponse);
        } else {
            orders = getPurchaseOrderUseCase.getPurchaseOrders(page, size)
                    .map(purchaseOrderMapper::toResponse);
        }
        
        return Mono.just(ResponseEntity.ok(orders));
    }

    @Override
    public Mono<ResponseEntity<PurchaseOrderResponse>> receivePurchaseOrder(
            Long id,
            ServerWebExchange exchange) {
        log.debug("Received request to mark purchase order {} as received", id);
        
        return receivePurchaseOrderUseCase.receivePurchaseOrder(id)
                .map(purchaseOrderMapper::toResponse)
                .map(ResponseEntity::ok);
    }
}
