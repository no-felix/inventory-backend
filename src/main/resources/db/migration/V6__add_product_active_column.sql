-- V6__add_product_active_column.sql
-- Add soft delete capability to products

-- Add active column with default true (all existing products are active)
ALTER TABLE products ADD COLUMN active BOOLEAN NOT NULL DEFAULT TRUE;

-- Create index for filtering active products efficiently
CREATE INDEX idx_products_active ON products(active);

-- Add comment
COMMENT ON COLUMN products.active IS 'Soft delete flag: true = active, false = deleted';
