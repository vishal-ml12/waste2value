-- ============================================================================
-- V1__init_schema.sql
-- Waste2Value Initial Database Schema
-- ============================================================================

-- 1. USERS
CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    email VARCHAR(150) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    phone VARCHAR(20),
    role VARCHAR(30) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT uk_users_email UNIQUE (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_role ON users(role);
CREATE INDEX idx_users_status ON users(status);

-- 2. REFRESH TOKENS
CREATE TABLE refresh_tokens (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    token_hash VARCHAR(64) NOT NULL,
    user_id BIGINT NOT NULL,
    expiry_date DATETIME(6) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    revoked_at DATETIME(6) NULL,
    replaced_by_token_hash VARCHAR(64) NULL,
    token_family VARCHAR(64) NOT NULL,
    CONSTRAINT uk_refresh_tokens_hash UNIQUE (token_hash),
    CONSTRAINT fk_refresh_tokens_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_rt_hash ON refresh_tokens(token_hash);
CREATE INDEX idx_rt_family ON refresh_tokens(token_family);
CREATE INDEX idx_rt_user ON refresh_tokens(user_id);

-- 3. ADDRESSES
CREATE TABLE addresses (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    address_line1 VARCHAR(255) NOT NULL,
    address_line2 VARCHAR(255) NULL,
    city VARCHAR(100) NOT NULL,
    state VARCHAR(100) NOT NULL,
    postal_code VARCHAR(20) NOT NULL,
    latitude DECIMAL(10, 7) NOT NULL,
    longitude DECIMAL(10, 7) NOT NULL,
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_addresses_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_addresses_user ON addresses(user_id);
CREATE INDEX idx_addresses_city ON addresses(city);
CREATE INDEX idx_addresses_state ON addresses(state);
CREATE INDEX idx_addresses_default ON addresses(is_default);

-- 4. CATEGORIES (Hierarchical)
CREATE TABLE categories (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    parent_id BIGINT NULL,
    name VARCHAR(100) NOT NULL,
    slug VARCHAR(100) NOT NULL,
    description VARCHAR(500) NULL,
    unit VARCHAR(20) NOT NULL DEFAULT 'kg',
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT uk_categories_slug UNIQUE (slug),
    CONSTRAINT fk_categories_parent FOREIGN KEY (parent_id) REFERENCES categories(id) ON DELETE RESTRICT,
    CONSTRAINT uk_categories_parent_name UNIQUE (parent_id, name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_cat_parent ON categories(parent_id);
CREATE INDEX idx_cat_active ON categories(is_active);
CREATE INDEX idx_cat_slug ON categories(slug);

-- 5. LISTINGS
CREATE TABLE listings (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    generator_id BIGINT NOT NULL,
    category_id BIGINT NOT NULL,
    address_id BIGINT NOT NULL,
    title VARCHAR(200) NOT NULL,
    description TEXT NOT NULL,
    quantity DECIMAL(12, 2) NOT NULL,
    unit VARCHAR(20) NOT NULL,
    expected_price DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    status VARCHAR(30) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_listings_generator FOREIGN KEY (generator_id) REFERENCES users(id) ON DELETE RESTRICT,
    CONSTRAINT fk_listings_category FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE RESTRICT,
    CONSTRAINT fk_listings_address FOREIGN KEY (address_id) REFERENCES addresses(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_listings_status ON listings(status);
CREATE INDEX idx_listings_generator ON listings(generator_id);
CREATE INDEX idx_listings_category ON listings(category_id);
CREATE INDEX idx_listings_address ON listings(address_id);
CREATE INDEX idx_listings_created_at ON listings(created_at);
CREATE INDEX idx_listings_price ON listings(expected_price);
CREATE INDEX idx_listings_quantity ON listings(quantity);

-- 6. LISTING IMAGES
CREATE TABLE listing_images (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    listing_id BIGINT NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    file_path VARCHAR(500) NOT NULL,
    file_size BIGINT NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_listing_images_listing FOREIGN KEY (listing_id) REFERENCES listings(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_listing_images_listing ON listing_images(listing_id);
