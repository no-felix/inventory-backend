-- V2__create_purchase_orders_table.sql
-- Create purchase orders and purchase order lines tables

-- ============================================================
-- PURCHASE ORDERS TABLE
-- ============================================================
CREATE TABLE purchase_orders (
    id BIGSERIAL PRIMARY KEY,
    supplier_name VARCHAR(255) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    received_at TIMESTAMP WITH TIME ZONE,
    version BIGINT NOT NULL DEFAULT 0,
    
    CONSTRAINT chk_purchase_order_status CHECK (status IN ('PENDING', 'RECEIVED', 'CANCELLED'))
);

-- Create index on status for filtering
CREATE INDEX idx_purchase_orders_status ON purchase_orders(status);

-- Create index on created_at for time-series queries
CREATE INDEX idx_purchase_orders_created_at ON purchase_orders(created_at);

-- Add comments
COMMENT ON TABLE purchase_orders IS 'Stores purchase orders from suppliers';
COMMENT ON COLUMN purchase_orders.status IS 'Order status: PENDING, RECEIVED, or CANCELLED';
COMMENT ON COLUMN purchase_orders.received_at IS 'Timestamp when order was marked as received';
COMMENT ON COLUMN purchase_orders.version IS 'Optimistic locking version';

-- ============================================================
-- PURCHASE ORDER LINES TABLE
-- ============================================================
CREATE TABLE purchase_order_lines (
    id BIGSERIAL PRIMARY KEY,
    purchase_order_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    quantity INTEGER NOT NULL,
    unit_price DECIMAL(19, 4) NOT NULL,
    
    CONSTRAINT fk_pol_purchase_order 
        FOREIGN KEY (purchase_order_id) 
        REFERENCES purchase_orders(id) 
        ON DELETE CASCADE,
    CONSTRAINT fk_pol_product 
        FOREIGN KEY (product_id) 
        REFERENCES products(id) 
        ON DELETE RESTRICT,
    CONSTRAINT chk_pol_quantity_positive CHECK (quantity > 0),
    CONSTRAINT chk_pol_unit_price_non_negative CHECK (unit_price >= 0)
);

-- Create index on purchase_order_id for joins
CREATE INDEX idx_pol_purchase_order_id ON purchase_order_lines(purchase_order_id);

-- Create index on product_id for product-based queries
CREATE INDEX idx_pol_product_id ON purchase_order_lines(product_id);

-- Add comments
COMMENT ON TABLE purchase_order_lines IS 'Line items for purchase orders';
COMMENT ON COLUMN purchase_order_lines.quantity IS 'Quantity ordered (must be positive)';
COMMENT ON COLUMN purchase_order_lines.unit_price IS 'Unit price at time of order';
