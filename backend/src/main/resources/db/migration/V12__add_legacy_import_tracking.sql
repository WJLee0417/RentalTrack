-- History is imported only through an explicit administrator preview/apply operation.
-- A missing legacy password remains missing; no generated password is assigned.
ALTER TABLE member_credential MODIFY password_hash VARCHAR(100) NULL;

CREATE TABLE legacy_import_mutex (
    id INT NOT NULL PRIMARY KEY
) ENGINE=InnoDB;
INSERT INTO legacy_import_mutex(id) VALUES (1);

CREATE TABLE legacy_import_run (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
    plan_token CHAR(64) NOT NULL,
    source_digest CHAR(64) NOT NULL,
    manifest_json LONGTEXT NOT NULL,
    result_json LONGTEXT NOT NULL,
    imported_by BIGINT UNSIGNED NOT NULL,
    imported_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    CONSTRAINT uk_legacy_import_plan UNIQUE (plan_token),
    CONSTRAINT fk_legacy_import_actor FOREIGN KEY (imported_by) REFERENCES `member`(member_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE legacy_entity_map (
    source_table VARCHAR(40) NOT NULL,
    source_id BIGINT NOT NULL,
    target_table VARCHAR(40) NOT NULL,
    target_id BIGINT UNSIGNED NOT NULL,
    source_digest CHAR(64) NOT NULL,
    import_run_id BIGINT UNSIGNED NOT NULL,
    PRIMARY KEY (source_table,source_id),
    CONSTRAINT uk_legacy_target UNIQUE (target_table,target_id),
    CONSTRAINT fk_legacy_map_run FOREIGN KEY (import_run_id) REFERENCES legacy_import_run(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
