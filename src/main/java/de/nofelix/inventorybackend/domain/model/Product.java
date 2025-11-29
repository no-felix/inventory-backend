package de.nofelix.inventorybackend.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Domain model representing a Product in the inventory system.
 * 
 * <p>This is a domain object with business rules and invariants for a product.
 * Lombok annotations are used for boilerplate reduction while keeping 
 * business methods explicit.</p>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "description")
public class Product {

    private Long id;
    private String sku;
    private String name;
    private String description;
    private Integer quantityOnHand;
    private BigDecimal unitPrice;
    private Instant createdAt;
    private Instant updatedAt;
    private Long version;

    // ========================================
    // Business Methods
    // ========================================

    /**
     * Increases the quantity on hand by the specified amount.
     *
     * @param quantity the quantity to add (must be positive)
     * @throws IllegalArgumentException if quantity is not positive
     */
    public void increaseStock(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        this.quantityOnHand += quantity;
    }

    /**
     * Decreases the quantity on hand by the specified amount.
     *
     * @param quantity the quantity to remove (must be positive)
     * @throws IllegalArgumentException if quantity is not positive
     * @throws IllegalStateException if insufficient stock
     */
    public void decreaseStock(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        if (this.quantityOnHand < quantity) {
            throw new IllegalStateException(
                    "Insufficient stock. Available: " + this.quantityOnHand + ", Requested: " + quantity);
        }
        this.quantityOnHand -= quantity;
    }

    /**
     * Calculates the total value of this product's stock.
     *
     * @return quantity * unitPrice
     */
    public BigDecimal calculateTotalValue() {
        if (quantityOnHand == null || unitPrice == null) {
            return BigDecimal.ZERO;
        }
        return unitPrice.multiply(BigDecimal.valueOf(quantityOnHand));
    }

    /**
     * Checks if the product is low on stock.
     *
     * @param threshold the low stock threshold
     * @return true if quantity on hand is less than or equal to threshold
     */
    public boolean isLowStock(int threshold) {
        return quantityOnHand != null && quantityOnHand <= threshold;
    }
}
