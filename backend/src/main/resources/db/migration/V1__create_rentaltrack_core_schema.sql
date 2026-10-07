CREATE TABLE rental_location (
    id BIGINT NOT NULL AUTO_INCREMENT,
    code VARCHAR(30) NOT NULL,
    name VARCHAR(100) NOT NULL,
    address VARCHAR(255) NOT NULL,
    latitude DECIMAL(10, 7) NOT NULL,
    longitude DECIMAL(10, 7) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),

    CONSTRAINT pk_rental_location PRIMARY KEY (id),
    CONSTRAINT uk_rental_location_code UNIQUE (code),
    CONSTRAINT ck_rental_location_latitude CHECK (latitude BETWEEN -90 AND 90),
    CONSTRAINT ck_rental_location_longitude CHECK (longitude BETWEEN -180 AND 180)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE product_category (
    id BIGINT NOT NULL AUTO_INCREMENT,
    code VARCHAR(30) NOT NULL,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500) NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),

    CONSTRAINT pk_product_category PRIMARY KEY (id),
    CONSTRAINT uk_product_category_code UNIQUE (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE rental_product (
    id BIGINT NOT NULL AUTO_INCREMENT,
    asset_code VARCHAR(50) NOT NULL,
    display_name VARCHAR(100) NOT NULL,
    category_id BIGINT NOT NULL,
    current_location_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE',
    version BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),

    CONSTRAINT pk_rental_product PRIMARY KEY (id),
    CONSTRAINT uk_rental_product_asset_code UNIQUE (asset_code),
    CONSTRAINT fk_rental_product_category
        FOREIGN KEY (category_id) REFERENCES product_category (id)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT fk_rental_product_current_location
        FOREIGN KEY (current_location_id) REFERENCES rental_location (id)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT ck_rental_product_status
        CHECK (status IN ('AVAILABLE', 'RESERVED', 'RENTED', 'INSPECTION', 'MAINTENANCE', 'RETIRED')),

    INDEX idx_rental_product_location_category_status (current_location_id, category_id, status),
    INDEX idx_rental_product_category_status (category_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE rental_contract (
    id BIGINT NOT NULL AUTO_INCREMENT,
    contract_code VARCHAR(50) NOT NULL,
    member_id BIGINT NOT NULL COMMENT 'Member FK is added in a later migration',
    rental_product_id BIGINT NOT NULL,
    pickup_location_id BIGINT NOT NULL,
    return_location_id BIGINT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'REQUESTED',
    requested_at DATETIME(6) NOT NULL,
    approved_at DATETIME(6) NULL,
    rented_at DATETIME(6) NULL,
    due_at DATETIME(6) NULL,
    returned_at DATETIME(6) NULL,
    completed_at DATETIME(6) NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),

    CONSTRAINT pk_rental_contract PRIMARY KEY (id),
    CONSTRAINT uk_rental_contract_code UNIQUE (contract_code),
    CONSTRAINT fk_rental_contract_product
        FOREIGN KEY (rental_product_id) REFERENCES rental_product (id)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT fk_rental_contract_pickup_location
        FOREIGN KEY (pickup_location_id) REFERENCES rental_location (id)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT fk_rental_contract_return_location
        FOREIGN KEY (return_location_id) REFERENCES rental_location (id)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT ck_rental_contract_status
        CHECK (status IN ('REQUESTED', 'APPROVED', 'IN_USE', 'RETURNED', 'INSPECTING', 'COMPLETED', 'REJECTED', 'CANCELED')),
    CONSTRAINT ck_rental_contract_due_after_rented
        CHECK (rented_at IS NULL OR due_at IS NULL OR due_at > rented_at),
    CONSTRAINT ck_rental_contract_return_after_rented
        CHECK (rented_at IS NULL OR returned_at IS NULL OR returned_at >= rented_at),

    INDEX idx_rental_contract_member_status (member_id, status),
    INDEX idx_rental_contract_product_status (rental_product_id, status),
    INDEX idx_rental_contract_pickup_requested (pickup_location_id, requested_at),
    INDEX idx_rental_contract_return_returned (return_location_id, returned_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
