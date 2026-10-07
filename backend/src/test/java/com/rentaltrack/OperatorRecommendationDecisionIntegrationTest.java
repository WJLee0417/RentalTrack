package com.rentaltrack;

import com.rentaltrack.forecast.application.OperatorRecommendationDecisionService;
import com.rentaltrack.forecast.domain.RecommendationStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class OperatorRecommendationDecisionIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private OperatorRecommendationDecisionService decisionService;

    private Long recommendationId;

    @BeforeEach
    void setUp() {
        cleanDatabase();

        Long locationId = insertLocation();
        Long categoryId = insertCategory();
        Long modelRunId = insertModelRun();
        Long forecastId = insertForecast(modelRunId, locationId, categoryId);

        recommendationId = insertRecommendation(forecastId);
    }

    @AfterEach
    void tearDown() {
        cleanDatabase();
    }

    @Test
    void operatorCanAcceptOpenRecommendation() {
        var response = decisionService.decide(
                recommendationId,
                RecommendationStatus.ACCEPTED
        );

        assertThat(response.status()).isEqualTo("ACCEPTED");
        assertThat(response.decidedAt()).isNotNull();

        String status = jdbcTemplate.queryForObject(
                "SELECT status FROM inventory_recommendation WHERE id = ?",
                String.class,
                recommendationId
        );
        LocalDateTime decidedAt = jdbcTemplate.queryForObject(
                "SELECT decided_at FROM inventory_recommendation WHERE id = ?",
                LocalDateTime.class,
                recommendationId
        );

        assertThat(status).isEqualTo("ACCEPTED");
        assertThat(decidedAt).isNotNull();
    }

    @Test
    void operatorCanDismissOpenRecommendation() {
        var response = decisionService.decide(
                recommendationId,
                RecommendationStatus.DISMISSED
        );

        assertThat(response.status()).isEqualTo("DISMISSED");
        assertThat(response.decidedAt()).isNotNull();
    }

    @Test
    void finalizedRecommendationCannotBeDecidedAgain() {
        decisionService.decide(
                recommendationId,
                RecommendationStatus.ACCEPTED
        );

        assertThatThrownBy(() ->
                decisionService.decide(
                        recommendationId,
                        RecommendationStatus.DISMISSED
                )
        ).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void openCannotBeUsedAsOperatorDecision() {
        assertThatThrownBy(() ->
                decisionService.decide(
                        recommendationId,
                        RecommendationStatus.OPEN
                )
        ).isInstanceOf(IllegalArgumentException.class);
    }

    private Long insertLocation() {
        jdbcTemplate.update(
                """
                INSERT INTO rental_location
                    (code, name, address, latitude, longitude, active)
                VALUES (?, 'Decision Station', 'Seoul', 37.5000000, 127.0000000, TRUE)
                """,
                "LOC-" + shortId()
        );

        return jdbcTemplate.queryForObject(
                "SELECT MAX(id) FROM rental_location",
                Long.class
        );
    }

    private Long insertCategory() {
        jdbcTemplate.update(
                """
                INSERT INTO product_category (code, name, description, active)
                VALUES (?, 'Decision Category', 'decision integration test', TRUE)
                """,
                "CAT-" + shortId()
        );

        return jdbcTemplate.queryForObject(
                "SELECT MAX(id) FROM product_category",
                Long.class
        );
    }

    private Long insertModelRun() {
        LocalDateTime now = LocalDateTime.now();

        jdbcTemplate.update(
                """
                INSERT INTO model_run (
                    model_name,
                    model_version,
                    run_type,
                    dataset_version,
                    mae,
                    rmse,
                    status,
                    started_at,
                    completed_at
                )
                VALUES (
                    'granite_ttm_r3',
                    'ibm-granite/granite-timeseries-ttm-r3',
                    'TTM_ZERO_SHOT',
                    'synthetic-decision-test',
                    1.000000,
                    1.300000,
                    'SUCCEEDED',
                    ?,
                    ?
                )
                """,
                now.minusMinutes(1),
                now
        );

        return jdbcTemplate.queryForObject(
                "SELECT MAX(id) FROM model_run",
                Long.class
        );
    }

    private Long insertForecast(
            Long modelRunId,
            Long locationId,
            Long categoryId
    ) {
        LocalDateTime start = LocalDateTime.now().plusHours(1);

        jdbcTemplate.update(
                """
                INSERT INTO demand_forecast (
                    model_run_id,
                    rental_location_id,
                    product_category_id,
                    forecast_start_at,
                    forecast_end_at,
                    predicted_demand,
                    generated_at
                )
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """,
                modelRunId,
                locationId,
                categoryId,
                start,
                start.plusHours(24),
                new BigDecimal("25.500000"),
                LocalDateTime.now()
        );

        return jdbcTemplate.queryForObject(
                "SELECT MAX(id) FROM demand_forecast",
                Long.class
        );
    }

    private Long insertRecommendation(Long forecastId) {
        jdbcTemplate.update(
                """
                INSERT INTO inventory_recommendation (
                    demand_forecast_id,
                    available_quantity,
                    safety_stock,
                    recommended_additional_quantity,
                    risk_level,
                    reason,
                    status,
                    generated_at
                )
                VALUES (?, 10, 1, 17, 'HIGH', 'decision integration test', 'OPEN', ?)
                """,
                forecastId,
                LocalDateTime.now()
        );

        return jdbcTemplate.queryForObject(
                "SELECT MAX(id) FROM inventory_recommendation",
                Long.class
        );
    }

    private String shortId() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }

    private void cleanDatabase() {
        jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 0");
        jdbcTemplate.execute("TRUNCATE TABLE inventory_recommendation");
        jdbcTemplate.execute("TRUNCATE TABLE demand_forecast");
        jdbcTemplate.execute("TRUNCATE TABLE model_run");
        jdbcTemplate.execute("TRUNCATE TABLE return_inspection");
        jdbcTemplate.execute("TRUNCATE TABLE damage_record");
        jdbcTemplate.execute("TRUNCATE TABLE condition_image");
        jdbcTemplate.execute("TRUNCATE TABLE condition_record");
        jdbcTemplate.execute("TRUNCATE TABLE rental_contract");
        jdbcTemplate.execute("TRUNCATE TABLE rental_product");
        jdbcTemplate.execute("TRUNCATE TABLE product_category");
        jdbcTemplate.execute("TRUNCATE TABLE rental_location");
        jdbcTemplate.execute("TRUNCATE TABLE member_account");
        jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 1");
    }
}
