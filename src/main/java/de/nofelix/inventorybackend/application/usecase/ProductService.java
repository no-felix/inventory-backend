package de.nofelix.inventorybackend.application.usecase;

import de.nofelix.inventorybackend.domain.exception.DuplicateSkuException;
import de.nofelix.inventorybackend.domain.exception.ProductNotFoundException;
import de.nofelix.inventorybackend.domain.model.Product;
import de.nofelix.inventorybackend.domain.port.in.CreateProductUseCase;
import de.nofelix.inventorybackend.domain.port.in.DeleteProductUseCase;
import de.nofelix.inventorybackend.domain.port.in.GetProductUseCase;
import de.nofelix.inventorybackend.domain.port.in.UpdateProductUseCase;
import de.nofelix.inventorybackend.domain.port.out.ProductRepositoryPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Service implementing all product-related use cases.
 * 
 * <p>This service orchestrates the business logic for product operations,
 * delegating persistence to the repository port.</p>
 */
@Service
@Transactional
public class ProductService implements 
        GetProductUseCase, 
        CreateProductUseCase, 
        UpdateProductUseCase, 
        DeleteProductUseCase {

    private static final Logger log = LoggerFactory.getLogger(ProductService.class);

    private final ProductRepositoryPort productRepository;

    public ProductService(ProductRepositoryPort productRepository) {
        this.productRepository = productRepository;
    }

    // ========================================
    // GetProductUseCase Implementation
    // ========================================

    @Override
    @Transactional(readOnly = true)
    public Mono<Product> getProductById(Long id) {
        log.debug("Getting product by ID: {}", id);
        return productRepository.findById(id)
                .switchIfEmpty(Mono.error(new ProductNotFoundException(id)));
    }

    @Override
    @Transactional(readOnly = true)
    public Flux<Product> listProducts(int page, int size) {
        log.debug("Listing products - page: {}, size: {}", page, size);
        return productRepository.findAll(page, size);
    }

    @Override
    @Transactional(readOnly = true)
    public Mono<Product> getProductBySku(String sku) {
        log.debug("Getting product by SKU: {}", sku);
        return productRepository.findBySku(sku)
                .switchIfEmpty(Mono.error(new ProductNotFoundException("Product with SKU '" + sku + "' not found")));
    }

    // ========================================
    // CreateProductUseCase Implementation
    // ========================================

    @Override
    public Mono<Product> createProduct(CreateProductCommand command) {
        log.info("Creating product with SKU: {}", command.sku());
        
        return productRepository.existsBySku(command.sku())
                .flatMap(exists -> {
                    if (exists) {
                        return Mono.error(new DuplicateSkuException(command.sku()));
                    }
                    
                    Product product = Product.builder()
                            .sku(command.sku())
                            .name(command.name())
                            .description(command.description())
                            .quantityOnHand(command.quantityOnHand())
                            .unitPrice(command.unitPrice())
                            .build();
                    
                    return productRepository.save(product);
                })
                .doOnSuccess(p -> log.info("Created product with ID: {}", p.getId()));
    }

    // ========================================
    // UpdateProductUseCase Implementation
    // ========================================

    @Override
    public Mono<Product> updateProduct(Long id, UpdateProductCommand command) {
        log.info("Updating product with ID: {}", id);
        
        return productRepository.findById(id)
                .switchIfEmpty(Mono.error(new ProductNotFoundException(id)))
                .flatMap(existingProduct -> {
                    // Check if SKU is being changed to an existing one
                    if (!existingProduct.getSku().equals(command.sku())) {
                        return productRepository.existsBySku(command.sku())
                                .flatMap(exists -> {
                                    if (exists) {
                                        return Mono.error(new DuplicateSkuException(command.sku()));
                                    }
                                    return Mono.just(existingProduct);
                                });
                    }
                    return Mono.just(existingProduct);
                })
                .flatMap(existingProduct -> {
                    Product updatedProduct = Product.builder()
                            .id(existingProduct.getId())
                            .sku(command.sku())
                            .name(command.name())
                            .description(command.description())
                            .quantityOnHand(command.quantityOnHand())
                            .unitPrice(command.unitPrice())
                            .createdAt(existingProduct.getCreatedAt())
                            .build();
                    
                    return productRepository.save(updatedProduct);
                })
                .doOnSuccess(p -> log.info("Updated product with ID: {}", p.getId()));
    }

    // ========================================
    // DeleteProductUseCase Implementation
    // ========================================

    @Override
    public Mono<Void> deleteProduct(Long id) {
        log.info("Deleting product with ID: {}", id);
        
        return productRepository.existsById(id)
                .flatMap(exists -> {
                    if (!exists) {
                        return Mono.error(new ProductNotFoundException(id));
                    }
                    return productRepository.deleteById(id);
                })
                .doOnSuccess(v -> log.info("Deleted product with ID: {}", id));
    }
}
