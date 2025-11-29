-- V5__seed_sample_data.sql
-- Sample data for development and demonstration purposes
-- Note: This migration runs in all environments.

-- ============================================================
-- SAMPLE USERS
-- Note: These are placeholder users. In a real environment, 
-- users should register through the /api/v1/auth/register endpoint.
-- The password hashes below are intentionally invalid.
-- ============================================================
-- To create working users, use the registration endpoint:
-- POST /api/v1/auth/register
-- {"username": "admin", "email": "admin@example.com", "password": "yourpassword", "role": "ADMIN"}
INSERT INTO users (username, email, password_hash, role, enabled) VALUES
    ('system', 'system@inventory.local', '$2a$10$INVALID_HASH_REGISTER_VIA_API', 'ADMIN', false)
ON CONFLICT (username) DO NOTHING;

-- ============================================================
-- SAMPLE PRODUCTS (Various categories and price ranges)
-- ============================================================

-- Electronics (High value)
INSERT INTO products (sku, name, description, quantity_on_hand, unit_price) VALUES
    ('ELEC-001', 'Wireless Bluetooth Headphones', 'Premium over-ear headphones with noise cancellation', 45, 149.99),
    ('ELEC-002', 'USB-C Hub 7-in-1', 'Multiport adapter with HDMI, USB-A, SD card reader', 120, 49.99),
    ('ELEC-003', 'Mechanical Keyboard RGB', 'Full-size mechanical keyboard with RGB backlighting', 35, 89.99),
    ('ELEC-004', 'Wireless Mouse', 'Ergonomic wireless mouse with silent clicks', 200, 29.99),
    ('ELEC-005', '4K Webcam', 'Ultra HD webcam with autofocus and microphone', 25, 129.99)
ON CONFLICT (sku) DO NOTHING;

-- Office Supplies (Medium value)
INSERT INTO products (sku, name, description, quantity_on_hand, unit_price) VALUES
    ('OFFC-001', 'Desk Organizer', 'Multi-compartment mesh desk organizer', 150, 24.99),
    ('OFFC-002', 'Ergonomic Chair Cushion', 'Memory foam seat cushion for office chairs', 80, 39.99),
    ('OFFC-003', 'Document Scanner', 'Portable document scanner with OCR', 15, 189.99),
    ('OFFC-004', 'Whiteboard 36x24', 'Magnetic dry-erase whiteboard with markers', 40, 54.99),
    ('OFFC-005', 'Cable Management Kit', 'Complete cable organizer set', 300, 14.99)
ON CONFLICT (sku) DO NOTHING;

-- Storage & Organization (Low to medium value)
INSERT INTO products (sku, name, description, quantity_on_hand, unit_price) VALUES
    ('STOR-001', 'Storage Bins Set of 6', 'Stackable plastic storage containers', 100, 34.99),
    ('STOR-002', 'File Cabinet 3-Drawer', 'Metal filing cabinet with lock', 20, 149.99),
    ('STOR-003', 'Shelf Dividers 4-Pack', 'Adjustable shelf organizers', 250, 19.99),
    ('STOR-004', 'Label Maker', 'Portable thermal label printer', 60, 44.99),
    ('STOR-005', 'Hanging File Folders 50-Pack', 'Letter size hanging folders', 180, 22.99)
ON CONFLICT (sku) DO NOTHING;

-- Tools & Hardware (Various values)
INSERT INTO products (sku, name, description, quantity_on_hand, unit_price) VALUES
    ('TOOL-001', 'Cordless Drill Set', '20V lithium-ion drill with accessories', 30, 129.99),
    ('TOOL-002', 'Screwdriver Set 40-Piece', 'Precision screwdriver set with case', 75, 24.99),
    ('TOOL-003', 'Digital Multimeter', 'Auto-ranging digital multimeter', 40, 39.99),
    ('TOOL-004', 'LED Work Light', 'Rechargeable 1000 lumen work lamp', 55, 34.99),
    ('TOOL-005', 'Tool Box Large', '3-tier cantilever tool box', 25, 64.99)
ON CONFLICT (sku) DO NOTHING;

-- Low Stock Items (for testing alerts)
INSERT INTO products (sku, name, description, quantity_on_hand, unit_price) VALUES
    ('LOW-001', 'Printer Paper 500 Sheets', 'Premium white copy paper', 5, 8.99),
    ('LOW-002', 'Ink Cartridge Black', 'Compatible ink cartridge', 3, 29.99),
    ('LOW-003', 'Sticky Notes Bulk', 'Assorted color sticky notes', 8, 12.99),
    ('LOW-004', 'Batteries AA 48-Pack', 'Alkaline batteries', 2, 19.99),
    ('LOW-005', 'USB Flash Drive 64GB', 'High-speed USB 3.0 drive', 7, 14.99)
ON CONFLICT (sku) DO NOTHING;

-- ============================================================
-- SAMPLE PURCHASE ORDERS
-- Note: total_amount is computed from order lines, not stored in DB
-- ============================================================

-- Completed order (received)
INSERT INTO purchase_orders (supplier_name, status, created_at, received_at) VALUES
    ('Tech Supplies Inc.', 'RECEIVED', NOW() - INTERVAL '7 days', NOW() - INTERVAL '5 days');

-- Get the ID of the inserted order for lines
DO $$
DECLARE
    po_id BIGINT;
