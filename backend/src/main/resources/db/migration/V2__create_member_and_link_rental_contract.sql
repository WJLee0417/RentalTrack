CREATE TABLE member_account (
    id BIGINT NOT NULL AUTO_INCREMENT,
    email VARCHAR(255) NOT NULL,
    name VARCHAR(100) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),

    CONSTRAINT pk_member_account PRIMARY KEY (id),
    CONSTRAINT uk_member_account_email UNIQUE (email),
    CONSTRAINT ck_member_account_status
        CHECK (status IN ('ACTIVE', 'SUSPENDED', 'WITHDRAWN'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

ALTER TABLE rental_contract
    ADD CONSTRAINT fk_rental_contract_member
        FOREIGN KEY (member_id) REFERENCES member_account (id)
        ON UPDATE RESTRICT ON DELETE RESTRICT;
