package de.nofelix.inventorybackend.adapter.in.web;

import de.nofelix.inventorybackend.adapter.in.web.model.PurchaseOrderLineRequest;
import de.nofelix.inventorybackend.adapter.in.web.model.PurchaseOrderLineResponse;
import de.nofelix.inventorybackend.adapter.in.web.model.PurchaseOrderRequest;
import de.nofelix.inventorybackend.adapter.in.web.model.PurchaseOrderResponse;
import de.nofelix.inventorybackend.adapter.in.web.model.PurchaseOrderStatus;
import de.nofelix.inventorybackend.application.mapper.PurchaseOrderMapper;
import de.nofelix.inventorybackend.domain.exception.PurchaseOrderNotFoundException;
import de.nofelix.inventorybackend.domain.exception.PurchaseOrderNotReceivableException;
import de.nofelix.inventorybackend.domain.model.PurchaseOrder;
import de.nofelix.inventorybackend.domain.model.PurchaseOrderLine;
import de.nofelix.inventorybackend.domain.port.in.CreatePurchaseOrderUseCase;
import de.nofelix.inventorybackend.domain.port.in.CreatePurchaseOrderUseCase.CreatePurchaseOrderCommand;
import de.nofelix.inventorybackend.domain.port.in.GetPurchaseOrderUseCase;
import de.nofelix.inventorybackend.domain.port.in.ReceivePurchaseOrderUseCase;
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
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for PurchaseOrderController.
 * 
 * <p>Tests REST endpoints with mocked use cases using WebTestClient.</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PurchaseOrderController")
class PurchaseOrderControllerTest {

    private WebTestClient webTestClient;

    @Mock
    private CreatePurchaseOrderUseCase createPurchaseOrderUseCase;

    @Mock
    private GetPurchaseOrderUseCase getPurchaseOrderUseCase;

    @Mock
    private ReceivePurchaseOrderUseCase receivePurchaseOrderUseCase;

    @Mock
    private PurchaseOrderMapper purchaseOrderMapper;

    private PurchaseOrder sampleOrder;
    private PurchaseOrderResponse sampleResponse;
    private PurchaseOrderRequest sampleRequest;

    @BeforeEach
    void setUp() {
        PurchaseOrderController controller = new PurchaseOrderController(
                createPurchaseOrderUseCase,
                getPurchaseOrderUseCase,
                receivePurchaseOrderUseCase,
                purchaseOrderMapper
        );

        webTestClient = WebTestClient.bindToController(controller)
                .controllerAdvice(new GlobalExceptionHandler())
                .build();

        Instant now = Instant.now();
        OffsetDateTime nowOffset = now.atOffset(ZoneOffset.UTC);

        PurchaseOrderLine sampleLine = PurchaseOrderLine.builder()
                .id(1L)
                .purchaseOrderId(1L)
                .productId(100L)
                .quantity(10)
                .unitPrice(new BigDecimal("25.00"))
                .build();

        sampleOrder = PurchaseOrder.builder()
                .id(1L)
                .supplierName("ACME Supplies")
                .status(de.nofelix.inventorybackend.domain.model.PurchaseOrderStatus.PENDING)
                .lines(List.of(sampleLine))
                .createdAt(now)
                .build();

        PurchaseOrderLineResponse lineResponse = new PurchaseOrderLineResponse()
                .id(1L)
                .productId(100L)
                .quantity(10)
                .unitPrice(25.0)
                .lineTotal(250.0);

        sampleResponse = new PurchaseOrderResponse()
                .id(1L)
                .supplierName("ACME Supplies")
                .status(PurchaseOrderStatus.PENDING)
                .lines(List.of(lineResponse))
                .totalAmount(250.0)
                .createdAt(nowOffset);

        PurchaseOrderLineRequest lineRequest = new PurchaseOrderLineRequest()
                .productId(100L)
                .quantity(10)
                .unitPrice(25.0);

        sampleRequest = new PurchaseOrderRequest()
                .supplierName("ACME Supplies")
                .lines(List.of(lineRequest))
                .received(false);
    }

    @Nested
    @DisplayName("POST /api/v1/purchase-orders")
    class CreatePurchaseOrderTests {

