-- V1__create_products_table.sql
-- Create the products table for inventory management

CREATE TABLE products (
    id BIGSERIAL PRIMARY KEY,
    sku VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    description VARCHAR(1000),
    quantity_on_hand INTEGER NOT NULL DEFAULT 0,
    unit_price DECIMAL(19, 4) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0
);

-- Create index on SKU for faster lookups
CREATE INDEX idx_products_sku ON products(sku);

-- Create index on name for search functionality
CREATE INDEX idx_products_name ON products(name);

-- Add comment to table
COMMENT ON TABLE products IS 'Stores product information for the inventory system';
COMMENT ON COLUMN products.sku IS 'Unique stock keeping unit identifier';
COMMENT ON COLUMN products.quantity_on_hand IS 'Current quantity in stock';
COMMENT ON COLUMN products.unit_price IS 'Price per unit with 4 decimal precision';
COMMENT ON COLUMN products.version IS 'Optimistic locking version';
