ALTER TABLE member_account
    ADD COLUMN role VARCHAR(20) NOT NULL DEFAULT 'USER' AFTER status;

ALTER TABLE member_account
    ADD CONSTRAINT ck_member_account_role
        CHECK (role IN ('USER', 'OPERATOR'));