        @Test
        @DisplayName("should return 201 when purchase order created successfully")
        void createPurchaseOrder_withValidRequest_returns201WithOrder() {
            // given
            CreatePurchaseOrderCommand command = new CreatePurchaseOrderCommand(
                    "ACME Supplies",
                    List.of(new CreatePurchaseOrderCommand.LineItem(100L, 10, new BigDecimal("25.00"))),
                    false
            );

            when(purchaseOrderMapper.toCreateCommand(any(PurchaseOrderRequest.class))).thenReturn(command);
            when(createPurchaseOrderUseCase.createPurchaseOrder(any(CreatePurchaseOrderCommand.class)))
                    .thenReturn(Mono.just(sampleOrder));
            when(purchaseOrderMapper.toResponse(sampleOrder)).thenReturn(sampleResponse);

            // when/then
            webTestClient.post()
                    .uri("/api/v1/purchase-orders")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(sampleRequest)
                    .exchange()
                    .expectStatus().isCreated()
                    .expectBody(PurchaseOrderResponse.class)
                    .value(response -> {
                        assertThat(response.getId()).isEqualTo(1L);
                        assertThat(response.getSupplierName()).isEqualTo("ACME Supplies");
                        assertThat(response.getStatus()).isEqualTo(PurchaseOrderStatus.PENDING);
                    });
        }

        @Test
        @DisplayName("should return 201 when purchase order created and received")
        void createPurchaseOrder_withReceivedFlag_returns201WithReceivedOrder() {
            // given
            PurchaseOrderRequest receivedRequest = new PurchaseOrderRequest()
                    .supplierName("ACME Supplies")
                    .lines(List.of(new PurchaseOrderLineRequest()
                            .productId(100L)
                            .quantity(10)
                            .unitPrice(25.0)))
                    .received(true);

            CreatePurchaseOrderCommand command = new CreatePurchaseOrderCommand(
                    "ACME Supplies",
                    List.of(new CreatePurchaseOrderCommand.LineItem(100L, 10, new BigDecimal("25.00"))),
                    true
            );

            PurchaseOrder receivedOrder = PurchaseOrder.builder()
                    .id(1L)
                    .supplierName("ACME Supplies")
                    .status(de.nofelix.inventorybackend.domain.model.PurchaseOrderStatus.RECEIVED)
                    .lines(sampleOrder.getLines())
                    .createdAt(Instant.now())
                    .receivedAt(Instant.now())
                    .build();

            PurchaseOrderResponse receivedResponse = new PurchaseOrderResponse()
                    .id(1L)
                    .supplierName("ACME Supplies")
                    .status(PurchaseOrderStatus.RECEIVED)
                    .totalAmount(250.0);

            when(purchaseOrderMapper.toCreateCommand(any(PurchaseOrderRequest.class))).thenReturn(command);
            when(createPurchaseOrderUseCase.createPurchaseOrder(any(CreatePurchaseOrderCommand.class)))
                    .thenReturn(Mono.just(receivedOrder));
            when(purchaseOrderMapper.toResponse(receivedOrder)).thenReturn(receivedResponse);

            // when/then
            webTestClient.post()
                    .uri("/api/v1/purchase-orders")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(receivedRequest)
                    .exchange()
                    .expectStatus().isCreated()
                    .expectBody(PurchaseOrderResponse.class)
                    .value(response -> {
                        assertThat(response.getId()).isEqualTo(1L);
                        assertThat(response.getStatus()).isEqualTo(PurchaseOrderStatus.RECEIVED);
                    });
        }
    }

    @Nested
    @DisplayName("GET /api/v1/purchase-orders/{id}")
    class GetPurchaseOrderByIdTests {

