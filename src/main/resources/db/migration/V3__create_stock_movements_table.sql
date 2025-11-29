-- V3__create_stock_movements_table.sql
-- Create stock movements table for inventory audit trail

CREATE TABLE stock_movements (
    id BIGSERIAL PRIMARY KEY,
    product_id BIGINT NOT NULL,
    change INTEGER NOT NULL,
    reason VARCHAR(20) NOT NULL,
    related_entity_type VARCHAR(50),
    related_entity_id BIGINT,
    performed_by VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_sm_product 
        FOREIGN KEY (product_id) 
        REFERENCES products(id) 
        ON DELETE RESTRICT,
    CONSTRAINT chk_sm_reason CHECK (reason IN (
        'PO_RECEIPT', 'ADJUSTMENT', 'SALE', 'RETURN', 'DAMAGE', 'TRANSFER'
    ))
);

-- Create index on product_id for product history queries
CREATE INDEX idx_sm_product_id ON stock_movements(product_id);

-- Create index on reason for filtering by movement type
CREATE INDEX idx_sm_reason ON stock_movements(reason);

-- Create index on created_at for time-series queries
CREATE INDEX idx_sm_created_at ON stock_movements(created_at);

-- Create composite index for related entity lookups
CREATE INDEX idx_sm_related_entity ON stock_movements(related_entity_type, related_entity_id);

-- Add comments
COMMENT ON TABLE stock_movements IS 'Audit trail of all inventory changes';
COMMENT ON COLUMN stock_movements.change IS 'Quantity change (positive=increase, negative=decrease)';
COMMENT ON COLUMN stock_movements.reason IS 'Reason for movement: PO_RECEIPT, ADJUSTMENT, SALE, RETURN, DAMAGE, TRANSFER';
COMMENT ON COLUMN stock_movements.related_entity_type IS 'Type of related entity (e.g., PURCHASE_ORDER, SALE_ORDER)';
COMMENT ON COLUMN stock_movements.related_entity_id IS 'ID of the related entity';
COMMENT ON COLUMN stock_movements.performed_by IS 'User who performed the stock movement';
