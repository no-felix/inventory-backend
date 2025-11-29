package de.nofelix.inventorybackend.domain.model;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Domain model representing a Product in the inventory system.
 * 
 * <p>This is a pure domain object with no framework dependencies.
 * It encapsulates the business rules and invariants for a product.</p>
 */
public class Product {

    private Long id;
    private String sku;
    private String name;
    private String description;
    private Integer quantityOnHand;
    private BigDecimal unitPrice;
    private Instant createdAt;
    private Instant updatedAt;

    public Product() {
    }

    public Product(Long id, String sku, String name, String description,
                   Integer quantityOnHand, BigDecimal unitPrice,
                   Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.sku = sku;
        this.name = name;
        this.description = description;
        this.quantityOnHand = quantityOnHand;
        this.unitPrice = unitPrice;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

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

    // ========================================
    // Getters and Setters
    // ========================================

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getQuantityOnHand() {
        return quantityOnHand;
    }

    public void setQuantityOnHand(Integer quantityOnHand) {
        this.quantityOnHand = quantityOnHand;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    // ========================================
    // Builder
    // ========================================

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private String sku;
        private String name;
        private String description;
        private Integer quantityOnHand;
        private BigDecimal unitPrice;
        private Instant createdAt;
        private Instant updatedAt;

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        public Builder sku(String sku) {
            this.sku = sku;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder quantityOnHand(Integer quantityOnHand) {
            this.quantityOnHand = quantityOnHand;
            return this;
        }

        public Builder unitPrice(BigDecimal unitPrice) {
            this.unitPrice = unitPrice;
            return this;
        }

        public Builder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Builder updatedAt(Instant updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        public Product build() {
            return new Product(id, sku, name, description, quantityOnHand, unitPrice, createdAt, updatedAt);
        }
    }

    @Override
    public String toString() {
        return "Product{" +
                "id=" + id +
                ", sku='" + sku + '\'' +
                ", name='" + name + '\'' +
                ", quantityOnHand=" + quantityOnHand +
                ", unitPrice=" + unitPrice +
                '}';
    }
}