        @Test
        @DisplayName("should return 200 with purchase order when found")
        void getPurchaseOrderById_withExistingId_returns200WithOrder() {
            // given
            when(getPurchaseOrderUseCase.getPurchaseOrderById(1L)).thenReturn(Mono.just(sampleOrder));
            when(purchaseOrderMapper.toResponse(sampleOrder)).thenReturn(sampleResponse);

            // when/then
            webTestClient.get()
                    .uri("/api/v1/purchase-orders/1")
                    .accept(MediaType.APPLICATION_JSON)
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody(PurchaseOrderResponse.class)
                    .value(response -> {
                        assertThat(response.getId()).isEqualTo(1L);
                        assertThat(response.getSupplierName()).isEqualTo("ACME Supplies");
                        assertThat(response.getStatus()).isEqualTo(PurchaseOrderStatus.PENDING);
                    });

            verify(getPurchaseOrderUseCase).getPurchaseOrderById(1L);
        }

        @Test
        @DisplayName("should return 404 when purchase order not found")
        void getPurchaseOrderById_withNonExistingId_returns404() {
            // given
            when(getPurchaseOrderUseCase.getPurchaseOrderById(999L))
                    .thenReturn(Mono.error(new PurchaseOrderNotFoundException(999L)));

            // when/then
            webTestClient.get()
                    .uri("/api/v1/purchase-orders/999")
                    .accept(MediaType.APPLICATION_JSON)
                    .exchange()
                    .expectStatus().isNotFound();
        }
    }

    @Nested
    @DisplayName("GET /api/v1/purchase-orders")
    class ListPurchaseOrdersTests {

        @Test
        @DisplayName("should return 200 with list of purchase orders")
        void listPurchaseOrders_withOrders_returns200WithList() {
            // given
            PurchaseOrder order1 = PurchaseOrder.builder()
                    .id(1L)
                    .supplierName("Supplier 1")
                    .status(de.nofelix.inventorybackend.domain.model.PurchaseOrderStatus.PENDING)
                    .build();
            PurchaseOrder order2 = PurchaseOrder.builder()
                    .id(2L)
                    .supplierName("Supplier 2")
                    .status(de.nofelix.inventorybackend.domain.model.PurchaseOrderStatus.RECEIVED)
                    .build();

            when(getPurchaseOrderUseCase.getPurchaseOrders(anyInt(), anyInt()))
                    .thenReturn(Flux.just(order1, order2));
            when(purchaseOrderMapper.toResponse(any(PurchaseOrder.class))).thenAnswer(invocation -> {
                PurchaseOrder po = invocation.getArgument(0);
                return new PurchaseOrderResponse()
                        .id(po.getId())
                        .supplierName(po.getSupplierName())
                        .status(po.getStatus() == de.nofelix.inventorybackend.domain.model.PurchaseOrderStatus.PENDING 
                                ? PurchaseOrderStatus.PENDING : PurchaseOrderStatus.RECEIVED);
            });

            // when/then
            webTestClient.get()
                    .uri("/api/v1/purchase-orders?page=0&size=10")
                    .accept(MediaType.APPLICATION_JSON)
                    .exchange()
                    .expectStatus().isOk();

            verify(getPurchaseOrderUseCase).getPurchaseOrders(0, 10);
        }

        @Test
        @DisplayName("should return 200 with filtered list when status provided")
        void listPurchaseOrders_withStatusFilter_returnsFilteredList() {
            // given
            PurchaseOrder pendingOrder = PurchaseOrder.builder()
                    .id(1L)
                    .supplierName("Supplier 1")
                    .status(de.nofelix.inventorybackend.domain.model.PurchaseOrderStatus.PENDING)
                    .build();

            when(purchaseOrderMapper.toDomainStatus(PurchaseOrderStatus.PENDING))
                    .thenReturn(de.nofelix.inventorybackend.domain.model.PurchaseOrderStatus.PENDING);
            when(getPurchaseOrderUseCase.getPurchaseOrdersByStatus(
                    de.nofelix.inventorybackend.domain.model.PurchaseOrderStatus.PENDING))
                    .thenReturn(Flux.just(pendingOrder));
            when(purchaseOrderMapper.toResponse(pendingOrder)).thenReturn(
                    new PurchaseOrderResponse()
                            .id(1L)
                            .supplierName("Supplier 1")
                            .status(PurchaseOrderStatus.PENDING));

            // when/then
            webTestClient.get()
                    .uri("/api/v1/purchase-orders?page=0&size=10&status=PENDING")
                    .accept(MediaType.APPLICATION_JSON)
                    .exchange()
                    .expectStatus().isOk();

            verify(getPurchaseOrderUseCase).getPurchaseOrdersByStatus(
                    de.nofelix.inventorybackend.domain.model.PurchaseOrderStatus.PENDING);
        }

