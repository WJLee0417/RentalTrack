ALTER TABLE member_account
    ADD COLUMN password_hash VARCHAR(100) NULL AFTER email;
