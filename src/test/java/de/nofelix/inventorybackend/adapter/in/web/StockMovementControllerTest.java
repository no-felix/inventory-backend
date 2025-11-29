package de.nofelix.inventorybackend.adapter.in.web;

import de.nofelix.inventorybackend.adapter.in.web.model.StockMovementReason;
import de.nofelix.inventorybackend.adapter.in.web.model.StockMovementResponse;
import de.nofelix.inventorybackend.application.mapper.StockMovementMapper;
import de.nofelix.inventorybackend.domain.model.StockMovement;
import de.nofelix.inventorybackend.domain.port.in.GetStockMovementUseCase;
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

import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for StockMovementController.
 * 
 * <p>Tests REST endpoints with mocked use cases using WebTestClient.</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("StockMovementController")
class StockMovementControllerTest {

    private WebTestClient webTestClient;

    @Mock
    private GetStockMovementUseCase getStockMovementUseCase;

    @Mock
    private StockMovementMapper stockMovementMapper;

    private StockMovement sampleMovement;
    private StockMovementResponse sampleResponse;

    @BeforeEach
    void setUp() {
        StockMovementController controller = new StockMovementController(
                getStockMovementUseCase,
                stockMovementMapper
        );

        webTestClient = WebTestClient.bindToController(controller)
                .controllerAdvice(new GlobalExceptionHandler())
                .build();

        Instant now = Instant.now();
        OffsetDateTime nowOffset = now.atOffset(ZoneOffset.UTC);

        sampleMovement = StockMovement.builder()
                .id(1L)
                .productId(100L)
                .productSku("SKU-001")
                .productName("Test Product")
                .change(10)
                .reason(de.nofelix.inventorybackend.domain.model.StockMovementReason.PO_RECEIPT)
                .relatedEntityType("PURCHASE_ORDER")
                .relatedEntityId(50L)
                .performedBy("system")
                .createdAt(now)
                .build();

        sampleResponse = new StockMovementResponse()
                .id(1L)
                .productId(100L)
                .productSku("SKU-001")
                .productName("Test Product")
                .change(10)
                .reason(StockMovementReason.PO_RECEIPT)
                .relatedEntityType("PURCHASE_ORDER")
                .relatedEntityId(50L)
                .performedBy("system")
                .createdAt(nowOffset);
    }

    @Nested
    @DisplayName("GET /api/v1/stock-movements")
    class ListStockMovementsTests {

        @Test
        @DisplayName("should return 200 with list of stock movements")
        void listStockMovements_withMovements_returns200WithList() {
            // given
            StockMovement movement1 = StockMovement.builder()
                    .id(1L)
                    .productId(100L)
                    .change(10)
                    .reason(de.nofelix.inventorybackend.domain.model.StockMovementReason.PO_RECEIPT)
                    .createdAt(Instant.now())
                    .build();
            StockMovement movement2 = StockMovement.builder()
                    .id(2L)
                    .productId(100L)
                    .change(-5)
                    .reason(de.nofelix.inventorybackend.domain.model.StockMovementReason.SALE)
                    .createdAt(Instant.now())
                    .build();

            when(getStockMovementUseCase.getStockMovements(isNull(), isNull(), isNull(), isNull()))
                    .thenReturn(Flux.just(movement1, movement2));
            when(stockMovementMapper.toResponse(any(StockMovement.class))).thenAnswer(invocation -> {
                StockMovement sm = invocation.getArgument(0);
                return new StockMovementResponse()
                        .id(sm.getId())
                        .productId(sm.getProductId())
                        .change(sm.getChange())
                        .reason(sm.getReason() == de.nofelix.inventorybackend.domain.model.StockMovementReason.PO_RECEIPT
                                ? StockMovementReason.PO_RECEIPT : StockMovementReason.SALE);
            });

            // when/then
            webTestClient.get()
                    .uri("/api/v1/stock-movements?page=0&size=10")
                    .accept(MediaType.APPLICATION_JSON)
                    .exchange()
                    .expectStatus().isOk();

            verify(getStockMovementUseCase).getStockMovements(null, null, null, null);
        }

        @Test
        @DisplayName("should return 200 with filtered list when productId provided")
        void listStockMovements_withProductIdFilter_returnsFilteredList() {
            // given
            when(getStockMovementUseCase.getStockMovements(eq(100L), isNull(), isNull(), isNull()))
                    .thenReturn(Flux.just(sampleMovement));
            when(stockMovementMapper.toResponse(sampleMovement)).thenReturn(sampleResponse);

            // when/then
            webTestClient.get()
                    .uri("/api/v1/stock-movements?page=0&size=10&productId=100")
                    .accept(MediaType.APPLICATION_JSON)
                    .exchange()
                    .expectStatus().isOk();

            verify(getStockMovementUseCase).getStockMovements(100L, null, null, null);
        }

        @Test
        @DisplayName("should return 200 with filtered list when reason provided")
        void listStockMovements_withReasonFilter_returnsFilteredList() {
            // given
            when(stockMovementMapper.toDomainReason(StockMovementReason.PO_RECEIPT))
                    .thenReturn(de.nofelix.inventorybackend.domain.model.StockMovementReason.PO_RECEIPT);
            when(getStockMovementUseCase.getStockMovements(
                    isNull(), 
                    eq(de.nofelix.inventorybackend.domain.model.StockMovementReason.PO_RECEIPT), 
                    isNull(), 
                    isNull()))
                    .thenReturn(Flux.just(sampleMovement));
            when(stockMovementMapper.toResponse(sampleMovement)).thenReturn(sampleResponse);

            // when/then
            webTestClient.get()
                    .uri("/api/v1/stock-movements?page=0&size=10&reason=PO_RECEIPT")
                    .accept(MediaType.APPLICATION_JSON)
                    .exchange()
                    .expectStatus().isOk();

            verify(getStockMovementUseCase).getStockMovements(
                    null, 
                    de.nofelix.inventorybackend.domain.model.StockMovementReason.PO_RECEIPT, 
                    null, 
                    null);
        }

