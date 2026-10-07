package com.rentaltrack.forecast.api;

import com.rentaltrack.forecast.domain.DemandForecast;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record DemandForecastResponse(
        Long id,
        Long modelRunId,
        String modelName,
        String modelVersion,
        Long rentalLocationId,
        String rentalLocationName,
        Long productCategoryId,
        String productCategoryName,
        LocalDateTime forecastStartAt,
        LocalDateTime forecastEndAt,
        BigDecimal predictedDemand,
        LocalDateTime generatedAt
) {
    public static DemandForecastResponse from(DemandForecast forecast) {
        return new DemandForecastResponse(
                forecast.getId(),
                forecast.getModelRun().getId(),
                forecast.getModelRun().getModelName(),
                forecast.getModelRun().getModelVersion(),
                forecast.getRentalLocation().getId(),
                forecast.getRentalLocation().getName(),
                forecast.getProductCategory().getId(),
                forecast.getProductCategory().getName(),
                forecast.getForecastStartAt(),
                forecast.getForecastEndAt(),
                forecast.getPredictedDemand(),
                forecast.getGeneratedAt()
        );
    }
}
