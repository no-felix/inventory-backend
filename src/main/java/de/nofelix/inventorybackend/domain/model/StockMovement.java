package de.nofelix.inventorybackend.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.Instant;

/**
 * Domain model representing a Stock Movement.
 * 
 * <p>Stock movements are audit records of all inventory changes.
 * They track the reason for change, quantity, and related entities.</p>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class StockMovement {

    private Long id;
    private Long productId;
    private String productSku;
    private String productName;
    
    /**
     * The quantity change (positive for increase, negative for decrease).
     */
    private Integer change;
    
    private StockMovementReason reason;
    
    /**
     * Type of related entity (e.g., "PURCHASE_ORDER", "SALE_ORDER").
     */
    private String relatedEntityType;
    
    /**
     * ID of the related entity (e.g., purchase order ID).
     */
    private Long relatedEntityId;
    
    /**
     * User who performed the action (optional).
     */
    private String performedBy;
    
    private Instant createdAt;

    // ========================================
    // Factory Methods
    // ========================================

    /**
     * Creates a stock movement for a purchase order receipt.
     *
     * @param productId the product ID
     * @param quantity the quantity received (positive)
     * @param purchaseOrderId the purchase order ID
     * @param performedBy the user who performed the action
     * @return a new stock movement
     */
    public static StockMovement forPurchaseOrderReceipt(
            Long productId, 
            int quantity, 
            Long purchaseOrderId, 
            String performedBy) {
        return StockMovement.builder()
                .productId(productId)
                .change(quantity)
                .reason(StockMovementReason.PO_RECEIPT)
                .relatedEntityType("PURCHASE_ORDER")
                .relatedEntityId(purchaseOrderId)
                .performedBy(performedBy)
                .createdAt(Instant.now())
                .build();
    }

    /**
     * Creates a stock movement for a manual adjustment.
     *
     * @param productId the product ID
     * @param change the quantity change (positive or negative)
     * @param performedBy the user who performed the action
     * @return a new stock movement
     */
    public static StockMovement forAdjustment(Long productId, int change, String performedBy) {
        return StockMovement.builder()
                .productId(productId)
                .change(change)
                .reason(StockMovementReason.ADJUSTMENT)
                .performedBy(performedBy)
                .createdAt(Instant.now())
                .build();
    }

    /**
     * Creates a stock movement for a sale.
     *
     * @param productId the product ID
     * @param quantity the quantity sold (will be stored as negative)
     * @param saleOrderId the sale order ID
     * @param performedBy the user who performed the action
     * @return a new stock movement
     */
    public static StockMovement forSale(
            Long productId, 
            int quantity, 
            Long saleOrderId, 
            String performedBy) {
        return StockMovement.builder()
                .productId(productId)
                .change(-quantity) // Negative for sales
                .reason(StockMovementReason.SALE)
                .relatedEntityType("SALE_ORDER")
                .relatedEntityId(saleOrderId)
                .performedBy(performedBy)
                .createdAt(Instant.now())
                .build();
    }

    // ========================================
    // Business Methods
    // ========================================

    /**
     * Checks if this movement represents an increase in stock.
     *
     * @return true if change is positive
     */
    public boolean isIncrease() {
        return change != null && change > 0;
    }

    /**
     * Checks if this movement represents a decrease in stock.
     *
     * @return true if change is negative
     */
    public boolean isDecrease() {
        return change != null && change < 0;
    }

    /**
     * Gets the absolute value of the change.
     *
     * @return absolute value of change
     */
    public int getAbsoluteChange() {
        return change == null ? 0 : Math.abs(change);
    }
}
