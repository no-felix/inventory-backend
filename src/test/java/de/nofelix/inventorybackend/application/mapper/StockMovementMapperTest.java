package de.nofelix.inventorybackend.application.mapper;

import de.nofelix.inventorybackend.TestcontainersConfiguration;
import de.nofelix.inventorybackend.adapter.in.web.model.StockMovementReason;
import de.nofelix.inventorybackend.domain.model.StockMovement;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for StockMovementMapper.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
@DisplayName("StockMovementMapper")
class StockMovementMapperTest {

    @Autowired
    private StockMovementMapper mapper;

    @Nested
    @DisplayName("toResponse")
    class ToResponseTests {

        @Test
        @DisplayName("should map domain to response for purchase order received")
        void toResponse_purchaseOrderReceived_mapsCorrectly() {
            // given
            var movement = StockMovement.builder()
                    .id(1L)
                    .productId(100L)
                    .change(50)
                    .reason(de.nofelix.inventorybackend.domain.model.StockMovementReason.PO_RECEIPT)
                    .relatedEntityType("PurchaseOrder")
                    .relatedEntityId(10L)
                    .performedBy("system")
                    .createdAt(Instant.parse("2025-01-15T10:30:00Z"))
                    .build();

            // when
            var response = mapper.toResponse(movement);

            // then
            assertThat(response.getId()).isEqualTo(1L);
            assertThat(response.getProductId()).isEqualTo(100L);
            assertThat(response.getChange()).isEqualTo(50);
            assertThat(response.getReason()).isEqualTo(StockMovementReason.PO_RECEIPT);
            assertThat(response.getRelatedEntityType()).isEqualTo("PurchaseOrder");
            assertThat(response.getRelatedEntityId()).isEqualTo(10L);
            assertThat(response.getPerformedBy()).isEqualTo("system");
            assertThat(response.getCreatedAt()).isNotNull();
        }

        @Test
        @DisplayName("should map domain to response for manual adjustment")
        void toResponse_manualAdjustment_mapsCorrectly() {
            // given
            var movement = StockMovement.builder()
                    .id(2L)
                    .productId(200L)
                    .change(-10)
                    .reason(de.nofelix.inventorybackend.domain.model.StockMovementReason.ADJUSTMENT)
                    .performedBy("admin")
                    .createdAt(Instant.parse("2025-01-16T14:00:00Z"))
                    .build();

            // when
            var response = mapper.toResponse(movement);

            // then
            assertThat(response.getId()).isEqualTo(2L);
            assertThat(response.getProductId()).isEqualTo(200L);
            assertThat(response.getChange()).isEqualTo(-10);
            assertThat(response.getReason()).isEqualTo(StockMovementReason.ADJUSTMENT);
            assertThat(response.getRelatedEntityType()).isNull();
            assertThat(response.getRelatedEntityId()).isNull();
            assertThat(response.getPerformedBy()).isEqualTo("admin");
        }
    }

    @Nested
    @DisplayName("reason mapping")
    class ReasonMappingTests {

        @Test
        @DisplayName("should map all domain reasons to API reasons")
        void toApiReason_allReasons_mapCorrectly() {
            // when/then
            assertThat(mapper.toApiReason(de.nofelix.inventorybackend.domain.model.StockMovementReason.PO_RECEIPT))
                    .isEqualTo(StockMovementReason.PO_RECEIPT);
            assertThat(mapper.toApiReason(de.nofelix.inventorybackend.domain.model.StockMovementReason.ADJUSTMENT))
                    .isEqualTo(StockMovementReason.ADJUSTMENT);
            assertThat(mapper.toApiReason(de.nofelix.inventorybackend.domain.model.StockMovementReason.SALE))
                    .isEqualTo(StockMovementReason.SALE);
            assertThat(mapper.toApiReason(de.nofelix.inventorybackend.domain.model.StockMovementReason.RETURN))
                    .isEqualTo(StockMovementReason.RETURN);
            assertThat(mapper.toApiReason(de.nofelix.inventorybackend.domain.model.StockMovementReason.DAMAGE))
                    .isEqualTo(StockMovementReason.DAMAGE);
            assertThat(mapper.toApiReason(de.nofelix.inventorybackend.domain.model.StockMovementReason.TRANSFER))
                    .isEqualTo(StockMovementReason.TRANSFER);
        }

        @Test
        @DisplayName("should handle null domain reason")
        void toApiReason_null_returnsNull() {
            // when/then
            assertThat(mapper.toApiReason(null)).isNull();
        }

        @Test
        @DisplayName("should map all API reasons to domain reasons")
        void toDomainReason_allReasons_mapCorrectly() {
            // when/then
            assertThat(mapper.toDomainReason(StockMovementReason.PO_RECEIPT))
                    .isEqualTo(de.nofelix.inventorybackend.domain.model.StockMovementReason.PO_RECEIPT);
            assertThat(mapper.toDomainReason(StockMovementReason.ADJUSTMENT))
                    .isEqualTo(de.nofelix.inventorybackend.domain.model.StockMovementReason.ADJUSTMENT);
            assertThat(mapper.toDomainReason(StockMovementReason.SALE))
                    .isEqualTo(de.nofelix.inventorybackend.domain.model.StockMovementReason.SALE);
            assertThat(mapper.toDomainReason(StockMovementReason.RETURN))
                    .isEqualTo(de.nofelix.inventorybackend.domain.model.StockMovementReason.RETURN);
            assertThat(mapper.toDomainReason(StockMovementReason.DAMAGE))
                    .isEqualTo(de.nofelix.inventorybackend.domain.model.StockMovementReason.DAMAGE);
            assertThat(mapper.toDomainReason(StockMovementReason.TRANSFER))
                    .isEqualTo(de.nofelix.inventorybackend.domain.model.StockMovementReason.TRANSFER);
        }

        @Test
        @DisplayName("should handle null API reason")
        void toDomainReason_null_returnsNull() {
            // when/then
            assertThat(mapper.toDomainReason(null)).isNull();
        }
    }

    @Nested
    @DisplayName("helper methods")
    class HelperMethodsTests {

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
