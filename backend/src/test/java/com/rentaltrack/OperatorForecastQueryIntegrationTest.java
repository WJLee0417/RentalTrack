package com.rentaltrack;

import com.rentaltrack.forecast.application.OperatorForecastQueryService;
import com.rentaltrack.forecast.domain.ModelRunStatus;
import com.rentaltrack.forecast.domain.ModelRunType;
import com.rentaltrack.forecast.domain.RecommendationStatus;
import com.rentaltrack.forecast.domain.RiskLevel;
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
class OperatorForecastQueryIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private OperatorForecastQueryService operatorForecastQueryService;

    private Long locationId;
    private Long categoryId;
    private Long baselineRunId;
    private Long ttmRunId;
    private Long baselineForecastId;
    private Long ttmForecastId;

    @BeforeEach
    void setUp() {
        cleanDatabase();

        locationId = insertLocation();
        categoryId = insertCategory();

        baselineRunId = insertModelRun(
                "historical_average",
                "weekday-hour-v1",
                "BASELINE",
                new BigDecimal("1.200000"),
                new BigDecimal("1.500000")
        );
        ttmRunId = insertModelRun(
                "granite_ttm_r3",
                "ibm-granite/granite-timeseries-ttm-r3",
                "TTM_ZERO_SHOT",
                new BigDecimal("1.000000"),
                new BigDecimal("1.300000")
        );

        baselineForecastId = insertForecast(
                baselineRunId,
                new BigDecimal("30.000000")
        );
        ttmForecastId = insertForecast(
                ttmRunId,
                new BigDecimal("25.500000")
        );

        insertRecommendation(
                baselineForecastId,
                20,
                1,
                11,
                "HIGH",
                "OPEN"
        );
        insertRecommendation(
                ttmForecastId,
                30,
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
    void modelRunsCanBeFilteredByTypeStatusAndDatasetVersion() {
        var result = operatorForecastQueryService.getModelRuns(
                ModelRunType.TTM_ZERO_SHOT,
                ModelRunStatus.SUCCEEDED,
                "synthetic-v1",
                0,
                20
        );

        assertThat(result.totalElements()).isEqualTo(1);
        assertThat(result.content()).singleElement().satisfies(run -> {
            assertThat(run.id()).isEqualTo(ttmRunId);
            assertThat(run.modelName()).isEqualTo("granite_ttm_r3");
            assertThat(run.mae()).isEqualByComparingTo("1.000000");
        });
    }

    @Test
    void forecastsCanBeFilteredByModelRunAndIncludeLocationAndCategory() {
        var result = operatorForecastQueryService.getForecasts(
                ttmRunId,
                locationId,
                categoryId,
                0,
                20
        );

        assertThat(result.totalElements()).isEqualTo(1);
        assertThat(result.content()).singleElement().satisfies(forecast -> {
            assertThat(forecast.id()).isEqualTo(ttmForecastId);
            assertThat(forecast.modelRunId()).isEqualTo(ttmRunId);
            assertThat(forecast.rentalLocationName()).isEqualTo("Forecast Station");
            assertThat(forecast.productCategoryName()).isEqualTo("Forecast Category");
            assertThat(forecast.predictedDemand()).isEqualByComparingTo("25.500000");
        });
    }

    @Test
    void recommendationsCanBeFilteredByRiskAndStatus() {
        var result = operatorForecastQueryService.getRecommendations(
                RecommendationStatus.OPEN,
                RiskLevel.HIGH,
                locationId,
                categoryId,
                0,
                20
        );

        assertThat(result.totalElements()).isEqualTo(1);
        assertThat(result.content()).singleElement().satisfies(recommendation -> {
            assertThat(recommendation.demandForecastId()).isEqualTo(baselineForecastId);
            assertThat(recommendation.riskLevel()).isEqualTo("HIGH");
            assertThat(recommendation.recommendedAdditionalQuantity()).isEqualTo(11);
            assertThat(recommendation.status()).isEqualTo("OPEN");
        });
    }

    @Test
    void detailQueriesReturnPersistedModelForecastAndRecommendation() {
        var modelRun = operatorForecastQueryService.getModelRun(ttmRunId);
        var forecast = operatorForecastQueryService.getForecast(ttmForecastId);

        Long recommendationId = jdbcTemplate.queryForObject(
                "SELECT id FROM inventory_recommendation WHERE demand_forecast_id = ?",
                Long.class,
                ttmForecastId
        );
        var recommendation =
                operatorForecastQueryService.getRecommendation(recommendationId);

        assertThat(modelRun.modelName()).isEqualTo("granite_ttm_r3");
        assertThat(forecast.predictedDemand()).isEqualByComparingTo("25.500000");
        assertThat(recommendation.status()).isEqualTo("ACCEPTED");
    }

    private Long insertLocation() {
        jdbcTemplate.update(
                """
                INSERT INTO rental_location
                    (code, name, address, latitude, longitude, active)
                VALUES (?, 'Forecast Station', 'Seoul', 37.5000000, 127.0000000, TRUE)
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
                VALUES (?, 'Forecast Category', 'forecast integration test', TRUE)
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
            BigDecimal rmse
    ) {
        LocalDateTime now = LocalDateTime.now();

        jdbcTemplate.update(
                """
                INSERT INTO model_run (
                    model_name,
                    model_version,
                    run_type,
                    dataset_version,
                    train_from,
                    train_to,
                    evaluation_from,
                    evaluation_to,
                    mae,
                    rmse,
                    status,
                    started_at,
                    completed_at
                )
                VALUES (?, ?, ?, 'synthetic-v1', ?, ?, ?, ?, ?, ?, 'SUCCEEDED', ?, ?)
                """,
                modelName,
                modelVersion,
                runType,
                now.minusDays(10),
                now.minusDays(2),
                now.minusDays(1),
                now,
                mae,
                rmse,
                now.minusMinutes(1),
                now
        );

        return jdbcTemplate.queryForObject(
                "SELECT MAX(id) FROM model_run",
                Long.class
        );
    }

    private Long insertForecast(Long modelRunId, BigDecimal predictedDemand) {
        LocalDateTime start = LocalDateTime.now().plusHours(1);
        LocalDateTime end = start.plusHours(24);

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
                end,
                predictedDemand,
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
                VALUES (?, ?, ?, ?, ?, 'integration test', ?, ?)
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
