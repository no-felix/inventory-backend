package de.nofelix.inventorybackend.application.mapper;

import de.nofelix.inventorybackend.adapter.in.web.model.InventorySummaryResponse;
import de.nofelix.inventorybackend.adapter.in.web.model.ReceiptsTimeSeriesResponse;
import de.nofelix.inventorybackend.adapter.in.web.model.StockLevelResponse;
import de.nofelix.inventorybackend.domain.port.in.GetMetricsUseCase.InventorySummary;
import de.nofelix.inventorybackend.domain.port.in.GetMetricsUseCase.ReceiptsTimeSeries;
import de.nofelix.inventorybackend.domain.port.in.GetMetricsUseCase.StockLevel;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.math.BigDecimal;

/**
 * MapStruct mapper for metrics DTOs.
 */
@Mapper(componentModel = "spring")
public interface MetricsMapper {

    @Mapping(target = "productCount", source = "productCount")
    @Mapping(target = "totalItems", source = "totalItems")
    @Mapping(target = "lowStockCount", source = "lowStockCount")
    @Mapping(target = "totalValue", source = "totalValue")
    InventorySummaryResponse toResponse(InventorySummary summary);

    @Mapping(target = "sku", source = "sku")
    @Mapping(target = "name", source = "name")
    @Mapping(target = "quantity", source = "quantity")
    @Mapping(target = "unitPrice", source = "unitPrice")
    @Mapping(target = "totalValue", source = "totalValue")
    StockLevelResponse toResponse(StockLevel stockLevel);

    @Mapping(target = "date", source = "date")
    @Mapping(target = "totalQuantity", source = "totalQuantity")
    @Mapping(target = "orderCount", source = "orderCount")
    ReceiptsTimeSeriesResponse toResponse(ReceiptsTimeSeries timeSeries);

    // Helper methods for type conversion
    default Double toDouble(BigDecimal value) {
        return value != null ? value.doubleValue() : null;
    }
}
