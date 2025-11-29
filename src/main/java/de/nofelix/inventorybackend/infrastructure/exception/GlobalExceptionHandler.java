package de.nofelix.inventorybackend.infrastructure.exception;

import de.nofelix.inventorybackend.domain.exception.AuthenticationException;
import de.nofelix.inventorybackend.domain.exception.DuplicateSkuException;
import de.nofelix.inventorybackend.domain.exception.ProductNotFoundException;
import de.nofelix.inventorybackend.domain.exception.PurchaseOrderNotFoundException;
import de.nofelix.inventorybackend.domain.exception.PurchaseOrderNotReceivableException;
import de.nofelix.inventorybackend.domain.exception.UserAlreadyExistsException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.time.Instant;

/**
 * Global exception handler for the application.
 * 
 * <p>Converts domain and validation exceptions to RFC 7807 Problem Detail responses.</p>
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final String ERROR_TYPE_BASE = "https://api.inventory.example.com/errors/";

    @ExceptionHandler(ProductNotFoundException.class)
    public Mono<ProblemDetail> handleProductNotFound(ProductNotFoundException ex) {
        log.warn("Product not found: {}", ex.getMessage());
        
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND, 
                ex.getMessage()
        );
        problemDetail.setType(URI.create(ERROR_TYPE_BASE + "product-not-found"));
        problemDetail.setTitle("Product Not Found");
        problemDetail.setProperty("timestamp", Instant.now());
        
        if (ex.getProductId() != null) {
            problemDetail.setProperty("productId", ex.getProductId());
        }
        
        return Mono.just(problemDetail);
    }

    @ExceptionHandler(PurchaseOrderNotFoundException.class)
    public Mono<ProblemDetail> handlePurchaseOrderNotFound(PurchaseOrderNotFoundException ex) {
        log.warn("Purchase order not found: {}", ex.getMessage());
        
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND, 
                ex.getMessage()
        );
        problemDetail.setType(URI.create(ERROR_TYPE_BASE + "purchase-order-not-found"));
        problemDetail.setTitle("Purchase Order Not Found");
        problemDetail.setProperty("timestamp", Instant.now());
        
        if (ex.getPurchaseOrderId() != null) {
            problemDetail.setProperty("purchaseOrderId", ex.getPurchaseOrderId());
        }
        
        return Mono.just(problemDetail);
    }

    @ExceptionHandler(PurchaseOrderNotReceivableException.class)
    public Mono<ProblemDetail> handlePurchaseOrderNotReceivable(PurchaseOrderNotReceivableException ex) {
        log.warn("Purchase order not receivable: {}", ex.getMessage());
        
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT, 
                ex.getMessage()
        );
        problemDetail.setType(URI.create(ERROR_TYPE_BASE + "purchase-order-not-receivable"));
        problemDetail.setTitle("Purchase Order Not Receivable");
        problemDetail.setProperty("timestamp", Instant.now());
        
        if (ex.getPurchaseOrderId() != null) {
            problemDetail.setProperty("purchaseOrderId", ex.getPurchaseOrderId());
        }
        
        return Mono.just(problemDetail);
    }

    @ExceptionHandler(DuplicateSkuException.class)
    public Mono<ProblemDetail> handleDuplicateSku(DuplicateSkuException ex) {
        log.warn("Duplicate SKU: {}", ex.getMessage());
        
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT, 
                ex.getMessage()
        );
        problemDetail.setType(URI.create(ERROR_TYPE_BASE + "duplicate-sku"));
        problemDetail.setTitle("Duplicate SKU");
        problemDetail.setProperty("timestamp", Instant.now());
        problemDetail.setProperty("sku", ex.getSku());
        
        return Mono.just(problemDetail);
    }

    @ExceptionHandler(AuthenticationException.class)
    public Mono<ProblemDetail> handleAuthentication(AuthenticationException ex) {
        log.warn("Authentication failed: {}", ex.getMessage());
        
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.UNAUTHORIZED, 
                ex.getMessage()
        );
        problemDetail.setType(URI.create(ERROR_TYPE_BASE + "authentication-failed"));
        problemDetail.setTitle("Authentication Failed");
        problemDetail.setProperty("timestamp", Instant.now());
        
        return Mono.just(problemDetail);
    }

    @ExceptionHandler(UserAlreadyExistsException.class)
    public Mono<ProblemDetail> handleUserAlreadyExists(UserAlreadyExistsException ex) {
        log.warn("User already exists: {}", ex.getMessage());
        
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT, 
                ex.getMessage()
        );
        problemDetail.setType(URI.create(ERROR_TYPE_BASE + "user-already-exists"));
        problemDetail.setTitle("User Already Exists");
        problemDetail.setProperty("timestamp", Instant.now());
        problemDetail.setProperty("field", ex.getField());
        problemDetail.setProperty("value", ex.getValue());
        
        return Mono.just(problemDetail);
    }

    @ExceptionHandler(WebExchangeBindException.class)
    public Mono<ProblemDetail> handleValidationErrors(WebExchangeBindException ex) {
        log.warn("Validation error: {}", ex.getMessage());
        
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, 
                "Validation failed for request"
        );
        problemDetail.setType(URI.create(ERROR_TYPE_BASE + "validation-error"));
        problemDetail.setTitle("Validation Error");
        problemDetail.setProperty("timestamp", Instant.now());
        
        var errors = ex.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> new ValidationError(
                        fieldError.getField(),
                        fieldError.getDefaultMessage(),
                        fieldError.getRejectedValue()
                ))
                .toList();
        
        problemDetail.setProperty("errors", errors);
        
        return Mono.just(problemDetail);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public Mono<ProblemDetail> handleIllegalArgument(IllegalArgumentException ex) {
        log.warn("Illegal argument: {}", ex.getMessage());
        
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, 
                ex.getMessage()
        );
        problemDetail.setType(URI.create(ERROR_TYPE_BASE + "bad-request"));
        problemDetail.setTitle("Bad Request");
        problemDetail.setProperty("timestamp", Instant.now());
        
        return Mono.just(problemDetail);
    }

    @ExceptionHandler(Exception.class)
    public Mono<ProblemDetail> handleGenericException(Exception ex) {
        log.error("Unexpected error: {}", ex.getMessage(), ex);
        
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR, 
                "An unexpected error occurred"
        );
        problemDetail.setType(URI.create(ERROR_TYPE_BASE + "internal-error"));
        problemDetail.setTitle("Internal Server Error");
        problemDetail.setProperty("timestamp", Instant.now());
        
        return Mono.just(problemDetail);
    }

    /**
     * Record for validation error details.
     */
    public record ValidationError(String field, String message, Object rejectedValue) {}
}
