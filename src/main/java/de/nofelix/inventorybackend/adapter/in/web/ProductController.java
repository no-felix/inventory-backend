package de.nofelix.inventorybackend.adapter.in.web;

import de.nofelix.inventorybackend.adapter.in.web.api.ProductsApi;
import de.nofelix.inventorybackend.adapter.in.web.model.PagedProductResponse;
import de.nofelix.inventorybackend.adapter.in.web.model.ProductRequest;
import de.nofelix.inventorybackend.adapter.in.web.model.ProductResponse;
import de.nofelix.inventorybackend.application.mapper.ProductMapper;
import de.nofelix.inventorybackend.domain.port.in.CreateProductUseCase;
import de.nofelix.inventorybackend.domain.port.in.DeleteProductUseCase;
import de.nofelix.inventorybackend.domain.port.in.GetProductUseCase;
import de.nofelix.inventorybackend.domain.port.in.UpdateProductUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * REST controller implementing the ProductsApi interface.
 * 
 * <p>This controller handles HTTP requests and delegates business logic
 * to the appropriate use cases.</p>
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class ProductController implements ProductsApi {

    private final GetProductUseCase getProductUseCase;
    private final CreateProductUseCase createProductUseCase;
    private final UpdateProductUseCase updateProductUseCase;
    private final DeleteProductUseCase deleteProductUseCase;
    private final ProductMapper productMapper;

    @Override
    public Mono<ResponseEntity<ProductResponse>> createProduct(
            Mono<ProductRequest> productRequest,
            ServerWebExchange exchange) {
        log.debug("Received request to create product");
        
        return productRequest
                .map(productMapper::toCreateCommand)
                .flatMap(createProductUseCase::createProduct)
                .map(productMapper::toResponse)
                .map(response -> ResponseEntity.status(HttpStatus.CREATED).body(response));
    }

    @Override
    public Mono<ResponseEntity<Void>> deleteProduct(
            Long id,
            ServerWebExchange exchange) {
        log.debug("Received request to delete product with ID: {}", id);
        
        return deleteProductUseCase.deleteProduct(id)
                .then(Mono.just(ResponseEntity.noContent().<Void>build()));
    }

    @Override
    public Mono<ResponseEntity<ProductResponse>> getProductById(
            Long id,
            ServerWebExchange exchange) {
        log.debug("Received request to get product with ID: {}", id);
        
        return getProductUseCase.getProductById(id)
                .map(productMapper::toResponse)
                .map(ResponseEntity::ok);
    }

    @Override
    public Mono<ResponseEntity<PagedProductResponse>> listProducts(
            Integer page,
            Integer size,
            String sort,
            ServerWebExchange exchange) {
        log.debug("Received request to list products - page: {}, size: {}, sort: {}", page, size, sort);
        
        return getProductUseCase.listProducts(page, size)
                .map(productMapper::toPagedResponse)
                .map(ResponseEntity::ok);
    }

    @Override
    public Mono<ResponseEntity<ProductResponse>> updateProduct(
            Long id,
            Mono<ProductRequest> productRequest,
            ServerWebExchange exchange) {
        log.debug("Received request to update product with ID: {}", id);
        
        return productRequest
                .map(productMapper::toUpdateCommand)
                .flatMap(command -> updateProductUseCase.updateProduct(id, command))
                .map(productMapper::toResponse)
                .map(ResponseEntity::ok);
    }
}
