package de.nofelix.inventorybackend.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Domain model representing a Purchase Order.
 * 
 * <p>A purchase order represents an order to a supplier for products
 * to be added to inventory. When received, stock levels are updated
 * and stock movements are recorded.</p>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "lines")
public class PurchaseOrder {

    private Long id;
    private String supplierName;
    private PurchaseOrderStatus status;
    
    @Builder.Default
    private List<PurchaseOrderLine> lines = new ArrayList<>();
    
    private Instant createdAt;
    private Instant receivedAt;
    private Long version;

    // ========================================
    // Business Methods
    // ========================================

    /**
     * Calculates the total amount of the purchase order.
     *
     * @return sum of all line totals
     */
    public BigDecimal calculateTotalAmount() {
        if (lines == null || lines.isEmpty()) {
            return BigDecimal.ZERO;
        }
        return lines.stream()
                .map(PurchaseOrderLine::calculateLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Adds a line item to the purchase order.
     *
     * @param line the line item to add
     * @throws IllegalStateException if the order is not in PENDING status
     */
    public void addLine(PurchaseOrderLine line) {
        if (status != null && status != PurchaseOrderStatus.PENDING) {
            throw new IllegalStateException("Cannot modify a non-pending purchase order");
        }
        if (lines == null) {
            lines = new ArrayList<>();
        }
        line.setPurchaseOrderId(this.id);
        lines.add(line);
    }

    /**
     * Marks the purchase order as received.
     *
     * @throws IllegalStateException if the order is not in PENDING status
     */
    public void markAsReceived() {
        if (status != PurchaseOrderStatus.PENDING) {
            throw new IllegalStateException(
                    "Can only receive orders in PENDING status. Current status: " + status);
        }
        this.status = PurchaseOrderStatus.RECEIVED;
        this.receivedAt = Instant.now();
    }

    /**
     * Cancels the purchase order.
     *
     * @throws IllegalStateException if the order has already been received
     */
    public void cancel() {
        if (status == PurchaseOrderStatus.RECEIVED) {
            throw new IllegalStateException("Cannot cancel an order that has already been received");
        }
        this.status = PurchaseOrderStatus.CANCELLED;
    }

    /**
     * Checks if the order can be received.
     *
     * @return true if the order is in PENDING status
     */
    public boolean canBeReceived() {
        return status == PurchaseOrderStatus.PENDING;
    }

    /**
     * Validates that the purchase order has valid data.
     *
     * @throws IllegalStateException if validation fails
     */
    public void validate() {
        if (supplierName == null || supplierName.isBlank()) {
            throw new IllegalStateException("Supplier name is required");
        }
        if (lines == null || lines.isEmpty()) {
            throw new IllegalStateException("Purchase order must have at least one line item");
        }
        lines.forEach(PurchaseOrderLine::validate);
    }
}
