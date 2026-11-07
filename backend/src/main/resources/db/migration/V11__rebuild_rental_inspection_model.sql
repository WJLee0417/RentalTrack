-- New model supplied on 2026-11-07. Constraints/code values below are implementation decisions.
-- Preserve legacy data and V1-V10 checksums; do not invent location -> business mappings.
RENAME TABLE rental_contract TO legacy_rental_contract, condition_record TO legacy_condition_record, return_inspection TO legacy_return_inspection;

CREATE TABLE `common_code_group` (
  `code_group_id` VARCHAR(40) NOT NULL,
  `code_group_name` VARCHAR(100) NOT NULL,
  `code_group_description` VARCHAR(500) NULL,
  `use_yn` CHAR(1) NOT NULL,
  `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`code_group_id`),
  CHECK (`use_yn` IN ('Y','N'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `common_code` (
  `code_group_id` VARCHAR(40) NOT NULL,
  `code_value` VARCHAR(40) NOT NULL,
  `code_name` VARCHAR(100) NOT NULL,
  `code_description` VARCHAR(500) NULL,
  `display_order` INT NOT NULL,
  `use_yn` CHAR(1) NOT NULL,
  `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`code_group_id`, `code_value`),
  CONSTRAINT v2_fk_common_code_code_group_id FOREIGN KEY (`code_group_id`) REFERENCES `common_code_group` (`code_group_id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CHECK (`use_yn` IN ('Y','N'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `member` (
  `member_id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `login_id` VARCHAR(50) NOT NULL,
  `member_name` VARCHAR(100) NOT NULL,
  `email` VARCHAR(254) NOT NULL,
  `member_role_cd` VARCHAR(20) NOT NULL,
  `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`member_id`),
  CONSTRAINT v2_uk_member_0 UNIQUE (`login_id`),
  CONSTRAINT v2_uk_member_1 UNIQUE (`email`),
  CHECK (`member_role_cd` IN ('CUSTOMER', 'INSPECTOR', 'ADMIN'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `business` (
  `business_id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `business_no` VARCHAR(10) NOT NULL,
  `business_name` VARCHAR(200) NOT NULL,
  `business_status_cd` VARCHAR(20) NOT NULL,
  `contact_email` VARCHAR(254) NULL,
  `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`business_id`),
  CONSTRAINT v2_uk_business_0 UNIQUE (`business_no`),
  CHECK (`business_status_cd` IN ('ACTIVE', 'SUSPENDED', 'CLOSED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `rental_item` (
  `item_id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `business_id` BIGINT UNSIGNED NOT NULL,
  `asset_code` VARCHAR(40) NOT NULL,
  `item_name` VARCHAR(200) NOT NULL,
  `category_cd` VARCHAR(30) NOT NULL,
  `item_status_cd` VARCHAR(20) NOT NULL,
  `description` VARCHAR(1000) NULL,
  `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`item_id`),
  CONSTRAINT v2_uk_rental_item_0 UNIQUE (`business_id`, `asset_code`),
  CONSTRAINT v2_fk_rental_item_business_id FOREIGN KEY (`business_id`) REFERENCES `business` (`business_id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CHECK (`category_cd` IN ('CAMERA', 'COMPUTER', 'HOME_APPLIANCE', 'SPORTS')),
  CHECK (`item_status_cd` IN ('AVAILABLE', 'RENTED', 'INSPECTION', 'REPAIR', 'INACTIVE'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `rental_contract` (
  `contract_id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `contract_no` VARCHAR(50) NOT NULL,
  `member_id` BIGINT UNSIGNED NOT NULL,
  `item_id` BIGINT UNSIGNED NOT NULL,
  `requested_at` DATETIME(3) NOT NULL,
  `rental_start_at` DATETIME(3) NULL,
  `rental_end_at` DATETIME(3) NULL,
  `actual_return_at` DATETIME(3) NULL,
  `contract_status_cd` VARCHAR(20) NOT NULL,
  `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`contract_id`),
  CONSTRAINT v2_uk_rental_contract_0 UNIQUE (`contract_no`),
  CONSTRAINT v2_fk_rental_contract_member_id FOREIGN KEY (`member_id`) REFERENCES `member` (`member_id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT v2_fk_rental_contract_item_id FOREIGN KEY (`item_id`) REFERENCES `rental_item` (`item_id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CHECK (`contract_status_cd` IN ('REQUESTED', 'RENTED', 'RETURNED', 'INSPECTED', 'CANCELED')),
  CHECK (rental_end_at IS NULL OR rental_end_at > rental_start_at),
  CHECK (actual_return_at IS NULL OR actual_return_at >= rental_start_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `condition_record` (
  `condition_id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `contract_id` BIGINT UNSIGNED NOT NULL,
  `record_stage_cd` VARCHAR(10) NOT NULL,
  `record_revision_no` INT NOT NULL,
  `recorded_at` DATETIME(3) NOT NULL,
  `record_note` VARCHAR(1000) NULL,
  `confirmed_yn` CHAR(1) NOT NULL,
  `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`condition_id`),
  CONSTRAINT v2_uk_condition_record_0 UNIQUE (`contract_id`, `record_stage_cd`, `record_revision_no`),
  CONSTRAINT v2_fk_condition_record_contract_id FOREIGN KEY (`contract_id`) REFERENCES `rental_contract` (`contract_id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CHECK (`record_stage_cd` IN ('INTAKE', 'RETURN')),
  CHECK (`record_revision_no` > 0),
  CHECK (`confirmed_yn` IN ('Y','N'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `condition_photo` (
  `photo_id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `condition_id` BIGINT UNSIGNED NOT NULL,
  `photo_seq` INT NOT NULL,
  `storage_key` VARCHAR(500) NOT NULL,
  `original_file_name` VARCHAR(255) NOT NULL,
  `mime_type` VARCHAR(100) NOT NULL,
  `file_size` BIGINT UNSIGNED NOT NULL,
  `file_hash` CHAR(64) NOT NULL,
  `capture_angle_cd` VARCHAR(30) NOT NULL,
  `captured_at` DATETIME(3) NULL,
  `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`photo_id`),
  CONSTRAINT v2_uk_condition_photo_0 UNIQUE (`condition_id`, `photo_seq`),
  CONSTRAINT v2_fk_condition_photo_condition_id FOREIGN KEY (`condition_id`) REFERENCES `condition_record` (`condition_id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CHECK (`photo_seq` > 0),
  CHECK (`capture_angle_cd` IN ('FRONT', 'BACK', 'LEFT', 'RIGHT', 'TOP', 'BOTTOM', 'DETAIL', 'OTHER'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `damage_observation` (
  `damage_id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `condition_id` BIGINT UNSIGNED NOT NULL,
  `damage_location_cd` VARCHAR(40) NOT NULL,
  `damage_type_cd` VARCHAR(40) NOT NULL,
  `damage_description` VARCHAR(1000) NULL,
  `severity_cd` VARCHAR(20) NOT NULL,
  `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`damage_id`),
  CONSTRAINT v2_fk_damage_observation_condition_id FOREIGN KEY (`condition_id`) REFERENCES `condition_record` (`condition_id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CHECK (`damage_location_cd` IN ('FRONT', 'BACK', 'LEFT', 'RIGHT', 'TOP', 'BOTTOM', 'EDGE', 'SCREEN', 'LENS', 'PORT', 'OTHER')),
  CHECK (`damage_type_cd` IN ('SCRATCH', 'DENT', 'CRACK', 'BROKEN', 'STAIN', 'DISCOLORATION', 'OTHER')),
  CHECK (`severity_cd` IN ('MINOR', 'MODERATE', 'SEVERE', 'UNKNOWN'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `damage_photo` (
  `damage_photo_id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `damage_id` BIGINT UNSIGNED NOT NULL,
  `photo_id` BIGINT UNSIGNED NOT NULL,
  `bbox_x_ratio` DECIMAL(5,4) NULL,
  `bbox_y_ratio` DECIMAL(5,4) NULL,
  `bbox_width_ratio` DECIMAL(5,4) NULL,
  `bbox_height_ratio` DECIMAL(5,4) NULL,
  `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`damage_photo_id`),
  CONSTRAINT v2_uk_damage_photo_0 UNIQUE (`damage_id`, `photo_id`),
  CONSTRAINT v2_fk_damage_photo_damage_id FOREIGN KEY (`damage_id`) REFERENCES `damage_observation` (`damage_id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT v2_fk_damage_photo_photo_id FOREIGN KEY (`photo_id`) REFERENCES `condition_photo` (`photo_id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CHECK (`bbox_x_ratio` BETWEEN 0 AND 1),
  CHECK (`bbox_y_ratio` BETWEEN 0 AND 1),
  CHECK (`bbox_width_ratio` BETWEEN 0 AND 1),
  CHECK (`bbox_height_ratio` BETWEEN 0 AND 1),
  CHECK ((bbox_x_ratio IS NULL AND bbox_y_ratio IS NULL AND bbox_width_ratio IS NULL AND bbox_height_ratio IS NULL) OR (bbox_x_ratio IS NOT NULL AND bbox_y_ratio IS NOT NULL AND bbox_width_ratio IS NOT NULL AND bbox_height_ratio IS NOT NULL AND bbox_width_ratio > 0 AND bbox_height_ratio > 0 AND bbox_x_ratio + bbox_width_ratio <= 1 AND bbox_y_ratio + bbox_height_ratio <= 1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `image_analysis_result` (
  `analysis_result_id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `photo_id` BIGINT UNSIGNED NOT NULL,
  `analysis_run_id` CHAR(36) NOT NULL,
  `detection_seq` INT NOT NULL,
  `model_name` VARCHAR(100) NOT NULL,
  `model_version` VARCHAR(50) NOT NULL,
  `predicted_damage_type_cd` VARCHAR(40) NOT NULL,
  `confidence_score` DECIMAL(5,4) NOT NULL,
  `bbox_x_ratio` DECIMAL(5,4) NOT NULL,
  `bbox_y_ratio` DECIMAL(5,4) NOT NULL,
  `bbox_width_ratio` DECIMAL(5,4) NOT NULL,
  `bbox_height_ratio` DECIMAL(5,4) NOT NULL,
  `analyzed_at` DATETIME(3) NOT NULL,
  PRIMARY KEY (`analysis_result_id`),
  CONSTRAINT v2_uk_image_analysis_result_0 UNIQUE (`photo_id`, `analysis_run_id`, `detection_seq`),
  CONSTRAINT v2_fk_image_analysis_result_photo_id FOREIGN KEY (`photo_id`) REFERENCES `condition_photo` (`photo_id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CHECK (`detection_seq` > 0),
  CHECK (`predicted_damage_type_cd` IN ('SCRATCH', 'DENT', 'CRACK', 'BROKEN', 'STAIN', 'DISCOLORATION', 'OTHER')),
  CHECK (`confidence_score` BETWEEN 0 AND 1),
  CHECK (`bbox_x_ratio` BETWEEN 0 AND 1),
  CHECK (`bbox_y_ratio` BETWEEN 0 AND 1),
  CHECK (`bbox_width_ratio` BETWEEN 0 AND 1),
  CHECK (`bbox_height_ratio` BETWEEN 0 AND 1),
  CHECK (bbox_width_ratio > 0 AND bbox_height_ratio > 0 AND bbox_x_ratio + bbox_width_ratio <= 1 AND bbox_y_ratio + bbox_height_ratio <= 1)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `return_inspection` (
  `inspection_id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `contract_id` BIGINT UNSIGNED NOT NULL,
  `intake_condition_id` BIGINT UNSIGNED NOT NULL,
  `return_condition_id` BIGINT UNSIGNED NOT NULL,
  `inspector_member_id` BIGINT UNSIGNED NOT NULL,
  `inspection_status_cd` VARCHAR(20) NOT NULL,
  `overall_result_cd` VARCHAR(20) NULL,
  `inspected_at` DATETIME(3) NULL,
  `inspection_note` VARCHAR(1000) NULL,
  `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`inspection_id`),
  CONSTRAINT v2_uk_return_inspection_0 UNIQUE (`contract_id`),
  CONSTRAINT v2_fk_return_inspection_contract_id FOREIGN KEY (`contract_id`) REFERENCES `rental_contract` (`contract_id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT v2_fk_return_inspection_intake_condition_id FOREIGN KEY (`intake_condition_id`) REFERENCES `condition_record` (`condition_id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT v2_fk_return_inspection_return_condition_id FOREIGN KEY (`return_condition_id`) REFERENCES `condition_record` (`condition_id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT v2_fk_return_inspection_inspector_member_id FOREIGN KEY (`inspector_member_id`) REFERENCES `member` (`member_id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CHECK (`inspection_status_cd` IN ('DRAFT', 'IN_PROGRESS', 'COMPLETED')),
  CHECK (`overall_result_cd` IN ('NO_NEW_DAMAGE', 'NEW_DAMAGE', 'REVIEW_REQUIRED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `damage_assessment` (
  `assessment_id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `inspection_id` BIGINT UNSIGNED NOT NULL,
  `return_damage_id` BIGINT UNSIGNED NOT NULL,
  `intake_damage_id` BIGINT UNSIGNED NULL,
  `assessment_result_cd` VARCHAR(20) NOT NULL,
  `assessment_reason` VARCHAR(1000) NULL,
  `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`assessment_id`),
  CONSTRAINT v2_uk_damage_assessment_0 UNIQUE (`inspection_id`, `return_damage_id`),
  CONSTRAINT v2_fk_damage_assessment_inspection_id FOREIGN KEY (`inspection_id`) REFERENCES `return_inspection` (`inspection_id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT v2_fk_damage_assessment_return_damage_id FOREIGN KEY (`return_damage_id`) REFERENCES `damage_observation` (`damage_id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT v2_fk_damage_assessment_intake_damage_id FOREIGN KEY (`intake_damage_id`) REFERENCES `damage_observation` (`damage_id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CHECK (`assessment_result_cd` IN ('EXISTING', 'NEW', 'HOLD')),
  CHECK ((assessment_result_cd = 'EXISTING' AND intake_damage_id IS NOT NULL) OR (assessment_result_cd IN ('NEW', 'HOLD') AND intake_damage_id IS NULL))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE member_credential (member_id BIGINT UNSIGNED PRIMARY KEY, password_hash VARCHAR(100) NOT NULL, status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE', CONSTRAINT v2_fk_credential_member FOREIGN KEY (member_id) REFERENCES `member`(member_id), CHECK (status IN ('ACTIVE','SUSPENDED','WITHDRAWN')));
INSERT INTO common_code_group (code_group_id,code_group_name,use_yn) VALUES ('MEMBER_ROLE','MEMBER_ROLE','Y');
INSERT INTO common_code (code_group_id,code_value,code_name,display_order,use_yn) VALUES ('MEMBER_ROLE','CUSTOMER','CUSTOMER',1,'Y');
INSERT INTO common_code (code_group_id,code_value,code_name,display_order,use_yn) VALUES ('MEMBER_ROLE','INSPECTOR','INSPECTOR',2,'Y');
INSERT INTO common_code (code_group_id,code_value,code_name,display_order,use_yn) VALUES ('MEMBER_ROLE','ADMIN','ADMIN',3,'Y');
INSERT INTO common_code_group (code_group_id,code_group_name,use_yn) VALUES ('BUSINESS_STATUS','BUSINESS_STATUS','Y');
INSERT INTO common_code (code_group_id,code_value,code_name,display_order,use_yn) VALUES ('BUSINESS_STATUS','ACTIVE','ACTIVE',1,'Y');
INSERT INTO common_code (code_group_id,code_value,code_name,display_order,use_yn) VALUES ('BUSINESS_STATUS','SUSPENDED','SUSPENDED',2,'Y');
INSERT INTO common_code (code_group_id,code_value,code_name,display_order,use_yn) VALUES ('BUSINESS_STATUS','CLOSED','CLOSED',3,'Y');
INSERT INTO common_code_group (code_group_id,code_group_name,use_yn) VALUES ('CATEGORY','CATEGORY','Y');
INSERT INTO common_code (code_group_id,code_value,code_name,display_order,use_yn) VALUES ('CATEGORY','CAMERA','CAMERA',1,'Y');
INSERT INTO common_code (code_group_id,code_value,code_name,display_order,use_yn) VALUES ('CATEGORY','COMPUTER','COMPUTER',2,'Y');
INSERT INTO common_code (code_group_id,code_value,code_name,display_order,use_yn) VALUES ('CATEGORY','HOME_APPLIANCE','HOME_APPLIANCE',3,'Y');
INSERT INTO common_code (code_group_id,code_value,code_name,display_order,use_yn) VALUES ('CATEGORY','SPORTS','SPORTS',4,'Y');
INSERT INTO common_code_group (code_group_id,code_group_name,use_yn) VALUES ('ITEM_STATUS','ITEM_STATUS','Y');
INSERT INTO common_code (code_group_id,code_value,code_name,display_order,use_yn) VALUES ('ITEM_STATUS','AVAILABLE','AVAILABLE',1,'Y');
INSERT INTO common_code (code_group_id,code_value,code_name,display_order,use_yn) VALUES ('ITEM_STATUS','RENTED','RENTED',2,'Y');
INSERT INTO common_code (code_group_id,code_value,code_name,display_order,use_yn) VALUES ('ITEM_STATUS','INSPECTION','INSPECTION',3,'Y');
INSERT INTO common_code (code_group_id,code_value,code_name,display_order,use_yn) VALUES ('ITEM_STATUS','REPAIR','REPAIR',4,'Y');
INSERT INTO common_code (code_group_id,code_value,code_name,display_order,use_yn) VALUES ('ITEM_STATUS','INACTIVE','INACTIVE',5,'Y');
INSERT INTO common_code_group (code_group_id,code_group_name,use_yn) VALUES ('CONTRACT_STATUS','CONTRACT_STATUS','Y');
INSERT INTO common_code (code_group_id,code_value,code_name,display_order,use_yn) VALUES ('CONTRACT_STATUS','REQUESTED','REQUESTED',1,'Y');
INSERT INTO common_code (code_group_id,code_value,code_name,display_order,use_yn) VALUES ('CONTRACT_STATUS','RENTED','RENTED',2,'Y');
INSERT INTO common_code (code_group_id,code_value,code_name,display_order,use_yn) VALUES ('CONTRACT_STATUS','RETURNED','RETURNED',3,'Y');
INSERT INTO common_code (code_group_id,code_value,code_name,display_order,use_yn) VALUES ('CONTRACT_STATUS','INSPECTED','INSPECTED',4,'Y');
INSERT INTO common_code (code_group_id,code_value,code_name,display_order,use_yn) VALUES ('CONTRACT_STATUS','CANCELED','CANCELED',5,'Y');
INSERT INTO common_code_group (code_group_id,code_group_name,use_yn) VALUES ('RECORD_STAGE','RECORD_STAGE','Y');
INSERT INTO common_code (code_group_id,code_value,code_name,display_order,use_yn) VALUES ('RECORD_STAGE','INTAKE','INTAKE',1,'Y');
INSERT INTO common_code (code_group_id,code_value,code_name,display_order,use_yn) VALUES ('RECORD_STAGE','RETURN','RETURN',2,'Y');
INSERT INTO common_code_group (code_group_id,code_group_name,use_yn) VALUES ('INSPECTION_STATUS','INSPECTION_STATUS','Y');
INSERT INTO common_code (code_group_id,code_value,code_name,display_order,use_yn) VALUES ('INSPECTION_STATUS','DRAFT','DRAFT',1,'Y');
INSERT INTO common_code (code_group_id,code_value,code_name,display_order,use_yn) VALUES ('INSPECTION_STATUS','IN_PROGRESS','IN_PROGRESS',2,'Y');
INSERT INTO common_code (code_group_id,code_value,code_name,display_order,use_yn) VALUES ('INSPECTION_STATUS','COMPLETED','COMPLETED',3,'Y');
INSERT INTO common_code_group (code_group_id,code_group_name,use_yn) VALUES ('OVERALL_RESULT','OVERALL_RESULT','Y');
INSERT INTO common_code (code_group_id,code_value,code_name,display_order,use_yn) VALUES ('OVERALL_RESULT','NO_NEW_DAMAGE','NO_NEW_DAMAGE',1,'Y');
INSERT INTO common_code (code_group_id,code_value,code_name,display_order,use_yn) VALUES ('OVERALL_RESULT','NEW_DAMAGE','NEW_DAMAGE',2,'Y');
INSERT INTO common_code (code_group_id,code_value,code_name,display_order,use_yn) VALUES ('OVERALL_RESULT','REVIEW_REQUIRED','REVIEW_REQUIRED',3,'Y');
INSERT INTO common_code_group (code_group_id,code_group_name,use_yn) VALUES ('ASSESSMENT_RESULT','ASSESSMENT_RESULT','Y');
INSERT INTO common_code (code_group_id,code_value,code_name,display_order,use_yn) VALUES ('ASSESSMENT_RESULT','EXISTING','EXISTING',1,'Y');
INSERT INTO common_code (code_group_id,code_value,code_name,display_order,use_yn) VALUES ('ASSESSMENT_RESULT','NEW','NEW',2,'Y');
INSERT INTO common_code (code_group_id,code_value,code_name,display_order,use_yn) VALUES ('ASSESSMENT_RESULT','HOLD','HOLD',3,'Y');
INSERT INTO common_code_group (code_group_id,code_group_name,use_yn) VALUES ('DAMAGE_TYPE','DAMAGE_TYPE','Y');
INSERT INTO common_code (code_group_id,code_value,code_name,display_order,use_yn) VALUES ('DAMAGE_TYPE','SCRATCH','SCRATCH',1,'Y');
INSERT INTO common_code (code_group_id,code_value,code_name,display_order,use_yn) VALUES ('DAMAGE_TYPE','DENT','DENT',2,'Y');
INSERT INTO common_code (code_group_id,code_value,code_name,display_order,use_yn) VALUES ('DAMAGE_TYPE','CRACK','CRACK',3,'Y');
INSERT INTO common_code (code_group_id,code_value,code_name,display_order,use_yn) VALUES ('DAMAGE_TYPE','BROKEN','BROKEN',4,'Y');
INSERT INTO common_code (code_group_id,code_value,code_name,display_order,use_yn) VALUES ('DAMAGE_TYPE','STAIN','STAIN',5,'Y');
INSERT INTO common_code (code_group_id,code_value,code_name,display_order,use_yn) VALUES ('DAMAGE_TYPE','DISCOLORATION','DISCOLORATION',6,'Y');
INSERT INTO common_code (code_group_id,code_value,code_name,display_order,use_yn) VALUES ('DAMAGE_TYPE','OTHER','OTHER',7,'Y');
INSERT INTO common_code_group (code_group_id,code_group_name,use_yn) VALUES ('DAMAGE_LOCATION','DAMAGE_LOCATION','Y');
INSERT INTO common_code (code_group_id,code_value,code_name,display_order,use_yn) VALUES ('DAMAGE_LOCATION','FRONT','FRONT',1,'Y');
INSERT INTO common_code (code_group_id,code_value,code_name,display_order,use_yn) VALUES ('DAMAGE_LOCATION','BACK','BACK',2,'Y');
INSERT INTO common_code (code_group_id,code_value,code_name,display_order,use_yn) VALUES ('DAMAGE_LOCATION','LEFT','LEFT',3,'Y');
INSERT INTO common_code (code_group_id,code_value,code_name,display_order,use_yn) VALUES ('DAMAGE_LOCATION','RIGHT','RIGHT',4,'Y');
INSERT INTO common_code (code_group_id,code_value,code_name,display_order,use_yn) VALUES ('DAMAGE_LOCATION','TOP','TOP',5,'Y');
INSERT INTO common_code (code_group_id,code_value,code_name,display_order,use_yn) VALUES ('DAMAGE_LOCATION','BOTTOM','BOTTOM',6,'Y');
INSERT INTO common_code (code_group_id,code_value,code_name,display_order,use_yn) VALUES ('DAMAGE_LOCATION','EDGE','EDGE',7,'Y');
INSERT INTO common_code (code_group_id,code_value,code_name,display_order,use_yn) VALUES ('DAMAGE_LOCATION','SCREEN','SCREEN',8,'Y');
INSERT INTO common_code (code_group_id,code_value,code_name,display_order,use_yn) VALUES ('DAMAGE_LOCATION','LENS','LENS',9,'Y');
INSERT INTO common_code (code_group_id,code_value,code_name,display_order,use_yn) VALUES ('DAMAGE_LOCATION','PORT','PORT',10,'Y');
INSERT INTO common_code (code_group_id,code_value,code_name,display_order,use_yn) VALUES ('DAMAGE_LOCATION','OTHER','OTHER',11,'Y');
INSERT INTO common_code_group (code_group_id,code_group_name,use_yn) VALUES ('SEVERITY','SEVERITY','Y');
INSERT INTO common_code (code_group_id,code_value,code_name,display_order,use_yn) VALUES ('SEVERITY','MINOR','MINOR',1,'Y');
INSERT INTO common_code (code_group_id,code_value,code_name,display_order,use_yn) VALUES ('SEVERITY','MODERATE','MODERATE',2,'Y');
INSERT INTO common_code (code_group_id,code_value,code_name,display_order,use_yn) VALUES ('SEVERITY','SEVERE','SEVERE',3,'Y');
INSERT INTO common_code (code_group_id,code_value,code_name,display_order,use_yn) VALUES ('SEVERITY','UNKNOWN','UNKNOWN',4,'Y');
INSERT INTO common_code_group (code_group_id,code_group_name,use_yn) VALUES ('CAPTURE_ANGLE','CAPTURE_ANGLE','Y');
INSERT INTO common_code (code_group_id,code_value,code_name,display_order,use_yn) VALUES ('CAPTURE_ANGLE','FRONT','FRONT',1,'Y');
INSERT INTO common_code (code_group_id,code_value,code_name,display_order,use_yn) VALUES ('CAPTURE_ANGLE','BACK','BACK',2,'Y');
INSERT INTO common_code (code_group_id,code_value,code_name,display_order,use_yn) VALUES ('CAPTURE_ANGLE','LEFT','LEFT',3,'Y');
INSERT INTO common_code (code_group_id,code_value,code_name,display_order,use_yn) VALUES ('CAPTURE_ANGLE','RIGHT','RIGHT',4,'Y');
INSERT INTO common_code (code_group_id,code_value,code_name,display_order,use_yn) VALUES ('CAPTURE_ANGLE','TOP','TOP',5,'Y');
INSERT INTO common_code (code_group_id,code_value,code_name,display_order,use_yn) VALUES ('CAPTURE_ANGLE','BOTTOM','BOTTOM',6,'Y');
INSERT INTO common_code (code_group_id,code_value,code_name,display_order,use_yn) VALUES ('CAPTURE_ANGLE','DETAIL','DETAIL',7,'Y');
INSERT INTO common_code (code_group_id,code_value,code_name,display_order,use_yn) VALUES ('CAPTURE_ANGLE','OTHER','OTHER',8,'Y');
