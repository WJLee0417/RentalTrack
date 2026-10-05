CREATE TABLE damage_record (
    id BIGINT NOT NULL AUTO_INCREMENT,
    condition_record_id BIGINT NOT NULL,
    damage_type VARCHAR(30) NOT NULL,
    damage_location VARCHAR(100) NOT NULL,
    severity VARCHAR(20) NOT NULL,
    description VARCHAR(500) NULL,
    comparison_result VARCHAR(20) NOT NULL,
    matched_before_damage_id BIGINT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),

    CONSTRAINT pk_damage_record PRIMARY KEY (id),
    CONSTRAINT fk_damage_record_condition
        FOREIGN KEY (condition_record_id) REFERENCES condition_record (id)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT fk_damage_record_matched_before
        FOREIGN KEY (matched_before_damage_id) REFERENCES damage_record (id)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT uk_damage_record_matched_before UNIQUE (matched_before_damage_id),
    CONSTRAINT ck_damage_record_type
        CHECK (damage_type IN ('SCRATCH', 'DENT', 'CRACK', 'BROKEN', 'STAIN', 'MISSING_PART', 'OTHER')),
    CONSTRAINT ck_damage_record_severity
        CHECK (severity IN ('MINOR', 'MODERATE', 'SEVERE')),
    CONSTRAINT ck_damage_record_comparison_result
        CHECK (comparison_result IN ('BASELINE', 'UNRESOLVED', 'EXISTING', 'NEW')),
    CONSTRAINT ck_damage_record_not_self_match
        CHECK (matched_before_damage_id IS NULL OR matched_before_damage_id <> id),

    INDEX idx_damage_record_condition (condition_record_id),
    INDEX idx_damage_record_condition_result (condition_record_id, comparison_result)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
