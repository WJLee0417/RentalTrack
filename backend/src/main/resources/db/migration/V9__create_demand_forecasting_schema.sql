CREATE TABLE model_run (
    id BIGINT NOT NULL AUTO_INCREMENT,
    model_name VARCHAR(100) NOT NULL,
    model_version VARCHAR(100) NOT NULL,
    run_type VARCHAR(30) NOT NULL,
    dataset_version VARCHAR(100) NOT NULL,
    train_from DATETIME(6) NULL,
    train_to DATETIME(6) NULL,
    evaluation_from DATETIME(6) NULL,
    evaluation_to DATETIME(6) NULL,
    mae DECIMAL(18, 6) NULL,
    rmse DECIMAL(18, 6) NULL,
    status VARCHAR(20) NOT NULL,
    started_at DATETIME(6) NOT NULL,
    completed_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

    CONSTRAINT pk_model_run PRIMARY KEY (id),
    CONSTRAINT ck_model_run_type
        CHECK (run_type IN ('BASELINE', 'TTM_ZERO_SHOT')),
    CONSTRAINT ck_model_run_status
        CHECK (status IN ('RUNNING', 'SUCCEEDED', 'FAILED')),
    CONSTRAINT ck_model_run_mae
        CHECK (mae IS NULL OR mae >= 0),
    CONSTRAINT ck_model_run_rmse
        CHECK (rmse IS NULL OR rmse >= 0),

    INDEX idx_model_run_model_started (model_name, started_at),
    INDEX idx_model_run_status_started (status, started_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE demand_forecast (
    id BIGINT NOT NULL AUTO_INCREMENT,
    model_run_id BIGINT NOT NULL,
    rental_location_id BIGINT NOT NULL,
    product_category_id BIGINT NOT NULL,
    forecast_start_at DATETIME(6) NOT NULL,
    forecast_end_at DATETIME(6) NOT NULL,
    predicted_demand DECIMAL(18, 6) NOT NULL,
    generated_at DATETIME(6) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

    CONSTRAINT pk_demand_forecast PRIMARY KEY (id),
    CONSTRAINT fk_demand_forecast_model_run
        FOREIGN KEY (model_run_id) REFERENCES model_run (id)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT fk_demand_forecast_location
        FOREIGN KEY (rental_location_id) REFERENCES rental_location (id)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT fk_demand_forecast_category
        FOREIGN KEY (product_category_id) REFERENCES product_category (id)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT ck_demand_forecast_window
        CHECK (forecast_end_at > forecast_start_at),
    CONSTRAINT ck_demand_forecast_value
        CHECK (predicted_demand >= 0),
    CONSTRAINT uk_demand_forecast_run_series_window
        UNIQUE (
            model_run_id,
            rental_location_id,
            product_category_id,
            forecast_start_at,
            forecast_end_at
        ),

    INDEX idx_demand_forecast_series_start
        (rental_location_id, product_category_id, forecast_start_at),
    INDEX idx_demand_forecast_generated
        (generated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE inventory_recommendation (
    id BIGINT NOT NULL AUTO_INCREMENT,
    demand_forecast_id BIGINT NOT NULL,
    available_quantity INT NOT NULL,
    safety_stock INT NOT NULL,
    recommended_additional_quantity INT NOT NULL,
    risk_level VARCHAR(20) NOT NULL,
    reason VARCHAR(500) NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    generated_at DATETIME(6) NOT NULL,
    decided_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),

    CONSTRAINT pk_inventory_recommendation PRIMARY KEY (id),
    CONSTRAINT uk_inventory_recommendation_forecast UNIQUE (demand_forecast_id),
    CONSTRAINT fk_inventory_recommendation_forecast
        FOREIGN KEY (demand_forecast_id) REFERENCES demand_forecast (id)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT ck_inventory_recommendation_available
        CHECK (available_quantity >= 0),
    CONSTRAINT ck_inventory_recommendation_safety
        CHECK (safety_stock >= 0),
    CONSTRAINT ck_inventory_recommendation_additional
        CHECK (recommended_additional_quantity >= 0),
    CONSTRAINT ck_inventory_recommendation_risk
        CHECK (risk_level IN ('LOW', 'MEDIUM', 'HIGH')),
    CONSTRAINT ck_inventory_recommendation_status
        CHECK (status IN ('OPEN', 'ACCEPTED', 'DISMISSED')),

    INDEX idx_inventory_recommendation_status_risk
        (status, risk_level, generated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
