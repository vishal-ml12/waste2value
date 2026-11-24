-- ============================================================================
-- V4__phase2_seed_data.sql
-- Waste2Value Phase 2 Seed Data: Verified Collector & Processor Profiles, Capabilities
-- ============================================================================

-- 1. SEED COLLECTOR PROFILE (User 3: City Collector - Verified)
INSERT INTO collector_profiles (
    id, user_id, vehicle_type, service_area, max_distance_km, is_verified, avg_rating, total_pickups, verified_at, verified_by_user_id, created_at, updated_at
) VALUES (
    1, 3, 'Medium Electric Flatbed Truck (Tata Ace EV)', 'Mumbai Metropolitan Region & Thane', 60.00, TRUE, 4.85, 12, CURRENT_TIMESTAMP(6), 1, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)
);

-- 2. SEED PROCESSOR PROFILE (User 4: Circular Processor - Verified)
INSERT INTO processor_profiles (
    id, user_id, company_name, license_number, description, max_capacity_kg, current_load_kg, is_verified, avg_rating, verified_at, verified_by_user_id, created_at, updated_at
) VALUES (
    1, 4, 'Circular Materials Refining & Polymer Recovery Ltd.', 'MPCB-RECYCLE-2024-MH-9941', 'State-of-the-art polymer washing, pelletizing, and secondary metallurgy recycling facility.', 25000.00, 4200.00, TRUE, 4.90, CURRENT_TIMESTAMP(6), 1, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)
);

-- 3. SEED PROCESSOR CAPABILITIES (Accepts PET Bottles, HDPE, Aluminium)
INSERT INTO processor_capabilities (
    id, processor_id, category_id, min_quantity, max_quantity, price_per_unit, is_active, created_at, updated_at
) VALUES
(1, 1, 8, 100.00, 5000.00, 22.50, TRUE, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),   -- PET Bottles: 22.50 / kg
(2, 1, 9, 100.00, 4000.00, 28.00, TRUE, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),   -- HDPE: 28.00 / kg
(3, 1, 13, 50.00, 2000.00, 115.00, TRUE, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6));  -- Aluminium: 115.00 / kg
