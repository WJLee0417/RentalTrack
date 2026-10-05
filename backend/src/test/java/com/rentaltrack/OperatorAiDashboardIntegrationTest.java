package com.rentaltrack;

import com.rentaltrack.forecast.application.OperatorAiDashboardService;
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

@SpringBootTest
class OperatorAiDashboardIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private OperatorAiDashboardService dashboardService;

    private Long locationId;
    private Long categoryId;

    @BeforeEach
    void setUp() {
        cleanDatabase();

        locationId = insertLocation();
        categoryId = insertCategory();

        Long baselineRunId = insertModelRun(
                "historical_average",
                "weekday-hour-v1",
                "BASELINE",
                new BigDecimal("1.200000"),
                new BigDecimal("1.500000"),
                LocalDateTime.now().minusMinutes(2)
        );
        Long ttmRunId = insertModelRun(
                "granite_ttm_r3",
                "ibm-granite/granite-timeseries-ttm-r3",
                "TTM_ZERO_SHOT",
                new BigDecimal("1.000000"),
                new BigDecimal("1.300000"),
                LocalDateTime.now().minusMinutes(1)
        );

        insertForecast(baselineRunId, new BigDecimal("30.000000"));
        Long ttmForecastId = insertForecast(
                ttmRunId,
                new BigDecimal("25.500000")
        );

        insertRecommendation(
                ttmForecastId,
                10,
                1,
                17,
                "HIGH",
                "OPEN"
        );

        Long secondForecastId = insertForecast(
                ttmRunId,
                new BigDecimal("10.000000")
        );
        insertRecommendation(
                secondForecastId,
                12,
                1,
                0,
                "LOW",
                "ACCEPTED"
        );
    }

    @AfterEach
    void tearDown() {
        cleanDatabase();
    }

    @Test
    void dashboardAggregatesLatestModelForecastAndOpenRecommendations() {
        var response = dashboardService.getDashboard();

        assertThat(response.datasetVersion()).isEqualTo("synthetic-dashboard");
        assertThat(response.latestModel()).isNotNull();
        assertThat(response.latestModel().modelName()).isEqualTo("granite_ttm_r3");
        assertThat(response.latestModel().mae()).isEqualByComparingTo("1.000000");

        assertThat(response.modelComparison()).hasSize(2);

        assertThat(response.forecast().seriesCount()).isEqualTo(2);
        assertThat(response.forecast().predictedDemand())
                .isEqualByComparingTo("35.500000");

        assertThat(response.recommendations().openCount()).isEqualTo(1);
        assertThat(response.recommendations().highRiskOpenCount()).isEqualTo(1);
        assertThat(response.recommendations().lowRiskOpenCount()).isZero();
        assertThat(response.recommendations().recommendedAdditionalQuantity())
                .isEqualTo(17);

        assertThat(response.priorityRecommendations()).hasSize(1);
        assertThat(response.priorityRecommendations().getFirst().riskLevel())
                .isEqualTo("HIGH");
    }

    private Long insertLocation() {
        jdbcTemplate.update(
                """
                INSERT INTO rental_location
                    (code, name, address, latitude, longitude, active)
                VALUES (?, 'Dashboard Station', 'Seoul', 37.5000000, 127.0000000, TRUE)
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
                VALUES (?, 'Dashboard Category', 'dashboard test', TRUE)
                """,
                "CAT-" + shortId()
        );
        return jdbcTemplate.queryForObject(
                "SELECT MAX(id) FROM product_category",
                Long.class
        );
    }

    private Long insertModelRun(
            String modelName,
            String modelVersion,
            String runType,
            BigDecimal mae,
            BigDecimal rmse,
            LocalDateTime completedAt
    ) {
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
                VALUES (?, ?, ?, 'synthetic-dashboard', ?, ?, 'SUCCEEDED', ?, ?)
                """,
                modelName,
                modelVersion,
                runType,
                mae,
                rmse,
                completedAt.minusMinutes(1),
                completedAt
        );
        return jdbcTemplate.queryForObject(
                "SELECT MAX(id) FROM model_run",
                Long.class
        );
    }

    private Long insertForecast(Long modelRunId, BigDecimal demand) {
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
                demand,
                LocalDateTime.now()
        );
        return jdbcTemplate.queryForObject(
                "SELECT MAX(id) FROM demand_forecast",
                Long.class
        );
    }

    private void insertRecommendation(
            Long forecastId,
            int availableQuantity,
            int safetyStock,
            int additionalQuantity,
            String riskLevel,
            String status
    ) {
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
                VALUES (?, ?, ?, ?, ?, 'dashboard test', ?, ?)
                """,
                forecastId,
                availableQuantity,
                safetyStock,
                additionalQuantity,
                riskLevel,
                status,
                LocalDateTime.now()
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
