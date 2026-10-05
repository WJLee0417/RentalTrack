ALTER TABLE condition_image
    ADD COLUMN original_filename VARCHAR(255) NULL AFTER storage_key,
    ADD COLUMN content_type VARCHAR(100) NULL AFTER original_filename,
    ADD COLUMN file_size BIGINT NULL AFTER content_type;

ALTER TABLE condition_image
    DROP COLUMN image_url;

ALTER TABLE condition_image
    ADD CONSTRAINT ck_condition_image_file_size
        CHECK (file_size IS NULL OR file_size >= 0);
