package com.rentaltrack.forecast.api;

import com.rentaltrack.forecast.domain.InventoryRecommendation;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record InventoryRecommendationResponse(
        Long id,
        Long demandForecastId,
        Long modelRunId,
        String modelName,
        Long rentalLocationId,
        String rentalLocationName,
        Long productCategoryId,
        String productCategoryName,
        LocalDateTime forecastStartAt,
        LocalDateTime forecastEndAt,
        BigDecimal predictedDemand,
        int availableQuantity,
        int safetyStock,
        int recommendedAdditionalQuantity,
        String riskLevel,
        String reason,
        String status,
        LocalDateTime generatedAt,
        LocalDateTime decidedAt
) {
    public static InventoryRecommendationResponse from(InventoryRecommendation recommendation) {
        var forecast = recommendation.getDemandForecast();

        return new InventoryRecommendationResponse(
                recommendation.getId(),
                forecast.getId(),
                forecast.getModelRun().getId(),
                forecast.getModelRun().getModelName(),
                forecast.getRentalLocation().getId(),
                forecast.getRentalLocation().getName(),
                forecast.getProductCategory().getId(),
                forecast.getProductCategory().getName(),
                forecast.getForecastStartAt(),
                forecast.getForecastEndAt(),
                forecast.getPredictedDemand(),
                recommendation.getAvailableQuantity(),
                recommendation.getSafetyStock(),
                recommendation.getRecommendedAdditionalQuantity(),
                recommendation.getRiskLevel().name(),
                recommendation.getReason(),
                recommendation.getStatus().name(),
                recommendation.getGeneratedAt(),
                recommendation.getDecidedAt()
        );
    }
}