BEGIN
    SELECT id INTO po_id FROM purchase_orders WHERE supplier_name = 'Tech Supplies Inc.' LIMIT 1;
    
    INSERT INTO purchase_order_lines (purchase_order_id, product_id, quantity, unit_price)
    SELECT po_id, id, 10, 149.99 FROM products WHERE sku = 'ELEC-001'
    UNION ALL
    SELECT po_id, id, 25, 49.99 FROM products WHERE sku = 'ELEC-002';
END $$;

-- Pending order
INSERT INTO purchase_orders (supplier_name, status, created_at) VALUES
    ('Office Depot', 'PENDING', NOW() - INTERVAL '2 days');

DO $$
DECLARE
    po_id BIGINT;
BEGIN
    SELECT id INTO po_id FROM purchase_orders WHERE supplier_name = 'Office Depot' AND status = 'PENDING' LIMIT 1;
    
    INSERT INTO purchase_order_lines (purchase_order_id, product_id, quantity, unit_price)
    SELECT po_id, id, 50, 24.99 FROM products WHERE sku = 'OFFC-001'
    UNION ALL
    SELECT po_id, id, 20, 39.99 FROM products WHERE sku = 'OFFC-002';
END $$;

-- Another completed order (older)
INSERT INTO purchase_orders (supplier_name, status, created_at, received_at) VALUES
    ('Hardware World', 'RECEIVED', NOW() - INTERVAL '14 days', NOW() - INTERVAL '12 days');

DO $$
DECLARE
    po_id BIGINT;
BEGIN
    SELECT id INTO po_id FROM purchase_orders WHERE supplier_name = 'Hardware World' LIMIT 1;
    
    INSERT INTO purchase_order_lines (purchase_order_id, product_id, quantity, unit_price)
    SELECT po_id, id, 15, 129.99 FROM products WHERE sku = 'TOOL-001'
    UNION ALL
    SELECT po_id, id, 30, 24.99 FROM products WHERE sku = 'TOOL-002'
    UNION ALL
    SELECT po_id, id, 20, 64.99 FROM products WHERE sku = 'TOOL-005';
END $$;

-- ============================================================
-- SAMPLE STOCK MOVEMENTS (for received orders and adjustments)
-- ============================================================

-- Stock movements from Tech Supplies order
INSERT INTO stock_movements (product_id, change, reason, related_entity_type, related_entity_id, created_at)
SELECT p.id, 10, 'PO_RECEIPT', 'PURCHASE_ORDER', po.id, NOW() - INTERVAL '5 days'
FROM products p, purchase_orders po
WHERE p.sku = 'ELEC-001' AND po.supplier_name = 'Tech Supplies Inc.';

INSERT INTO stock_movements (product_id, change, reason, related_entity_type, related_entity_id, created_at)
SELECT p.id, 25, 'PO_RECEIPT', 'PURCHASE_ORDER', po.id, NOW() - INTERVAL '5 days'
FROM products p, purchase_orders po
WHERE p.sku = 'ELEC-002' AND po.supplier_name = 'Tech Supplies Inc.';

-- Stock movements from Hardware World order
INSERT INTO stock_movements (product_id, change, reason, related_entity_type, related_entity_id, created_at)
SELECT p.id, 15, 'PO_RECEIPT', 'PURCHASE_ORDER', po.id, NOW() - INTERVAL '12 days'
FROM products p, purchase_orders po
WHERE p.sku = 'TOOL-001' AND po.supplier_name = 'Hardware World';

INSERT INTO stock_movements (product_id, change, reason, related_entity_type, related_entity_id, created_at)
SELECT p.id, 30, 'PO_RECEIPT', 'PURCHASE_ORDER', po.id, NOW() - INTERVAL '12 days'
FROM products p, purchase_orders po
WHERE p.sku = 'TOOL-002' AND po.supplier_name = 'Hardware World';

INSERT INTO stock_movements (product_id, change, reason, related_entity_type, related_entity_id, created_at)
SELECT p.id, 20, 'PO_RECEIPT', 'PURCHASE_ORDER', po.id, NOW() - INTERVAL '12 days'
FROM products p, purchase_orders po
WHERE p.sku = 'TOOL-005' AND po.supplier_name = 'Hardware World';

-- Adjustment movements (damages, returns)
INSERT INTO stock_movements (product_id, change, reason, performed_by, created_at)
SELECT id, -2, 'DAMAGE', 'admin', NOW() - INTERVAL '3 days'
FROM products WHERE sku = 'ELEC-003';

INSERT INTO stock_movements (product_id, change, reason, performed_by, created_at)
SELECT id, 3, 'RETURN', 'admin', NOW() - INTERVAL '1 day'
FROM products WHERE sku = 'OFFC-004';

-- Sale movements
INSERT INTO stock_movements (product_id, change, reason, performed_by, created_at)
SELECT id, -5, 'SALE', 'user', NOW() - INTERVAL '2 days'
FROM products WHERE sku = 'ELEC-004';

INSERT INTO stock_movements (product_id, change, reason, performed_by, created_at)
SELECT id, -10, 'SALE', 'user', NOW() - INTERVAL '4 days'
FROM products WHERE sku = 'OFFC-001';

-- Add comments
COMMENT ON TABLE users IS 'User accounts for authentication - seeded with sample data';
