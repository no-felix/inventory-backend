package de.nofelix.inventorybackend.application.usecase;

import de.nofelix.inventorybackend.domain.exception.ProductNotFoundException;
import de.nofelix.inventorybackend.domain.exception.PurchaseOrderNotFoundException;
import de.nofelix.inventorybackend.domain.exception.PurchaseOrderNotReceivableException;
import de.nofelix.inventorybackend.domain.model.Product;
import de.nofelix.inventorybackend.domain.model.PurchaseOrder;
import de.nofelix.inventorybackend.domain.model.PurchaseOrderLine;
import de.nofelix.inventorybackend.domain.model.PurchaseOrderStatus;
import de.nofelix.inventorybackend.domain.model.StockMovement;
import de.nofelix.inventorybackend.domain.port.in.CreatePurchaseOrderUseCase.CreatePurchaseOrderCommand;
import de.nofelix.inventorybackend.domain.port.out.ProductRepositoryPort;
import de.nofelix.inventorybackend.domain.port.out.PurchaseOrderRepositoryPort;
import de.nofelix.inventorybackend.domain.port.out.StockMovementRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for PurchaseOrderService.
 * 
 * <p>Tests use case implementations with mocked repository ports.</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PurchaseOrderService")
class PurchaseOrderServiceTest {

    @Mock
    private PurchaseOrderRepositoryPort purchaseOrderRepository;

    @Mock
    private ProductRepositoryPort productRepository;

    @Mock
    private StockMovementRepositoryPort stockMovementRepository;

    @InjectMocks
    private PurchaseOrderService purchaseOrderService;

    @Captor
    private ArgumentCaptor<PurchaseOrder> orderCaptor;

    @Captor
    private ArgumentCaptor<StockMovement> movementCaptor;

    private Product sampleProduct;
    private PurchaseOrder sampleOrder;
    private PurchaseOrderLine sampleLine;

    @BeforeEach
    void setUp() {
        sampleProduct = Product.builder()
                .id(1L)
                .sku("SKU-001")
                .name("Test Product")
                .quantityOnHand(100)
                .unitPrice(new BigDecimal("10.00"))
                .build();

        sampleLine = PurchaseOrderLine.builder()
                .id(1L)
                .purchaseOrderId(1L)
                .productId(1L)
                .quantity(50)
                .unitPrice(new BigDecimal("8.00"))
                .build();

        sampleOrder = PurchaseOrder.builder()
                .id(1L)
                .supplierName("Test Supplier")
                .status(PurchaseOrderStatus.PENDING)
                .lines(new ArrayList<>(List.of(sampleLine)))
                .createdAt(Instant.now())
                .build();
    }

    @Nested
    @DisplayName("createPurchaseOrder")
    class CreatePurchaseOrderTests {

        @Test
        @DisplayName("should create pending order when received is false")
        void createPurchaseOrder_withReceivedFalse_createsPendingOrder() {
            // given
            CreatePurchaseOrderCommand command = new CreatePurchaseOrderCommand(
                    "Acme Supplies",
                    List.of(new CreatePurchaseOrderCommand.LineItem(1L, 50, new BigDecimal("8.00"))),
                    false
            );

            when(productRepository.existsById(1L)).thenReturn(Mono.just(true));
            when(purchaseOrderRepository.save(any(PurchaseOrder.class)))
                    .thenAnswer(inv -> {
                        PurchaseOrder order = inv.getArgument(0);
                        order.setId(1L);
                        return Mono.just(order);
                    });
            when(purchaseOrderRepository.saveLines(any()))
                    .thenReturn(Flux.just(sampleLine));

            // when/then
            StepVerifier.create(purchaseOrderService.createPurchaseOrder(command))
                    .assertNext(order -> {
                        assertThat(order.getId()).isEqualTo(1L);
                        assertThat(order.getSupplierName()).isEqualTo("Acme Supplies");
                        assertThat(order.getStatus()).isEqualTo(PurchaseOrderStatus.PENDING);
                        assertThat(order.getLines()).hasSize(1);
                    })
                    .verifyComplete();

            verify(productRepository).existsById(1L);
            verify(purchaseOrderRepository).save(any(PurchaseOrder.class));
            verify(stockMovementRepository, never()).save(any(StockMovement.class));
        }

