package com.rentaltrack.forecast.api;

import com.rentaltrack.access.application.AccessControlService;
import com.rentaltrack.forecast.application.OperatorAiDashboardService;
import com.rentaltrack.forecast.application.OperatorForecastQueryService;
import com.rentaltrack.forecast.application.OperatorRecommendationDecisionService;
import com.rentaltrack.forecast.domain.ModelRunStatus;
import com.rentaltrack.forecast.domain.ModelRunType;
import com.rentaltrack.forecast.domain.RecommendationStatus;
import com.rentaltrack.forecast.domain.RiskLevel;
import com.rentaltrack.global.api.PageResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Locale;

@Validated
@RestController
@RequestMapping("/api/v1/operator/ai")
public class OperatorForecastController {

    private final OperatorAiDashboardService operatorAiDashboardService;
    private final OperatorForecastQueryService operatorForecastQueryService;
    private final OperatorRecommendationDecisionService operatorRecommendationDecisionService;
    private final AccessControlService accessControlService;

    public OperatorForecastController(
            OperatorAiDashboardService operatorAiDashboardService,
            OperatorForecastQueryService operatorForecastQueryService,
            OperatorRecommendationDecisionService operatorRecommendationDecisionService,
            AccessControlService accessControlService
    ) {
        this.operatorAiDashboardService = operatorAiDashboardService;
        this.operatorForecastQueryService = operatorForecastQueryService;
        this.operatorRecommendationDecisionService = operatorRecommendationDecisionService;
        this.accessControlService = accessControlService;
    }

    @GetMapping("/dashboard")
    public OperatorAiDashboardResponse getDashboard(
            @RequestHeader(name = "X-Actor-Id", required = false) @Positive Long actorId
    ) {
        accessControlService.requireOperator(actorId);
        return operatorAiDashboardService.getDashboard();
    }

    @GetMapping("/model-runs")
    public PageResponse<ModelRunResponse> getModelRuns(
            @RequestHeader(name = "X-Actor-Id", required = false) @Positive Long actorId,
            @RequestParam(required = false) String runType,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String datasetVersion,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        accessControlService.requireOperator(actorId);

        return operatorForecastQueryService.getModelRuns(
                parseEnum(runType, ModelRunType.class, "runType"),
                parseEnum(status, ModelRunStatus.class, "status"),
                datasetVersion,
                page,
                size
        );
    }

    @GetMapping("/model-runs/{modelRunId}")
    public ModelRunResponse getModelRun(
            @RequestHeader(name = "X-Actor-Id", required = false) @Positive Long actorId,
            @PathVariable @Positive Long modelRunId
    ) {
        accessControlService.requireOperator(actorId);
        return operatorForecastQueryService.getModelRun(modelRunId);
    }

    @GetMapping("/forecasts")
    public PageResponse<DemandForecastResponse> getForecasts(
            @RequestHeader(name = "X-Actor-Id", required = false) @Positive Long actorId,
            @RequestParam(required = false) @Positive Long modelRunId,
            @RequestParam(required = false) @Positive Long locationId,
            @RequestParam(required = false) @Positive Long categoryId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        accessControlService.requireOperator(actorId);

        return operatorForecastQueryService.getForecasts(
                modelRunId,
                locationId,
                categoryId,
                page,
                size
        );
    }

    @GetMapping("/forecasts/{forecastId}")
    public DemandForecastResponse getForecast(
            @RequestHeader(name = "X-Actor-Id", required = false) @Positive Long actorId,
            @PathVariable @Positive Long forecastId
    ) {
        accessControlService.requireOperator(actorId);
        return operatorForecastQueryService.getForecast(forecastId);
    }

    @GetMapping("/recommendations")
    public PageResponse<InventoryRecommendationResponse> getRecommendations(
            @RequestHeader(name = "X-Actor-Id", required = false) @Positive Long actorId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String riskLevel,
            @RequestParam(required = false) @Positive Long locationId,
            @RequestParam(required = false) @Positive Long categoryId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        accessControlService.requireOperator(actorId);

        return operatorForecastQueryService.getRecommendations(
                parseEnum(status, RecommendationStatus.class, "status"),
                parseEnum(riskLevel, RiskLevel.class, "riskLevel"),
                locationId,
                categoryId,
                page,
                size
        );
    }

    @GetMapping("/recommendations/{recommendationId}")
    public InventoryRecommendationResponse getRecommendation(
            @RequestHeader(name = "X-Actor-Id", required = false) @Positive Long actorId,
            @PathVariable @Positive Long recommendationId
    ) {
        accessControlService.requireOperator(actorId);
        return operatorForecastQueryService.getRecommendation(recommendationId);
    }

    @PatchMapping("/recommendations/{recommendationId}/decision")
    public InventoryRecommendationResponse decideRecommendation(
            @RequestHeader(name = "X-Actor-Id", required = false) @Positive Long actorId,
            @PathVariable @Positive Long recommendationId,
            @Valid @RequestBody RecommendationDecisionRequest request
    ) {
        accessControlService.requireOperator(actorId);
        return operatorRecommendationDecisionService.decide(
                recommendationId,
                request.decision()
        );
    }

    private <E extends Enum<E>> E parseEnum(
            String value,
            Class<E> enumType,
            String parameterName
    ) {
        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return Enum.valueOf(
                    enumType,
                    value.trim().toUpperCase(Locale.ROOT)
            );
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(
                    "Unknown " + parameterName + ": " + value
            );
        }
    }
}
