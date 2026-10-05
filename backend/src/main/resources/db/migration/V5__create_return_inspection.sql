CREATE TABLE return_inspection (
    id BIGINT NOT NULL AUTO_INCREMENT,
    rental_contract_id BIGINT NOT NULL,
    result VARCHAR(30) NOT NULL,
    note VARCHAR(1000) NULL,
    inspected_at DATETIME(6) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),

    CONSTRAINT pk_return_inspection PRIMARY KEY (id),
    CONSTRAINT uk_return_inspection_contract UNIQUE (rental_contract_id),
    CONSTRAINT fk_return_inspection_contract
        FOREIGN KEY (rental_contract_id) REFERENCES rental_contract (id)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT ck_return_inspection_result
        CHECK (result IN ('NO_NEW_DAMAGE', 'NEW_DAMAGE_CONFIRMED', 'REVIEW_REQUIRED')),

    INDEX idx_return_inspection_result_inspected (result, inspected_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