        @Test
        @DisplayName("should create and receive order when received is true")
        void createPurchaseOrder_withReceivedTrue_createsAndReceivesOrder() {
            // given
            CreatePurchaseOrderCommand command = new CreatePurchaseOrderCommand(
                    "Acme Supplies",
                    List.of(new CreatePurchaseOrderCommand.LineItem(1L, 50, new BigDecimal("8.00"))),
                    true
            );

            when(productRepository.existsById(1L)).thenReturn(Mono.just(true));
            when(purchaseOrderRepository.save(any(PurchaseOrder.class)))
                    .thenAnswer(inv -> {
                        PurchaseOrder order = inv.getArgument(0);
                        if (order.getId() == null) {
                            order.setId(1L);
                        }
                        return Mono.just(order);
                    });
            when(purchaseOrderRepository.saveLines(any()))
                    .thenReturn(Flux.just(sampleLine));
            when(productRepository.findById(1L)).thenReturn(Mono.just(sampleProduct));
            when(productRepository.save(any(Product.class))).thenReturn(Mono.just(sampleProduct));
            when(stockMovementRepository.save(any(StockMovement.class)))
                    .thenAnswer(inv -> {
                        StockMovement m = inv.getArgument(0);
                        m.setId(1L);
                        return Mono.just(m);
                    });

            // when/then
            StepVerifier.create(purchaseOrderService.createPurchaseOrder(command))
                    .assertNext(order -> {
                        assertThat(order.getStatus()).isEqualTo(PurchaseOrderStatus.RECEIVED);
                        assertThat(order.getReceivedAt()).isNotNull();
                    })
                    .verifyComplete();

            verify(stockMovementRepository).save(any(StockMovement.class));
        }

