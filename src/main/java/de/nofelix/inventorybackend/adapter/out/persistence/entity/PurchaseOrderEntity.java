package de.nofelix.inventorybackend.adapter.out.persistence.entity;

import de.nofelix.inventorybackend.domain.model.PurchaseOrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;

/**
 * R2DBC entity representing a purchase order in the database.
 */
@Table("purchase_orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
public class PurchaseOrderEntity {

    @Id
    private Long id;

    @Column("supplier_name")
    private String supplierName;

    @Column("status")
    private PurchaseOrderStatus status;

    @CreatedDate
    @Column("created_at")
    private Instant createdAt;

    @Column("received_at")
    private Instant receivedAt;

    @Version
    @Column("version")
    private Long version;
}
