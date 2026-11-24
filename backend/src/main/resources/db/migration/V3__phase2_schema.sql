-- ============================================================================
-- V3__phase2_schema.sql
-- Waste2Value Phase 2 Schema: Collectors, Processors, Pickups, Transactions,
-- Material Receipts, Reviews, Notifications, Reports/Disputes
-- ============================================================================

-- 1. COLLECTOR PROFILES
CREATE TABLE collector_profiles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    vehicle_type VARCHAR(100) NOT NULL,
    service_area VARCHAR(255) NOT NULL,
    max_distance_km DECIMAL(6, 2) NOT NULL DEFAULT 50.00,
    is_verified BOOLEAN NOT NULL DEFAULT FALSE,
    avg_rating DECIMAL(3, 2) NOT NULL DEFAULT 0.00,
    total_pickups INT NOT NULL DEFAULT 0,
    verified_at DATETIME(6) NULL,
    verified_by_user_id BIGINT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT uk_collector_user UNIQUE (user_id),
    CONSTRAINT fk_collector_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_collector_verified_by FOREIGN KEY (verified_by_user_id) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_collector_verified ON collector_profiles(is_verified);
CREATE INDEX idx_collector_user ON collector_profiles(user_id);

-- 2. PROCESSOR PROFILES
CREATE TABLE processor_profiles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    company_name VARCHAR(150) NOT NULL,
    license_number VARCHAR(100) NOT NULL,
    description TEXT NULL,
    max_capacity_kg DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    current_load_kg DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    is_verified BOOLEAN NOT NULL DEFAULT FALSE,
    avg_rating DECIMAL(3, 2) NOT NULL DEFAULT 0.00,
    verified_at DATETIME(6) NULL,
    verified_by_user_id BIGINT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT uk_processor_user UNIQUE (user_id),
    CONSTRAINT fk_processor_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_processor_verified_by FOREIGN KEY (verified_by_user_id) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_processor_verified ON processor_profiles(is_verified);
CREATE INDEX idx_processor_user ON processor_profiles(user_id);

