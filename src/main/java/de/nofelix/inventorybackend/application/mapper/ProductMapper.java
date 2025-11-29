package de.nofelix.inventorybackend.application.mapper;

import de.nofelix.inventorybackend.adapter.in.web.model.ProductRequest;
import de.nofelix.inventorybackend.adapter.in.web.model.ProductResponse;
import de.nofelix.inventorybackend.domain.model.Product;
import de.nofelix.inventorybackend.domain.port.in.CreateProductUseCase.CreateProductCommand;
import de.nofelix.inventorybackend.domain.port.in.UpdateProductUseCase.UpdateProductCommand;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

/**
 * MapStruct mapper for Product conversions between domain, API, and command objects.
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.WARN)
public interface ProductMapper {

    /**
     * Converts a ProductRequest to a CreateProductCommand.
     */
    @Mapping(target = "unitPrice", expression = "java(toBigDecimal(request.getUnitPrice()))")
    CreateProductCommand toCreateCommand(ProductRequest request);

    /**
     * Converts a ProductRequest to an UpdateProductCommand.
     */
    @Mapping(target = "unitPrice", expression = "java(toBigDecimal(request.getUnitPrice()))")
    UpdateProductCommand toUpdateCommand(ProductRequest request);

    /**
     * Converts a Product domain object to a ProductResponse.
     */
    @Mapping(target = "unitPrice", expression = "java(toDouble(product.getUnitPrice()))")
    @Mapping(target = "createdAt", expression = "java(toOffsetDateTime(product.getCreatedAt()))")
    @Mapping(target = "updatedAt", expression = "java(toOffsetDateTime(product.getUpdatedAt()))")
    ProductResponse toResponse(Product product);

    /**
     * Helper method to convert Double to BigDecimal.
     */
    default BigDecimal toBigDecimal(Double value) {
        return value != null ? BigDecimal.valueOf(value) : null;
    }

    /**
     * Helper method to convert BigDecimal to Double.
     */
    default Double toDouble(BigDecimal value) {
        return value != null ? value.doubleValue() : null;
    }

    /**
     * Helper method to convert Instant to OffsetDateTime.
     */
    default OffsetDateTime toOffsetDateTime(Instant instant) {
        return instant != null ? instant.atOffset(ZoneOffset.UTC) : null;
    }
}
