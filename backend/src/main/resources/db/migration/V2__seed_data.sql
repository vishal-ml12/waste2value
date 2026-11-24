-- ============================================================================
-- V2__seed_data.sql
-- Waste2Value Seed Data
-- ============================================================================

-- 1. USERS
INSERT INTO users (id, first_name, last_name, email, password_hash, phone, role, status, created_at, updated_at)
VALUES
(1, 'System', 'Administrator', 'admin@waste2value.com', '$2a$10$HVr22IYieYcJEBnCSCp13uXy4oun8f75KXAkYjfDeXliAAYkroifG', '+91 9876543210', 'ADMIN', 'ACTIVE', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
(2, 'Eco', 'Generator', 'generator@waste2value.com', '$2a$10$F3dtfTgZX5sn2tyc6SwkXueLqsIf5wdJ4arWi1bcanjAKnD57YFTu', '+91 9876543211', 'GENERATOR', 'ACTIVE', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
(3, 'City', 'Collector', 'collector@waste2value.com', '$2a$10$fcU3ayfeDFQpteMZtowFuuvsMDjfOpMgYBR6pPJm8twk7RAfIBJ1y', '+91 9876543212', 'COLLECTOR', 'ACTIVE', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
(4, 'Circular', 'Processor', 'processor@waste2value.com', '$2a$10$IWSX8bPFTO6hUJtfDBvcuOjpqhe/0vwV19F9JG64wMIji8I9n4MSu', '+91 9876543213', 'PROCESSOR', 'ACTIVE', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6));

-- 2. PARENT CATEGORIES
INSERT INTO categories (id, parent_id, name, slug, description, unit, is_active, created_at, updated_at)
VALUES
(1, NULL, 'Plastic', 'plastic', 'Recyclable plastics including polymers and rigid plastics', 'kg', TRUE, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
(2, NULL, 'Paper', 'paper', 'Cellulose-based recyclable paper, boards, and packaging', 'kg', TRUE, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
(3, NULL, 'Metal', 'metal', 'Ferrous and non-ferrous scrap metals', 'kg', TRUE, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
(4, NULL, 'Glass', 'glass', 'Container glass, bottles, and cullet', 'kg', TRUE, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
(5, NULL, 'E-Waste', 'e-waste', 'Electronic devices, circuitry, and electrical components', 'items', TRUE, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
(6, NULL, 'Textile', 'textile', 'Post-consumer and industrial textile fabrics and fibres', 'kg', TRUE, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
(7, NULL, 'Organic Waste', 'organic-waste', 'Biodegradable composting and biogas feedstocks', 'kg', TRUE, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6));

-- 3. SUB-CATEGORIES
INSERT INTO categories (id, parent_id, name, slug, description, unit, is_active, created_at, updated_at)
VALUES
(8, 1, 'PET Bottles', 'pet-bottles', 'Polyethylene terephthalate clear and tinted bottles', 'kg', TRUE, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
(9, 1, 'HDPE', 'hdpe', 'High-density polyethylene containers and caps', 'kg', TRUE, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
(10, 1, 'Mixed Plastic', 'mixed-plastic', 'Assorted post-consumer flexible and rigid plastic mix', 'kg', TRUE, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
(11, 2, 'Cardboard', 'cardboard', 'Corrugated cardboard boxes and packaging cartons', 'kg', TRUE, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
(12, 2, 'Newspaper', 'newspaper', 'De-inkable newsprint and printed paper', 'kg', TRUE, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
(13, 3, 'Aluminium', 'aluminium', 'Aluminium cans, sheets, and extrusions', 'kg', TRUE, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
(14, 3, 'Steel', 'steel', 'Steel scrap, tin cans, and structural scrap', 'kg', TRUE, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6));

-- 4. ADDRESSES
INSERT INTO addresses (id, user_id, address_line1, address_line2, city, state, postal_code, latitude, longitude, is_default, created_at, updated_at)
VALUES
(1, 2, 'Plot 42, Green Tech Park', 'MIDC Andheri East', 'Mumbai', 'Maharashtra', '400093', 19.1136000, 72.8697000, TRUE, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
(2, 2, 'Warehouse B-12', 'Bhiwandi Logistics Hub', 'Thane', 'Maharashtra', '421302', 19.2967000, 73.0622000, FALSE, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6));

-- 5. LISTINGS
INSERT INTO listings (id, generator_id, category_id, address_id, title, description, quantity, unit, expected_price, status, version, created_at, updated_at)
VALUES
(1, 2, 8, 1, 'Clean Sorted PET Bottles (Baled)', 'Over 400 kg of sorted, washed, and baled clear PET beverage bottles. Caps removed and stored dry in warehouse.', 450.00, 'kg', 18.00, 'POSTED', 0, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
(2, 2, 11, 1, 'Industrial Corrugated Cardboard Boxes', 'Flatted corrugated packaging cardboard from ongoing warehouse unpackaging. Dry, bundled, and ready for pickup.', 1200.00, 'kg', 9.50, 'POSTED', 0, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
(3, 2, 13, 2, 'Clean Aluminium Beverage Cans', 'Crushed and sorted beverage cans. Magnetically separated from steel, completely moisture free.', 180.00, 'kg', 110.00, 'POSTED', 0, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
(4, 2, 5, 1, 'Decommissioned Server Racks and Power Units', 'IT infrastructure refresh surplus. Contains server cases, motherboards, and power supplies. Certified disposal required.', 25.00, 'items', 350.00, 'DRAFT', 0, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6));
