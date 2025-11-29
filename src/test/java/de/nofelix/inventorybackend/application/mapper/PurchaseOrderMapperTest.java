package de.nofelix.inventorybackend.application.mapper;

import de.nofelix.inventorybackend.TestcontainersConfiguration;
import de.nofelix.inventorybackend.adapter.in.web.model.PurchaseOrderLineRequest;
import de.nofelix.inventorybackend.adapter.in.web.model.PurchaseOrderRequest;
import de.nofelix.inventorybackend.adapter.in.web.model.PurchaseOrderStatus;
import de.nofelix.inventorybackend.domain.model.PurchaseOrder;
import de.nofelix.inventorybackend.domain.model.PurchaseOrderLine;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for PurchaseOrderMapper.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
@DisplayName("PurchaseOrderMapper")
class PurchaseOrderMapperTest {

    @Autowired
    private PurchaseOrderMapper mapper;

    @Nested
    @DisplayName("toCreateCommand")
    class ToCreateCommandTests {

        @Test
        @DisplayName("should map request to command with lines")
        void toCreateCommand_withLines_mapsCorrectly() {
            // given
            var lineRequest = new PurchaseOrderLineRequest()
                    .productId(1L)
                    .quantity(10)
                    .unitPrice(25.99);

            var request = new PurchaseOrderRequest()
                    .supplierName("Test Supplier")
                    .lines(List.of(lineRequest))
                    .received(false);

            // when
            var command = mapper.toCreateCommand(request);

            // then
            assertThat(command.supplierName()).isEqualTo("Test Supplier");
            assertThat(command.received()).isFalse();
            assertThat(command.lines()).hasSize(1);
            assertThat(command.lines().get(0).productId()).isEqualTo(1L);
            assertThat(command.lines().get(0).quantity()).isEqualTo(10);
            assertThat(command.lines().get(0).unitPrice()).isEqualByComparingTo(BigDecimal.valueOf(25.99));
        }

        @Test
        @DisplayName("should handle null lines")
        void toCreateCommand_nullLines_returnsEmptyList() {
            // given
            var request = new PurchaseOrderRequest()
                    .supplierName("Test Supplier")
                    .lines(null);

            // when
            var command = mapper.toCreateCommand(request);

            // then
            assertThat(command.lines()).isEmpty();
        }

        @Test
        @DisplayName("should default received to false")
        void toCreateCommand_nullReceived_defaultsToFalse() {
            // given
            var request = new PurchaseOrderRequest()
                    .supplierName("Test Supplier")
                    .lines(List.of());

            // when
            var command = mapper.toCreateCommand(request);

            // then
            assertThat(command.received()).isFalse();
        }
    }

    @Nested
    @DisplayName("toResponse")
    class ToResponseTests {

        @Test
        @DisplayName("should map domain to response")
        void toResponse_validOrder_mapsCorrectly() {
            // given
            var line = PurchaseOrderLine.builder()
                    .id(1L)
                    .purchaseOrderId(10L)
                    .productId(100L)
                    .quantity(5)
                    .unitPrice(BigDecimal.valueOf(20.00))
                    .build();

            var order = PurchaseOrder.builder()
                    .id(10L)
                    .supplierName("Test Supplier")
                    .status(de.nofelix.inventorybackend.domain.model.PurchaseOrderStatus.PENDING)
                    .createdAt(Instant.parse("2025-01-15T10:30:00Z"))
                    .lines(List.of(line))
                    .build();

            // when
            var response = mapper.toResponse(order);

            // then
            assertThat(response.getId()).isEqualTo(10L);
            assertThat(response.getSupplierName()).isEqualTo("Test Supplier");
            assertThat(response.getStatus()).isEqualTo(PurchaseOrderStatus.PENDING);
            assertThat(response.getCreatedAt()).isNotNull();
            assertThat(response.getLines()).hasSize(1);
            assertThat(response.getTotalAmount()).isEqualTo(100.00); // 5 * 20.00
        }

        @Test
        @DisplayName("should handle received order with receivedAt")
        void toResponse_receivedOrder_mapsReceivedAt() {
            // given
            var order = PurchaseOrder.builder()
                    .id(10L)
                    .supplierName("Test Supplier")
                    .status(de.nofelix.inventorybackend.domain.model.PurchaseOrderStatus.RECEIVED)
                    .createdAt(Instant.parse("2025-01-15T10:30:00Z"))
                    .receivedAt(Instant.parse("2025-01-16T14:00:00Z"))
                    .lines(new ArrayList<>())
                    .build();

            // when
            var response = mapper.toResponse(order);

            // then
            assertThat(response.getStatus()).isEqualTo(PurchaseOrderStatus.RECEIVED);
            assertThat(response.getReceivedAt()).isNotNull();
        }
    }