        @Test
        @DisplayName("should throw when product does not exist")
        void createPurchaseOrder_withNonExistentProduct_throwsException() {
            // given
            CreatePurchaseOrderCommand command = new CreatePurchaseOrderCommand(
                    "Acme Supplies",
                    List.of(new CreatePurchaseOrderCommand.LineItem(999L, 50, new BigDecimal("8.00"))),
                    false
            );

            when(productRepository.existsById(999L)).thenReturn(Mono.just(false));

            // when/then
            StepVerifier.create(purchaseOrderService.createPurchaseOrder(command))
                    .expectError(ProductNotFoundException.class)
                    .verify();

            verify(purchaseOrderRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("getPurchaseOrderById")
    class GetPurchaseOrderByIdTests {

        @Test
        @DisplayName("should return order with lines when found")
        void getPurchaseOrderById_withExistingId_returnsOrderWithLines() {
            // given
            when(purchaseOrderRepository.findByIdWithLines(1L)).thenReturn(Mono.just(sampleOrder));
            when(productRepository.findById(1L)).thenReturn(Mono.just(sampleProduct));

            // when/then
            StepVerifier.create(purchaseOrderService.getPurchaseOrderById(1L))
                    .assertNext(order -> {
                        assertThat(order.getId()).isEqualTo(1L);
                        assertThat(order.getSupplierName()).isEqualTo("Test Supplier");
                        assertThat(order.getLines()).hasSize(1);
                    })
                    .verifyComplete();
        }

        @Test
        @DisplayName("should throw when not found")
        void getPurchaseOrderById_withNonExistingId_throwsException() {
            // given
            when(purchaseOrderRepository.findByIdWithLines(999L)).thenReturn(Mono.empty());

            // when/then
            StepVerifier.create(purchaseOrderService.getPurchaseOrderById(999L))
                    .expectError(PurchaseOrderNotFoundException.class)
                    .verify();
        }
    }

    @Nested
    @DisplayName("receivePurchaseOrder")
    class ReceivePurchaseOrderTests {

        @Test
        @DisplayName("should mark order as received and update stock")
        void receivePurchaseOrder_withPendingOrder_marksAsReceivedAndUpdatesStock() {
            // given
            when(purchaseOrderRepository.findByIdWithLines(1L)).thenReturn(Mono.just(sampleOrder));
            when(productRepository.findById(1L)).thenReturn(Mono.just(sampleProduct));
            when(productRepository.save(any(Product.class)))
                    .thenAnswer(inv -> Mono.just(inv.getArgument(0)));
            when(stockMovementRepository.save(any(StockMovement.class)))
                    .thenAnswer(inv -> {
                        StockMovement m = inv.getArgument(0);
                        m.setId(1L);
                        return Mono.just(m);
                    });
            when(purchaseOrderRepository.save(any(PurchaseOrder.class)))
                    .thenAnswer(inv -> Mono.just(inv.getArgument(0)));

            // when/then
            StepVerifier.create(purchaseOrderService.receivePurchaseOrder(1L))
                    .assertNext(order -> {
                        assertThat(order.getStatus()).isEqualTo(PurchaseOrderStatus.RECEIVED);
                        assertThat(order.getReceivedAt()).isNotNull();
                    })
                    .verifyComplete();

            verify(productRepository).save(any(Product.class));
            verify(stockMovementRepository).save(movementCaptor.capture());

            StockMovement savedMovement = movementCaptor.getValue();
            assertThat(savedMovement.getChange()).isEqualTo(50);
            assertThat(savedMovement.getReason()).isEqualTo(de.nofelix.inventorybackend.domain.model.StockMovementReason.PO_RECEIPT);
        }

        @Test
        @DisplayName("should throw when order not found")
        void receivePurchaseOrder_withNonExistingOrder_throwsException() {
            // given
            when(purchaseOrderRepository.findByIdWithLines(999L)).thenReturn(Mono.empty());

            // when/then
            StepVerifier.create(purchaseOrderService.receivePurchaseOrder(999L))
                    .expectError(PurchaseOrderNotFoundException.class)
                    .verify();
        }

        @Test
        @DisplayName("should throw when order is already received")
        void receivePurchaseOrder_withAlreadyReceivedOrder_throwsException() {
            // given
            PurchaseOrder receivedOrder = PurchaseOrder.builder()
                    .id(1L)
                    .supplierName("Test Supplier")
                    .status(PurchaseOrderStatus.RECEIVED)
                    .lines(new ArrayList<>(List.of(sampleLine)))
                    .build();

            when(purchaseOrderRepository.findByIdWithLines(1L)).thenReturn(Mono.just(receivedOrder));

            // when/then
            StepVerifier.create(purchaseOrderService.receivePurchaseOrder(1L))
                    .expectError(PurchaseOrderNotReceivableException.class)
                    .verify();

            verify(productRepository, never()).save(any());
            verify(stockMovementRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("getAllPurchaseOrders")
    class GetAllPurchaseOrdersTests {

        @Test
        @DisplayName("should return all orders with lines")
        void getAllPurchaseOrders_returnsAllOrders() {
            // given
            PurchaseOrder order2 = PurchaseOrder.builder()
                    .id(2L)
                    .supplierName("Another Supplier")
                    .status(PurchaseOrderStatus.RECEIVED)
                    .lines(new ArrayList<>())
                    .build();

            when(purchaseOrderRepository.findAll()).thenReturn(Flux.just(sampleOrder, order2));
            when(purchaseOrderRepository.findLinesByPurchaseOrderId(1L))
                    .thenReturn(Flux.just(sampleLine));
            when(purchaseOrderRepository.findLinesByPurchaseOrderId(2L))
                    .thenReturn(Flux.empty());

            // when/then
            StepVerifier.create(purchaseOrderService.getAllPurchaseOrders())
                    .assertNext(order -> assertThat(order.getId()).isEqualTo(1L))
                    .assertNext(order -> assertThat(order.getId()).isEqualTo(2L))
                    .verifyComplete();
        }
    }

    @Nested
    @DisplayName("getPurchaseOrdersByStatus")
    class GetPurchaseOrdersByStatusTests {

        @Test
        @DisplayName("should return orders filtered by status")
        void getPurchaseOrdersByStatus_withValidStatus_returnsFilteredOrders() {
            // given
            when(purchaseOrderRepository.findByStatus(PurchaseOrderStatus.PENDING))
                    .thenReturn(Flux.just(sampleOrder));

            // when/then
            StepVerifier.create(purchaseOrderService.getPurchaseOrdersByStatus(PurchaseOrderStatus.PENDING))
                    .assertNext(order -> {
                        assertThat(order.getId()).isEqualTo(1L);
                        assertThat(order.getStatus()).isEqualTo(PurchaseOrderStatus.PENDING);
                    })
                    .verifyComplete();

            verify(purchaseOrderRepository).findByStatus(PurchaseOrderStatus.PENDING);
        }
    }
}
