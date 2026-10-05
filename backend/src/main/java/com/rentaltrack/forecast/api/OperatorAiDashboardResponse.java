package com.rentaltrack.forecast.api;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OperatorAiDashboardResponse(
        LocalDateTime generatedAt,
        String datasetVersion,
        LatestModel latestModel,
        ForecastSummary forecast,
        RecommendationSummary recommendations,
        List<ModelMetric> modelComparison,
        List<InventoryRecommendationResponse> priorityRecommendations
) {
    public record LatestModel(
            Long modelRunId,
            String modelName,
            String modelVersion,
            String runType,
            BigDecimal mae,
            BigDecimal rmse,
            LocalDateTime completedAt
    ) {
    }

    public record ForecastSummary(
            long seriesCount,
            BigDecimal predictedDemand
    ) {
    }

    public record RecommendationSummary(
            long openCount,
            long highRiskOpenCount,
            long mediumRiskOpenCount,
            long lowRiskOpenCount,
            long recommendedAdditionalQuantity
    ) {
    }

    public record ModelMetric(
            Long modelRunId,
            String modelName,
            String runType,
            BigDecimal mae,
            BigDecimal rmse
    ) {
    }
}