    @Nested
    @DisplayName("toLineResponse")
    class ToLineResponseTests {

        @Test
        @DisplayName("should map line domain to response")
        void toLineResponse_validLine_mapsCorrectly() {
            // given
            var line = PurchaseOrderLine.builder()
                    .id(1L)
                    .purchaseOrderId(10L)
                    .productId(100L)
                    .quantity(5)
                    .unitPrice(BigDecimal.valueOf(20.00))
                    .build();

            // when
            var response = mapper.toLineResponse(line);

            // then
            assertThat(response.getId()).isEqualTo(1L);
            assertThat(response.getProductId()).isEqualTo(100L);
            assertThat(response.getQuantity()).isEqualTo(5);
            assertThat(response.getUnitPrice()).isEqualTo(20.00);
            assertThat(response.getLineTotal()).isEqualTo(100.00);
        }
    }

    @Nested
    @DisplayName("status mapping")
    class StatusMappingTests {

        @Test
        @DisplayName("should map domain status to API status")
        void toApiStatus_allStatuses_mapCorrectly() {
            // when/then
            assertThat(mapper.toApiStatus(de.nofelix.inventorybackend.domain.model.PurchaseOrderStatus.PENDING))
                    .isEqualTo(PurchaseOrderStatus.PENDING);
            assertThat(mapper.toApiStatus(de.nofelix.inventorybackend.domain.model.PurchaseOrderStatus.RECEIVED))
                    .isEqualTo(PurchaseOrderStatus.RECEIVED);
            assertThat(mapper.toApiStatus(de.nofelix.inventorybackend.domain.model.PurchaseOrderStatus.CANCELLED))
                    .isEqualTo(PurchaseOrderStatus.CANCELLED);
        }

        @Test
        @DisplayName("should handle null status")
        void toApiStatus_null_returnsNull() {
            // when/then
            assertThat(mapper.toApiStatus(null)).isNull();
        }

        @Test
        @DisplayName("should map API status to domain status")
        void toDomainStatus_allStatuses_mapCorrectly() {
            // when/then
            assertThat(mapper.toDomainStatus(PurchaseOrderStatus.PENDING))
                    .isEqualTo(de.nofelix.inventorybackend.domain.model.PurchaseOrderStatus.PENDING);
            assertThat(mapper.toDomainStatus(PurchaseOrderStatus.RECEIVED))
                    .isEqualTo(de.nofelix.inventorybackend.domain.model.PurchaseOrderStatus.RECEIVED);
            assertThat(mapper.toDomainStatus(PurchaseOrderStatus.CANCELLED))
                    .isEqualTo(de.nofelix.inventorybackend.domain.model.PurchaseOrderStatus.CANCELLED);
        }

        @Test
        @DisplayName("should handle null API status")
        void toDomainStatus_null_returnsNull() {
            // when/then
            assertThat(mapper.toDomainStatus(null)).isNull();
        }
    }

    @Nested
    @DisplayName("helper methods")
    class HelperMethodsTests {

        @Test
        @DisplayName("should convert Double to BigDecimal")
        void toBigDecimal_validDouble_convertCorrectly() {
            // when/then
            assertThat(mapper.toBigDecimal(25.99)).isEqualByComparingTo(BigDecimal.valueOf(25.99));
            assertThat(mapper.toBigDecimal(null)).isNull();
        }

        @Test
        @DisplayName("should convert BigDecimal to Double")
        void toDouble_validBigDecimal_convertCorrectly() {
            // when/then
            assertThat(mapper.toDouble(BigDecimal.valueOf(25.99))).isEqualTo(25.99);
            assertThat(mapper.toDouble(null)).isNull();
        }

        @Test
        @DisplayName("should convert Instant to OffsetDateTime")
        void toOffsetDateTime_validInstant_convertCorrectly() {
            // given
            var instant = Instant.parse("2025-01-15T10:30:00Z");

            // when
            var result = mapper.toOffsetDateTime(instant);

            // then
            assertThat(result).isNotNull();
            assertThat(result.toInstant()).isEqualTo(instant);
        }

        @Test
        @DisplayName("should handle null Instant")
        void toOffsetDateTime_null_returnsNull() {
            // when/then
            assertThat(mapper.toOffsetDateTime(null)).isNull();
        }
    }
}
