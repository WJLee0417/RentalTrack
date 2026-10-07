CREATE TABLE condition_record (
    id BIGINT NOT NULL AUTO_INCREMENT,
    rental_contract_id BIGINT NOT NULL,
    record_type VARCHAR(20) NOT NULL,
    recorded_by_member_id BIGINT NULL,
    summary VARCHAR(500) NULL,
    recorded_at DATETIME(6) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),

    CONSTRAINT pk_condition_record PRIMARY KEY (id),
    CONSTRAINT fk_condition_record_contract
        FOREIGN KEY (rental_contract_id) REFERENCES rental_contract (id)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT fk_condition_record_member
        FOREIGN KEY (recorded_by_member_id) REFERENCES member_account (id)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT ck_condition_record_type
        CHECK (record_type IN ('BEFORE_RENTAL', 'AFTER_RENTAL')),
    CONSTRAINT uk_condition_record_contract_type
        UNIQUE (rental_contract_id, record_type),

    INDEX idx_condition_record_contract_recorded (rental_contract_id, recorded_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE condition_image (
    id BIGINT NOT NULL AUTO_INCREMENT,
    condition_record_id BIGINT NOT NULL,
    image_url VARCHAR(1000) NOT NULL,
    storage_key VARCHAR(500) NOT NULL,
    capture_order INT NOT NULL,
    viewpoint VARCHAR(30) NULL,
    captured_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

    CONSTRAINT pk_condition_image PRIMARY KEY (id),
    CONSTRAINT fk_condition_image_record
        FOREIGN KEY (condition_record_id) REFERENCES condition_record (id)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT uk_condition_image_storage_key UNIQUE (storage_key),
    CONSTRAINT uk_condition_image_record_order UNIQUE (condition_record_id, capture_order),
    CONSTRAINT ck_condition_image_capture_order CHECK (capture_order > 0),

    INDEX idx_condition_image_record (condition_record_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
