-- ============================================================================
-- V6__environmental_factors.sql
-- Waste2Value Phase 3B: Environmental Impact & Emission Factors Schema
-- Configurable GHG emission factors and landfill diversion rates per waste category
-- ============================================================================

CREATE TABLE environmental_emission_factors (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    category_id BIGINT NOT NULL,
    co2_factor_kg_per_unit DECIMAL(10, 4) NULL COMMENT 'Estimated kg CO2e avoided per unit of recycled material; NULL if factor is unconfigured',
    landfill_diversion_rate DECIMAL(5, 4) NOT NULL DEFAULT 1.0000 COMMENT 'Fraction (0.0000 to 1.0000) of processed material diverted from landfill',
    unit VARCHAR(20) NOT NULL DEFAULT 'kg',
    source_reference VARCHAR(255) NOT NULL COMMENT 'Authoritative published reference, e.g. US EPA WARM v15, UK DEFRA',
    methodology_notes TEXT NULL COMMENT 'Methodology summary describing lifecycle boundaries and emission offsets',
    effective_from DATE NOT NULL,
    effective_to DATE NULL COMMENT 'NULL indicates the factor is currently in effect without expiration',
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_eef_category FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE RESTRICT,
    CONSTRAINT uk_eef_category_effective UNIQUE (category_id, effective_from)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_eef_category ON environmental_emission_factors(category_id);
CREATE INDEX idx_eef_active ON environmental_emission_factors(is_active);
CREATE INDEX idx_eef_effective ON environmental_emission_factors(effective_from, effective_to);

-- Seed Initial Verified Emission Factors based on US EPA WARM v15 & UK DEFRA / WRAP
-- 1. Plastic (Parent Category, ID: 1)
INSERT INTO environmental_emission_factors (
    category_id, co2_factor_kg_per_unit, landfill_diversion_rate, unit, source_reference, methodology_notes, effective_from, effective_to, is_active
) VALUES (
    1, 1.3000, 1.0000, 'kg', 'US EPA WARM v15 (Mixed Plastics Recovery)',
    'Lifecycle emissions avoided by displacing virgin polymer synthesis with mechanical recycling, net of reprocessing energy.',
    '2024-01-01', NULL, TRUE
);

-- 2. Paper (Parent Category, ID: 2)
INSERT INTO environmental_emission_factors (
    category_id, co2_factor_kg_per_unit, landfill_diversion_rate, unit, source_reference, methodology_notes, effective_from, effective_to, is_active
) VALUES (
    2, 2.8000, 1.0000, 'kg', 'US EPA WARM v15 (General Paper Products Average)',
    'Avoided emissions from virgin pulp processing, chemical digestors, and avoided anaerobic landfill degradation.',
    '2024-01-01', NULL, TRUE
);

-- 3. Metal (Parent Category, ID: 3)
INSERT INTO environmental_emission_factors (
    category_id, co2_factor_kg_per_unit, landfill_diversion_rate, unit, source_reference, methodology_notes, effective_from, effective_to, is_active
) VALUES (
    3, 3.5000, 1.0000, 'kg', 'US EPA WARM v15 (Mixed Metals Average)',
    'Primary reduction in mining, transport, and metallurgical smelting energy consumption.',
    '2024-01-01', NULL, TRUE
);

-- 4. Glass (Parent Category, ID: 4)
INSERT INTO environmental_emission_factors (
    category_id, co2_factor_kg_per_unit, landfill_diversion_rate, unit, source_reference, methodology_notes, effective_from, effective_to, is_active
) VALUES (
    4, 0.3100, 1.0000, 'kg', 'US EPA WARM v15 (Container Glass)',
    'Emissions avoided from reduced furnace melting temperatures when using recycled cullet vs virgin silica sand, soda ash, and limestone.',
    '2024-01-01', NULL, TRUE
);

-- 5. E-Waste (Parent Category, ID: 5) - UNCONFIGURED: Complex heterogeneous composition precludes uniform per-item factor without audit
INSERT INTO environmental_emission_factors (
    category_id, co2_factor_kg_per_unit, landfill_diversion_rate, unit, source_reference, methodology_notes, effective_from, effective_to, is_active
) VALUES (
    5, NULL, 0.8500, 'items', 'CPCB / Basel Convention Guidelines (Unconfigured Factor)',
    'Complex multi-material electronic assembly. No standardized per-item CO2 conversion factor is applied to avoid unverified estimates.',
    '2024-01-01', NULL, TRUE
);

-- 6. Textile (Parent Category, ID: 6)
INSERT INTO environmental_emission_factors (
    category_id, co2_factor_kg_per_unit, landfill_diversion_rate, unit, source_reference, methodology_notes, effective_from, effective_to, is_active
) VALUES (
    6, 3.2000, 0.9500, 'kg', 'WRAP UK / DEFRA GHG Conversion Factors (Post-Consumer Textiles)',
    'Avoided virgin cotton cultivation (irrigation and fertilizer footprint) and synthetic polyester petroleum cracking.',
    '2024-01-01', NULL, TRUE
);

-- 7. Organic Waste (Parent Category, ID: 7)
INSERT INTO environmental_emission_factors (
    category_id, co2_factor_kg_per_unit, landfill_diversion_rate, unit, source_reference, methodology_notes, effective_from, effective_to, is_active
) VALUES (
    7, 0.2400, 1.0000, 'kg', 'US EPA WARM v15 (Food Waste Composting)',
    'Avoided fugitive methane (CH4) emissions resulting from anaerobic decomposition in municipal landfills.',
    '2024-01-01', NULL, TRUE
);

-- 8. Sub-category: PET Bottles (ID: 8)
INSERT INTO environmental_emission_factors (
    category_id, co2_factor_kg_per_unit, landfill_diversion_rate, unit, source_reference, methodology_notes, effective_from, effective_to, is_active
) VALUES (
    8, 1.5300, 1.0000, 'kg', 'US EPA WARM v15 (Polyethylene Terephthalate - PET)',
    'Closed-loop bottle-to-bottle and flake recycling displacing virgin PTA and MEG petrochemical feedstocks.',
    '2024-01-01', NULL, TRUE
);

-- 9. Sub-category: HDPE (ID: 9)
INSERT INTO environmental_emission_factors (
    category_id, co2_factor_kg_per_unit, landfill_diversion_rate, unit, source_reference, methodology_notes, effective_from, effective_to, is_active
) VALUES (
    9, 1.4000, 1.0000, 'kg', 'US EPA WARM v15 (High-Density Polyethylene - HDPE)',
    'Pelletized recycled HDPE displacing ethylene polymerization processes.',
    '2024-01-01', NULL, TRUE
);

-- 10. Sub-category: Mixed Plastic (ID: 10)
INSERT INTO environmental_emission_factors (
    category_id, co2_factor_kg_per_unit, landfill_diversion_rate, unit, source_reference, methodology_notes, effective_from, effective_to, is_active
) VALUES (
    10, 1.0200, 0.9000, 'kg', 'US EPA WARM v15 (Mixed Plastics Recovery)',
    'Aggregated polymer recovery, accounting for 10% unrecyclable processing residue.',
    '2024-01-01', NULL, TRUE
);

-- 11. Sub-category: Cardboard (ID: 11)
INSERT INTO environmental_emission_factors (
    category_id, co2_factor_kg_per_unit, landfill_diversion_rate, unit, source_reference, methodology_notes, effective_from, effective_to, is_active
) VALUES (
    11, 3.1400, 1.0000, 'kg', 'US EPA WARM v15 (Corrugated Containers / OCC)',
    'Avoided virgin timber harvesting, kraft pulping chemical consumption, and landfill methane prevention.',
    '2024-01-01', NULL, TRUE
);

-- 12. Sub-category: Newspaper (ID: 12)
INSERT INTO environmental_emission_factors (
    category_id, co2_factor_kg_per_unit, landfill_diversion_rate, unit, source_reference, methodology_notes, effective_from, effective_to, is_active
) VALUES (
    12, 2.7200, 1.0000, 'kg', 'US EPA WARM v15 (Newsprint Recycling)',
    'De-inking and secondary repulping vs virgin mechanical thermomechanical pulping.',
    '2024-01-01', NULL, TRUE
);

-- 13. Sub-category: Aluminium (ID: 13)
INSERT INTO environmental_emission_factors (
    category_id, co2_factor_kg_per_unit, landfill_diversion_rate, unit, source_reference, methodology_notes, effective_from, effective_to, is_active
) VALUES (
    13, 9.1200, 1.0000, 'kg', 'US EPA WARM v15 (Aluminium Ingot & Cans Recycling)',
    'Secondary re-melting requires only 5% of electrical energy required by virgin Hall-Héroult bauxite electrolysis.',
    '2024-01-01', NULL, TRUE
);

-- 14. Sub-category: Steel (ID: 14)
INSERT INTO environmental_emission_factors (
    category_id, co2_factor_kg_per_unit, landfill_diversion_rate, unit, source_reference, methodology_notes, effective_from, effective_to, is_active
) VALUES (
    14, 1.7800, 1.0000, 'kg', 'US EPA WARM v15 (Steel Scrap Recycling)',
    'Electric Arc Furnace (EAF) recycling vs integrated Basic Oxygen Furnace (BOF) iron ore and metallurgical coke reduction.',
    '2024-01-01', NULL, TRUE
);