-- 3. PROCESSOR CAPABILITIES
CREATE TABLE processor_capabilities (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    processor_id BIGINT NOT NULL,
    category_id BIGINT NOT NULL,
    min_quantity DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    max_quantity DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    price_per_unit DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT uk_proc_cap_unique UNIQUE (processor_id, category_id),
    CONSTRAINT fk_proc_cap_processor FOREIGN KEY (processor_id) REFERENCES processor_profiles(id) ON DELETE CASCADE,
    CONSTRAINT fk_proc_cap_category FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_proc_cap_proc ON processor_capabilities(processor_id);
CREATE INDEX idx_proc_cap_cat ON processor_capabilities(category_id);
CREATE INDEX idx_proc_cap_active ON processor_capabilities(is_active);

-- 4. PICKUP REQUESTS
CREATE TABLE pickup_requests (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    listing_id BIGINT NOT NULL,
    collector_id BIGINT NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'REQUESTED',
    proposed_price DECIMAL(12, 2) NOT NULL,
    note TEXT NULL,
    actual_quantity DECIMAL(12, 2) NULL,
    completion_note TEXT NULL,
    requested_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    responded_at DATETIME(6) NULL,
    completed_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_pr_listing FOREIGN KEY (listing_id) REFERENCES listings(id) ON DELETE RESTRICT,
    CONSTRAINT fk_pr_collector FOREIGN KEY (collector_id) REFERENCES users(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_pr_listing ON pickup_requests(listing_id);
CREATE INDEX idx_pr_collector ON pickup_requests(collector_id);
CREATE INDEX idx_pr_status ON pickup_requests(status);
CREATE INDEX idx_pr_created_at ON pickup_requests(created_at);

-- 5. TRANSACTIONS
CREATE TABLE transactions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    listing_id BIGINT NOT NULL,
    pickup_request_id BIGINT NOT NULL,
    generator_id BIGINT NOT NULL,
    collector_id BIGINT NOT NULL,
    processor_id BIGINT NULL,
    quantity DECIMAL(12, 2) NOT NULL,
    unit VARCHAR(20) NOT NULL,
    agreed_price DECIMAL(12, 2) NOT NULL,
    total_amount DECIMAL(14, 2) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'COMPLETED',
    transaction_date DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT uk_tx_pickup_request UNIQUE (pickup_request_id),
    CONSTRAINT fk_tx_listing FOREIGN KEY (listing_id) REFERENCES listings(id) ON DELETE RESTRICT,
    CONSTRAINT fk_tx_pickup_request FOREIGN KEY (pickup_request_id) REFERENCES pickup_requests(id) ON DELETE RESTRICT,
    CONSTRAINT fk_tx_generator FOREIGN KEY (generator_id) REFERENCES users(id) ON DELETE RESTRICT,
    CONSTRAINT fk_tx_collector FOREIGN KEY (collector_id) REFERENCES users(id) ON DELETE RESTRICT,
    CONSTRAINT fk_tx_processor FOREIGN KEY (processor_id) REFERENCES processor_profiles(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_tx_generator ON transactions(generator_id);
CREATE INDEX idx_tx_collector ON transactions(collector_id);
CREATE INDEX idx_tx_processor ON transactions(processor_id);
CREATE INDEX idx_tx_listing ON transactions(listing_id);
CREATE INDEX idx_tx_status ON transactions(status);

-- 6. PROCESSOR RECEIPTS (Deliveries to Processor)
CREATE TABLE processor_receipts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    listing_id BIGINT NULL,
    pickup_request_id BIGINT NULL,
    processor_id BIGINT NOT NULL,
    collector_id BIGINT NOT NULL,
    category_id BIGINT NOT NULL,
    quantity DECIMAL(12, 2) NOT NULL,
    unit VARCHAR(20) NOT NULL,
    agreed_price DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    total_amount DECIMAL(14, 2) NOT NULL DEFAULT 0.00,
    status VARCHAR(30) NOT NULL DEFAULT 'RECEIVED',
    notes TEXT NULL,
    received_date DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_receipt_listing FOREIGN KEY (listing_id) REFERENCES listings(id) ON DELETE SET NULL,
    CONSTRAINT fk_receipt_pickup FOREIGN KEY (pickup_request_id) REFERENCES pickup_requests(id) ON DELETE SET NULL,
    CONSTRAINT fk_receipt_processor FOREIGN KEY (processor_id) REFERENCES processor_profiles(id) ON DELETE RESTRICT,
    CONSTRAINT fk_receipt_collector FOREIGN KEY (collector_id) REFERENCES users(id) ON DELETE RESTRICT,
    CONSTRAINT fk_receipt_category FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_receipt_processor ON processor_receipts(processor_id);
CREATE INDEX idx_receipt_collector ON processor_receipts(collector_id);
CREATE INDEX idx_receipt_category ON processor_receipts(category_id);
CREATE INDEX idx_receipt_status ON processor_receipts(status);

-- 7. REVIEWS
CREATE TABLE reviews (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    reviewer_id BIGINT NOT NULL,
    target_user_id BIGINT NOT NULL,
    pickup_request_id BIGINT NOT NULL,
    rating INT NOT NULL,
    comment TEXT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT uk_review_pickup_reviewer UNIQUE (pickup_request_id, reviewer_id),
    CONSTRAINT fk_review_reviewer FOREIGN KEY (reviewer_id) REFERENCES users(id) ON DELETE RESTRICT,
    CONSTRAINT fk_review_target FOREIGN KEY (target_user_id) REFERENCES users(id) ON DELETE RESTRICT,
    CONSTRAINT fk_review_pickup FOREIGN KEY (pickup_request_id) REFERENCES pickup_requests(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_review_target ON reviews(target_user_id);
CREATE INDEX idx_review_reviewer ON reviews(reviewer_id);
CREATE INDEX idx_review_pickup ON reviews(pickup_request_id);

-- 8. NOTIFICATIONS
CREATE TABLE notifications (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    type VARCHAR(50) NOT NULL,
    title VARCHAR(200) NOT NULL,
    message TEXT NOT NULL,
    related_entity_type VARCHAR(50) NULL,
    related_entity_id BIGINT NULL,
    read_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_notifications_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_notif_user ON notifications(user_id);
CREATE INDEX idx_notif_user_read ON notifications(user_id, read_at);
CREATE INDEX idx_notif_created_at ON notifications(created_at);

-- 9. REPORTS & DISPUTES
CREATE TABLE reports (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    reporter_id BIGINT NOT NULL,
    target_type VARCHAR(30) NOT NULL,
    target_id BIGINT NOT NULL,
    reason VARCHAR(255) NOT NULL,
    description TEXT NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'OPEN',
    admin_note TEXT NULL,
    resolved_by_user_id BIGINT NULL,
    resolved_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_reports_reporter FOREIGN KEY (reporter_id) REFERENCES users(id) ON DELETE RESTRICT,
    CONSTRAINT fk_reports_resolved_by FOREIGN KEY (resolved_by_user_id) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_reports_reporter ON reports(reporter_id);
CREATE INDEX idx_reports_status ON reports(status);
CREATE INDEX idx_reports_target ON reports(target_type, target_id);
