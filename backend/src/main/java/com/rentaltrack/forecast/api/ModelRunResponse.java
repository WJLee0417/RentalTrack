package com.rentaltrack.forecast.api;

import com.rentaltrack.forecast.domain.ModelRun;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ModelRunResponse(
        Long id,
        String modelName,
        String modelVersion,
        String runType,
        String datasetVersion,
        LocalDateTime trainFrom,
        LocalDateTime trainTo,
        LocalDateTime evaluationFrom,
        LocalDateTime evaluationTo,
        BigDecimal mae,
        BigDecimal rmse,
        String status,
        LocalDateTime startedAt,
        LocalDateTime completedAt
) {
    public static ModelRunResponse from(ModelRun modelRun) {
        return new ModelRunResponse(
                modelRun.getId(),
                modelRun.getModelName(),
                modelRun.getModelVersion(),
                modelRun.getRunType().name(),
                modelRun.getDatasetVersion(),
                modelRun.getTrainFrom(),
                modelRun.getTrainTo(),
                modelRun.getEvaluationFrom(),
                modelRun.getEvaluationTo(),
                modelRun.getMae(),
                modelRun.getRmse(),
                modelRun.getStatus().name(),
                modelRun.getStartedAt(),
                modelRun.getCompletedAt()
        );
    }
}