        @Test
        @DisplayName("should return 200 with empty list when no purchase orders")
        void listPurchaseOrders_withNoOrders_returns200WithEmptyList() {
            // given
            when(getPurchaseOrderUseCase.getPurchaseOrders(anyInt(), anyInt())).thenReturn(Flux.empty());

            // when/then
            webTestClient.get()
                    .uri("/api/v1/purchase-orders")
                    .accept(MediaType.APPLICATION_JSON)
                    .exchange()
                    .expectStatus().isOk();
        }
    }

    @Nested
    @DisplayName("POST /api/v1/purchase-orders/{id}/receive")
    class ReceivePurchaseOrderTests {

        @Test
        @DisplayName("should return 200 when purchase order received successfully")
        void receivePurchaseOrder_withPendingOrder_returns200WithReceivedOrder() {
            // given
            PurchaseOrder receivedOrder = PurchaseOrder.builder()
                    .id(1L)
                    .supplierName("ACME Supplies")
                    .status(de.nofelix.inventorybackend.domain.model.PurchaseOrderStatus.RECEIVED)
                    .lines(sampleOrder.getLines())
                    .createdAt(Instant.now())
                    .receivedAt(Instant.now())
                    .build();

            PurchaseOrderResponse receivedResponse = new PurchaseOrderResponse()
                    .id(1L)
                    .supplierName("ACME Supplies")
                    .status(PurchaseOrderStatus.RECEIVED)
                    .totalAmount(250.0);

            when(receivePurchaseOrderUseCase.receivePurchaseOrder(1L))
                    .thenReturn(Mono.just(receivedOrder));
            when(purchaseOrderMapper.toResponse(receivedOrder)).thenReturn(receivedResponse);

            // when/then
            webTestClient.post()
                    .uri("/api/v1/purchase-orders/1/receive")
                    .accept(MediaType.APPLICATION_JSON)
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody(PurchaseOrderResponse.class)
                    .value(response -> {
                        assertThat(response.getId()).isEqualTo(1L);
                        assertThat(response.getStatus()).isEqualTo(PurchaseOrderStatus.RECEIVED);
                    });

            verify(receivePurchaseOrderUseCase).receivePurchaseOrder(1L);
        }

        @Test
        @DisplayName("should return 404 when purchase order not found")
        void receivePurchaseOrder_withNonExistingId_returns404() {
            // given
            when(receivePurchaseOrderUseCase.receivePurchaseOrder(999L))
                    .thenReturn(Mono.error(new PurchaseOrderNotFoundException(999L)));

            // when/then
            webTestClient.post()
                    .uri("/api/v1/purchase-orders/999/receive")
                    .accept(MediaType.APPLICATION_JSON)
                    .exchange()
                    .expectStatus().isNotFound();
        }

        @Test
        @DisplayName("should return 409 when purchase order already received")
        void receivePurchaseOrder_withAlreadyReceivedOrder_returns409() {
            // given
            when(receivePurchaseOrderUseCase.receivePurchaseOrder(1L))
                    .thenReturn(Mono.error(new PurchaseOrderNotReceivableException(1L, "Order already received")));

            // when/then
            webTestClient.post()
                    .uri("/api/v1/purchase-orders/1/receive")
                    .accept(MediaType.APPLICATION_JSON)
                    .exchange()
                    .expectStatus().isEqualTo(409);
        }

        @Test
        @DisplayName("should return 409 when purchase order cancelled")
        void receivePurchaseOrder_withCancelledOrder_returns409() {
            // given
            when(receivePurchaseOrderUseCase.receivePurchaseOrder(1L))
                    .thenReturn(Mono.error(new PurchaseOrderNotReceivableException(1L, "Order is cancelled")));

            // when/then
            webTestClient.post()
                    .uri("/api/v1/purchase-orders/1/receive")
                    .accept(MediaType.APPLICATION_JSON)
                    .exchange()
                    .expectStatus().isEqualTo(409);
        }
    }
}
