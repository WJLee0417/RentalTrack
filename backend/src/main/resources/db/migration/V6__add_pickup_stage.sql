ALTER TABLE rental_contract
    ADD COLUMN picked_up_at DATETIME(6) NULL AFTER approved_at;

ALTER TABLE rental_contract
    DROP CHECK ck_rental_contract_status;

ALTER TABLE rental_contract
    ADD CONSTRAINT ck_rental_contract_status
        CHECK (status IN (
            'REQUESTED',
            'APPROVED',
            'PICKED_UP',
            'IN_USE',
            'RETURNED',
            'INSPECTING',
            'COMPLETED',
            'REJECTED',
            'CANCELED'
        ));

ALTER TABLE rental_contract
    ADD CONSTRAINT ck_rental_contract_pickup_after_approval
        CHECK (
            picked_up_at IS NULL
            OR approved_at IS NULL
            OR picked_up_at >= approved_at
        ),
    ADD CONSTRAINT ck_rental_contract_rental_after_pickup
        CHECK (
            rented_at IS NULL
            OR picked_up_at IS NULL
            OR rented_at >= picked_up_at
        );
