package de.nofelix.inventorybackend.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;

/**
 * Domain model representing a line item in a purchase order.
 * 
 * <p>Each line represents a specific product and quantity being ordered.</p>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class PurchaseOrderLine {

    private Long id;
    private Long purchaseOrderId;
    private Long productId;
    private String productSku;
    private String productName;
    private Integer quantity;
    private BigDecimal unitPrice;

    // ========================================
    // Business Methods
    // ========================================

    /**
     * Calculates the total value of this line item.
     *
     * @return quantity * unitPrice
     */
    public BigDecimal calculateLineTotal() {
        if (quantity == null || unitPrice == null) {
            return BigDecimal.ZERO;
        }
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }

    /**
     * Validates that the line has valid data.
     *
     * @throws IllegalStateException if validation fails
     */
    public void validate() {
        if (productId == null) {
            throw new IllegalStateException("Product ID is required");
        }
        if (quantity == null || quantity <= 0) {
            throw new IllegalStateException("Quantity must be positive");
        }
        if (unitPrice == null || unitPrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalStateException("Unit price must be non-negative");
        }
    }
}
