package de.nofelix.inventorybackend.application.mapper;

import de.nofelix.inventorybackend.adapter.in.web.model.StockMovementReason;
import de.nofelix.inventorybackend.adapter.in.web.model.StockMovementRequest;
import de.nofelix.inventorybackend.adapter.in.web.model.StockMovementResponse;
import de.nofelix.inventorybackend.domain.model.StockMovement;
import de.nofelix.inventorybackend.domain.port.in.CreateStockMovementUseCase.CreateStockMovementCommand;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

/**
 * MapStruct mapper for StockMovement conversions between domain and API objects.
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.WARN)
public interface StockMovementMapper {

    /**
     * Converts a StockMovement domain object to a StockMovementResponse.
     */
    @Mapping(target = "reason", source = "reason", qualifiedByName = "toApiReason")
    @Mapping(target = "createdAt", expression = "java(toOffsetDateTime(movement.getCreatedAt()))")
    StockMovementResponse toResponse(StockMovement movement);

    /**
     * Maps domain reason to API reason.
     */
    @Named("toApiReason")
    default StockMovementReason toApiReason(de.nofelix.inventorybackend.domain.model.StockMovementReason reason) {
        if (reason == null) {
            return null;
        }
        return StockMovementReason.fromValue(reason.name());
    }

    /**
     * Maps API reason to domain reason.
     */
    default de.nofelix.inventorybackend.domain.model.StockMovementReason toDomainReason(StockMovementReason reason) {
        if (reason == null) {
            return null;
        }
        return de.nofelix.inventorybackend.domain.model.StockMovementReason.valueOf(reason.getValue());
    }

    /**
     * Converts a StockMovementRequest to a CreateStockMovementCommand.
     */
    default CreateStockMovementCommand toCreateCommand(StockMovementRequest request) {
        return new CreateStockMovementCommand(
                request.getProductId(),
                request.getChange(),
                toDomainReason(StockMovementReason.fromValue(request.getReason().getValue())),
                request.getNotes(),
                request.getPerformedBy()
        );
    }

    /**
     * Helper method to convert Instant to OffsetDateTime.
     */
    default OffsetDateTime toOffsetDateTime(Instant instant) {
        return instant != null ? instant.atOffset(ZoneOffset.UTC) : null;
    }
}
