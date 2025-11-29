package de.nofelix.inventorybackend.adapter.out.persistence.entity;

import de.nofelix.inventorybackend.domain.model.StockMovementReason;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;

/**
 * R2DBC entity representing a stock movement in the database.
 */
@Table("stock_movements")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
public class StockMovementEntity {

    @Id
    private Long id;

    @Column("product_id")
    private Long productId;

    @Column("change")
    private Integer change;

    @Column("reason")
    private StockMovementReason reason;

    @Column("related_entity_type")
    private String relatedEntityType;

    @Column("related_entity_id")
    private Long relatedEntityId;

    @Column("performed_by")
    private String performedBy;

    @CreatedDate
    @Column("created_at")
    private Instant createdAt;
}
