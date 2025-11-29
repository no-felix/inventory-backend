package de.nofelix.inventorybackend.application.mapper;

import de.nofelix.inventorybackend.adapter.in.web.model.PurchaseOrderLineRequest;
import de.nofelix.inventorybackend.adapter.in.web.model.PurchaseOrderLineResponse;
import de.nofelix.inventorybackend.adapter.in.web.model.PurchaseOrderRequest;
import de.nofelix.inventorybackend.adapter.in.web.model.PurchaseOrderResponse;
import de.nofelix.inventorybackend.adapter.in.web.model.PurchaseOrderStatus;
import de.nofelix.inventorybackend.domain.model.PurchaseOrder;
import de.nofelix.inventorybackend.domain.model.PurchaseOrderLine;
import de.nofelix.inventorybackend.domain.port.in.CreatePurchaseOrderUseCase.CreatePurchaseOrderCommand;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

/**
 * MapStruct mapper for PurchaseOrder conversions between domain, API, and command objects.
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.WARN)
public interface PurchaseOrderMapper {

    /**
     * Converts a PurchaseOrderRequest to a CreatePurchaseOrderCommand.
     */
    @Mapping(target = "lines", source = "lines", qualifiedByName = "toLineItems")
    @Mapping(target = "received", source = "received", defaultValue = "false")
    CreatePurchaseOrderCommand toCreateCommand(PurchaseOrderRequest request);

    /**
     * Converts line requests to line items.
     */
    @Named("toLineItems")
    default List<CreatePurchaseOrderCommand.LineItem> toLineItems(List<PurchaseOrderLineRequest> lines) {
        if (lines == null) {
            return List.of();
        }
        return lines.stream()
                .map(this::toLineItem)
                .toList();
    }

    /**
     * Converts a single line request to a line item.
     */
    default CreatePurchaseOrderCommand.LineItem toLineItem(PurchaseOrderLineRequest request) {
        return new CreatePurchaseOrderCommand.LineItem(
                request.getProductId(),
                request.getQuantity(),
                toBigDecimal(request.getUnitPrice())
        );
    }

    /**
     * Converts a PurchaseOrder domain object to a PurchaseOrderResponse.
     */
    @Mapping(target = "totalAmount", expression = "java(toDouble(order.calculateTotalAmount()))")
    @Mapping(target = "status", source = "status", qualifiedByName = "toApiStatus")
    @Mapping(target = "createdAt", expression = "java(toOffsetDateTime(order.getCreatedAt()))")
    @Mapping(target = "receivedAt", expression = "java(toOffsetDateTime(order.getReceivedAt()))")
    @Mapping(target = "lines", source = "lines")
    PurchaseOrderResponse toResponse(PurchaseOrder order);

    /**
     * Converts a PurchaseOrderLine domain object to a PurchaseOrderLineResponse.
     */
    @Mapping(target = "lineTotal", expression = "java(toDouble(line.calculateLineTotal()))")
    @Mapping(target = "unitPrice", expression = "java(toDouble(line.getUnitPrice()))")
    PurchaseOrderLineResponse toLineResponse(PurchaseOrderLine line);

    /**
     * Maps domain status to API status.
     */
    @Named("toApiStatus")
    default PurchaseOrderStatus toApiStatus(de.nofelix.inventorybackend.domain.model.PurchaseOrderStatus status) {
        if (status == null) {
            return null;
        }
        return PurchaseOrderStatus.fromValue(status.name());
    }

    /**
     * Maps API status to domain status.
     */
    default de.nofelix.inventorybackend.domain.model.PurchaseOrderStatus toDomainStatus(PurchaseOrderStatus status) {
        if (status == null) {
            return null;
        }
        return de.nofelix.inventorybackend.domain.model.PurchaseOrderStatus.valueOf(status.getValue());
    }

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