        @Test
        @DisplayName("should return 200 with filtered list when date range provided")
        void listStockMovements_withDateRangeFilter_returnsFilteredList() {
            // given
            LocalDate from = LocalDate.of(2024, 1, 1);
            LocalDate to = LocalDate.of(2024, 12, 31);

            when(getStockMovementUseCase.getStockMovements(isNull(), isNull(), eq(from), eq(to)))
                    .thenReturn(Flux.just(sampleMovement));
            when(stockMovementMapper.toResponse(sampleMovement)).thenReturn(sampleResponse);

            // when/then
            webTestClient.get()
                    .uri("/api/v1/stock-movements?page=0&size=10&from=2024-01-01&to=2024-12-31")
                    .accept(MediaType.APPLICATION_JSON)
                    .exchange()
                    .expectStatus().isOk();

            verify(getStockMovementUseCase).getStockMovements(null, null, from, to);
        }

        @Test
        @DisplayName("should return 200 with empty list when no movements")
        void listStockMovements_withNoMovements_returns200WithEmptyList() {
            // given
            when(getStockMovementUseCase.getStockMovements(isNull(), isNull(), isNull(), isNull()))
                    .thenReturn(Flux.empty());

            // when/then
            webTestClient.get()
                    .uri("/api/v1/stock-movements")
                    .accept(MediaType.APPLICATION_JSON)
                    .exchange()
                    .expectStatus().isOk();
        }

        @Test
        @DisplayName("should return 200 with all filters applied")
        void listStockMovements_withAllFilters_returnsFilteredList() {
            // given
            LocalDate from = LocalDate.of(2024, 1, 1);
            LocalDate to = LocalDate.of(2024, 12, 31);

            when(stockMovementMapper.toDomainReason(StockMovementReason.ADJUSTMENT))
                    .thenReturn(de.nofelix.inventorybackend.domain.model.StockMovementReason.ADJUSTMENT);
            when(getStockMovementUseCase.getStockMovements(
                    eq(100L),
                    eq(de.nofelix.inventorybackend.domain.model.StockMovementReason.ADJUSTMENT),
                    eq(from),
                    eq(to)))
                    .thenReturn(Flux.empty());

            // when/then
            webTestClient.get()
                    .uri("/api/v1/stock-movements?page=0&size=10&productId=100&reason=ADJUSTMENT&from=2024-01-01&to=2024-12-31")
                    .accept(MediaType.APPLICATION_JSON)
                    .exchange()
                    .expectStatus().isOk();

            verify(getStockMovementUseCase).getStockMovements(
                    100L,
                    de.nofelix.inventorybackend.domain.model.StockMovementReason.ADJUSTMENT,
                    from,
                    to);
        }
    }

    @Nested
    @DisplayName("GET /api/v1/stock-movements/product/{productId}")
    class GetStockMovementsByProductTests {

        @Test
        @DisplayName("should return 200 with stock movements for product")
        void getStockMovementsByProduct_withExistingProduct_returns200WithMovements() {
            // given
            StockMovement movement1 = StockMovement.builder()
                    .id(1L)
                    .productId(100L)
                    .change(10)
                    .reason(de.nofelix.inventorybackend.domain.model.StockMovementReason.PO_RECEIPT)
                    .createdAt(Instant.now())
                    .build();
            StockMovement movement2 = StockMovement.builder()
                    .id(2L)
                    .productId(100L)
                    .change(-3)
                    .reason(de.nofelix.inventorybackend.domain.model.StockMovementReason.SALE)
                    .createdAt(Instant.now())
                    .build();

            when(getStockMovementUseCase.getStockMovementsByProduct(100L))
                    .thenReturn(Flux.just(movement1, movement2));
            when(stockMovementMapper.toResponse(any(StockMovement.class))).thenAnswer(invocation -> {
                StockMovement sm = invocation.getArgument(0);
                return new StockMovementResponse()
                        .id(sm.getId())
                        .productId(sm.getProductId())
                        .change(sm.getChange())
                        .reason(sm.getReason() == de.nofelix.inventorybackend.domain.model.StockMovementReason.PO_RECEIPT
                                ? StockMovementReason.PO_RECEIPT : StockMovementReason.SALE);
            });

            // when/then
            webTestClient.get()
                    .uri("/api/v1/stock-movements/product/100")
                    .accept(MediaType.APPLICATION_JSON)
                    .exchange()
                    .expectStatus().isOk();

            verify(getStockMovementUseCase).getStockMovementsByProduct(100L);
        }

        @Test
        @DisplayName("should return 200 with empty list when product has no movements")
        void getStockMovementsByProduct_withNoMovements_returns200WithEmptyList() {
            // given
            when(getStockMovementUseCase.getStockMovementsByProduct(999L))
                    .thenReturn(Flux.empty());

            // when/then
            webTestClient.get()
                    .uri("/api/v1/stock-movements/product/999")
                    .accept(MediaType.APPLICATION_JSON)
                    .exchange()
                    .expectStatus().isOk();

            verify(getStockMovementUseCase).getStockMovementsByProduct(999L);
        }
    }
}
