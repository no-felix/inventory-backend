package de.nofelix.inventorybackend.adapter.in.web;

import de.nofelix.inventorybackend.adapter.in.web.model.InventorySummaryResponse;
import de.nofelix.inventorybackend.adapter.in.web.model.ReceiptsTimeSeriesResponse;
import de.nofelix.inventorybackend.adapter.in.web.model.StockLevelResponse;
import de.nofelix.inventorybackend.application.mapper.MetricsMapper;
import de.nofelix.inventorybackend.domain.port.in.GetMetricsUseCase;
import de.nofelix.inventorybackend.domain.port.in.GetMetricsUseCase.InventorySummary;
import de.nofelix.inventorybackend.domain.port.in.GetMetricsUseCase.ReceiptsTimeSeries;
import de.nofelix.inventorybackend.domain.port.in.GetMetricsUseCase.StockLevel;
import de.nofelix.inventorybackend.infrastructure.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for MetricsController.
 * 
 * <p>Tests REST endpoints with mocked use cases using WebTestClient.</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("MetricsController")
class MetricsControllerTest {

    private WebTestClient webTestClient;

    @Mock
    private GetMetricsUseCase getMetricsUseCase;

    @Mock
    private MetricsMapper metricsMapper;

    @BeforeEach
    void setUp() {
        MetricsController controller = new MetricsController(getMetricsUseCase, metricsMapper);

        webTestClient = WebTestClient.bindToController(controller)
                .controllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Nested
    @DisplayName("GET /api/v1/metrics/inventory-summary")
    class GetInventorySummaryTests {

        @Test
        @DisplayName("should return 200 with inventory summary")
        void getInventorySummary_returns200WithSummary() {
            // given
            InventorySummary domainSummary = new InventorySummary(
                    5, 1000, 3, new BigDecimal("50000.00")
            );
            
            InventorySummaryResponse response = new InventorySummaryResponse()
                    .productCount(5)
                    .totalItems(1000)
                    .lowStockCount(3)
                    .totalValue(50000.00);

            when(getMetricsUseCase.getInventorySummary()).thenReturn(Mono.just(domainSummary));
            when(metricsMapper.toResponse(domainSummary)).thenReturn(response);

            // when/then
            webTestClient.get()
                    .uri("/api/v1/metrics/inventory-summary")
                    .accept(MediaType.APPLICATION_JSON)
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody(InventorySummaryResponse.class)
                    .value(body -> {
                        assertThat(body.getProductCount()).isEqualTo(5);
                        assertThat(body.getTotalItems()).isEqualTo(1000);
                        assertThat(body.getLowStockCount()).isEqualTo(3);
                        assertThat(body.getTotalValue()).isEqualTo(50000.00);
                    });

            verify(getMetricsUseCase).getInventorySummary();
        }

        @Test
        @DisplayName("should return 200 with zero values when no products")
        void getInventorySummary_withNoProducts_returnsZeroValues() {
            // given
            InventorySummary domainSummary = new InventorySummary(0, 0, 0, BigDecimal.ZERO);
            
            InventorySummaryResponse response = new InventorySummaryResponse()
                    .productCount(0)
                    .totalItems(0)
                    .lowStockCount(0)
                    .totalValue(0.0);

            when(getMetricsUseCase.getInventorySummary()).thenReturn(Mono.just(domainSummary));
            when(metricsMapper.toResponse(domainSummary)).thenReturn(response);

            // when/then
            webTestClient.get()
                    .uri("/api/v1/metrics/inventory-summary")
                    .accept(MediaType.APPLICATION_JSON)
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody(InventorySummaryResponse.class)
                    .value(body -> {
                        assertThat(body.getProductCount()).isZero();
                        assertThat(body.getTotalItems()).isZero();
                        assertThat(body.getLowStockCount()).isZero();
                        assertThat(body.getTotalValue()).isZero();
                    });
        }
    }

    @Nested
    @DisplayName("GET /api/v1/metrics/stock-levels")
    class GetStockLevelsTests {

        @Test
        @DisplayName("should return 200 with stock levels")
        void getStockLevels_returns200WithStockLevels() {
            // given
            StockLevel level1 = new StockLevel("SKU-001", "Product A", 100, new BigDecimal("12.50"), new BigDecimal("1250.00"));
            StockLevel level2 = new StockLevel("SKU-002", "Product B", 50, new BigDecimal("25.00"), new BigDecimal("1250.00"));

            StockLevelResponse response1 = new StockLevelResponse()
                    .sku("SKU-001")
                    .name("Product A")
                    .quantity(100)
                    .unitPrice(12.50)
                    .totalValue(1250.00);
            StockLevelResponse response2 = new StockLevelResponse()
                    .sku("SKU-002")
                    .name("Product B")
                    .quantity(50)
                    .unitPrice(25.00)
                    .totalValue(1250.00);

            when(getMetricsUseCase.getStockLevels()).thenReturn(Flux.just(level1, level2));
            when(metricsMapper.toResponse(level1)).thenReturn(response1);
            when(metricsMapper.toResponse(level2)).thenReturn(response2);

            // when/then
            webTestClient.get()
                    .uri("/api/v1/metrics/stock-levels")
                    .accept(MediaType.APPLICATION_JSON)
                    .exchange()
                    .expectStatus().isOk()
                    .expectBodyList(StockLevelResponse.class)
                    .hasSize(2)
                    .value(list -> {
                        assertThat(list.get(0).getSku()).isEqualTo("SKU-001");
                        assertThat(list.get(0).getQuantity()).isEqualTo(100);
                        assertThat(list.get(1).getSku()).isEqualTo("SKU-002");
                        assertThat(list.get(1).getQuantity()).isEqualTo(50);
                    });

            verify(getMetricsUseCase).getStockLevels();
        }

        @Test
        @DisplayName("should return 200 with empty list when no products")
        void getStockLevels_withNoProducts_returnsEmptyList() {
            // given
            when(getMetricsUseCase.getStockLevels()).thenReturn(Flux.empty());

            // when/then
            webTestClient.get()
                    .uri("/api/v1/metrics/stock-levels")
                    .accept(MediaType.APPLICATION_JSON)
                    .exchange()
                    .expectStatus().isOk()
                    .expectBodyList(StockLevelResponse.class)
                    .hasSize(0);
        }

        @Test
        @DisplayName("should include products with low stock")
        void getStockLevels_withLowStock_includesLowStockProducts() {
            // given
            StockLevel lowStockProduct = new StockLevel("SKU-LOW", "Low Stock Item", 5, new BigDecimal("10.00"), new BigDecimal("50.00"));

            StockLevelResponse response = new StockLevelResponse()
                    .sku("SKU-LOW")
                    .name("Low Stock Item")
                    .quantity(5)
                    .unitPrice(10.00)
                    .totalValue(50.00);

            when(getMetricsUseCase.getStockLevels()).thenReturn(Flux.just(lowStockProduct));
            when(metricsMapper.toResponse(lowStockProduct)).thenReturn(response);

            // when/then
            webTestClient.get()
                    .uri("/api/v1/metrics/stock-levels")
                    .accept(MediaType.APPLICATION_JSON)
                    .exchange()
                    .expectStatus().isOk()
                    .expectBodyList(StockLevelResponse.class)
                    .hasSize(1)
                    .value(list -> {
                        assertThat(list.get(0).getQuantity()).isEqualTo(5);
                        assertThat(list.get(0).getSku()).isEqualTo("SKU-LOW");
                    });
        }
    }

    @Nested
    @DisplayName("GET /api/v1/metrics/receipts")
    class GetReceiptsTimeSeriesTests {

        @Test
        @DisplayName("should return 200 with receipts time series")
        void getReceiptsTimeSeries_returns200WithTimeSeries() {
            // given
            LocalDate from = LocalDate.of(2025, 1, 1);
            LocalDate to = LocalDate.of(2025, 1, 31);
            
            ReceiptsTimeSeries data1 = new ReceiptsTimeSeries(
                    LocalDate.of(2025, 1, 15), 100, 2
            );
            ReceiptsTimeSeries data2 = new ReceiptsTimeSeries(
                    LocalDate.of(2025, 1, 20), 200, 3
            );

            ReceiptsTimeSeriesResponse response1 = new ReceiptsTimeSeriesResponse()
                    .date(LocalDate.of(2025, 1, 15))
                    .totalQuantity(100)
                    .orderCount(2);
            ReceiptsTimeSeriesResponse response2 = new ReceiptsTimeSeriesResponse()
                    .date(LocalDate.of(2025, 1, 20))
                    .totalQuantity(200)
                    .orderCount(3);

            when(getMetricsUseCase.getReceiptsTimeSeries(from, to))
                    .thenReturn(Flux.just(data1, data2));
            when(metricsMapper.toResponse(data1)).thenReturn(response1);
            when(metricsMapper.toResponse(data2)).thenReturn(response2);

            // when/then
            webTestClient.get()
                    .uri("/api/v1/metrics/receipts?from=2025-01-01&to=2025-01-31")
                    .accept(MediaType.APPLICATION_JSON)
                    .exchange()
                    .expectStatus().isOk()
                    .expectBodyList(ReceiptsTimeSeriesResponse.class)
                    .hasSize(2)
                    .value(list -> {
                        assertThat(list.get(0).getDate()).isEqualTo(LocalDate.of(2025, 1, 15));
                        assertThat(list.get(0).getTotalQuantity()).isEqualTo(100);
                        assertThat(list.get(0).getOrderCount()).isEqualTo(2);
                        assertThat(list.get(1).getDate()).isEqualTo(LocalDate.of(2025, 1, 20));
                        assertThat(list.get(1).getTotalQuantity()).isEqualTo(200);
                    });

            verify(getMetricsUseCase).getReceiptsTimeSeries(from, to);
        }

        @Test
        @DisplayName("should return 200 with empty list when no receipts in date range")
        void getReceiptsTimeSeries_withNoReceipts_returnsEmptyList() {
            // given
            LocalDate from = LocalDate.of(2025, 1, 1);
            LocalDate to = LocalDate.of(2025, 1, 31);

            when(getMetricsUseCase.getReceiptsTimeSeries(from, to)).thenReturn(Flux.empty());

            // when/then
            webTestClient.get()
                    .uri("/api/v1/metrics/receipts?from=2025-01-01&to=2025-01-31")
                    .accept(MediaType.APPLICATION_JSON)
                    .exchange()
                    .expectStatus().isOk()
                    .expectBodyList(ReceiptsTimeSeriesResponse.class)
                    .hasSize(0);
        }

        @Test
        @DisplayName("should handle single day date range")
        void getReceiptsTimeSeries_withSingleDay_returnsSingleDayData() {
            // given
            LocalDate singleDate = LocalDate.of(2025, 1, 15);
            
            ReceiptsTimeSeries data = new ReceiptsTimeSeries(
                    singleDate, 50, 1
            );

            ReceiptsTimeSeriesResponse response = new ReceiptsTimeSeriesResponse()
                    .date(singleDate)
                    .totalQuantity(50)
                    .orderCount(1);

            when(getMetricsUseCase.getReceiptsTimeSeries(singleDate, singleDate))
                    .thenReturn(Flux.just(data));
            when(metricsMapper.toResponse(data)).thenReturn(response);

            // when/then
            webTestClient.get()
                    .uri("/api/v1/metrics/receipts?from=2025-01-15&to=2025-01-15")
                    .accept(MediaType.APPLICATION_JSON)
                    .exchange()
                    .expectStatus().isOk()
                    .expectBodyList(ReceiptsTimeSeriesResponse.class)
                    .hasSize(1)
                    .value(list -> {
                        assertThat(list.get(0).getDate()).isEqualTo(singleDate);
                        assertThat(list.get(0).getTotalQuantity()).isEqualTo(50);
                    });
        }
    }
}
