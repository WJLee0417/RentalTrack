package com.rentaltrack.forecast.application;

import com.rentaltrack.forecast.api.InventoryRecommendationResponse;
import com.rentaltrack.forecast.api.OperatorAiDashboardResponse;
import com.rentaltrack.forecast.domain.InventoryRecommendation;
import com.rentaltrack.forecast.domain.ModelRun;
import com.rentaltrack.forecast.domain.ModelRunStatus;
import com.rentaltrack.forecast.domain.RecommendationStatus;
import com.rentaltrack.forecast.domain.RiskLevel;
import com.rentaltrack.forecast.repository.DemandForecastRepository;
import com.rentaltrack.forecast.repository.InventoryRecommendationRepository;
import com.rentaltrack.forecast.repository.ModelRunRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class OperatorAiDashboardService {

    private static final int PRIORITY_LIMIT = 5;
    private static final int PRIORITY_CANDIDATE_LIMIT = 100;

    private final ModelRunRepository modelRunRepository;
    private final DemandForecastRepository demandForecastRepository;
    private final InventoryRecommendationRepository inventoryRecommendationRepository;

    public OperatorAiDashboardService(
            ModelRunRepository modelRunRepository,
            DemandForecastRepository demandForecastRepository,
            InventoryRecommendationRepository inventoryRecommendationRepository
    ) {
        this.modelRunRepository = modelRunRepository;
        this.demandForecastRepository = demandForecastRepository;
        this.inventoryRecommendationRepository = inventoryRecommendationRepository;
    }

    public OperatorAiDashboardResponse getDashboard() {
        ModelRun latest = modelRunRepository
                .findFirstByStatusOrderByCompletedAtDescIdDesc(ModelRunStatus.SUCCEEDED)
                .orElse(null);

        if (latest == null) {
            return emptyDashboard();
        }

        List<OperatorAiDashboardResponse.ModelMetric> comparison =
                modelRunRepository
                        .findAllByDatasetVersionAndStatusOrderByStartedAtAscIdAsc(
                                latest.getDatasetVersion(),
                                ModelRunStatus.SUCCEEDED
                        )
                        .stream()
                        .map(this::toMetric)
                        .toList();

        BigDecimal totalPredictedDemand =
                demandForecastRepository.sumPredictedDemandByModelRunId(latest.getId());
        long forecastSeries = demandForecastRepository.countByModelRunId(latest.getId());

        long openCount = inventoryRecommendationRepository.countByStatus(
                RecommendationStatus.OPEN
        );
        long highCount = inventoryRecommendationRepository.countByStatusAndRiskLevel(
                RecommendationStatus.OPEN,
                RiskLevel.HIGH
        );
        long mediumCount = inventoryRecommendationRepository.countByStatusAndRiskLevel(
                RecommendationStatus.OPEN,
                RiskLevel.MEDIUM
        );
        long lowCount = inventoryRecommendationRepository.countByStatusAndRiskLevel(
                RecommendationStatus.OPEN,
                RiskLevel.LOW
        );
        long additionalQuantity =
                inventoryRecommendationRepository
                        .sumRecommendedAdditionalQuantityByStatus(
                                RecommendationStatus.OPEN
                        );

        List<InventoryRecommendationResponse> priorities =
                inventoryRecommendationRepository
                        .findAllFiltered(
                                RecommendationStatus.OPEN,
                                null,
                                null,
                                null,
                                PageRequest.of(
                                        0,
                                        PRIORITY_CANDIDATE_LIMIT,
                                        Sort.by(
                                                Sort.Order.desc("recommendedAdditionalQuantity"),
                                                Sort.Order.desc("generatedAt"),
                                                Sort.Order.desc("id")
                                        )
                                )
                        )
                        .getContent()
                        .stream()
                        .sorted(
                                Comparator.comparingInt(this::riskPriority)
                                        .reversed()
                                        .thenComparing(
                                                InventoryRecommendation::getRecommendedAdditionalQuantity,
                                                Comparator.reverseOrder()
                                        )
                                        .thenComparing(
                                                InventoryRecommendation::getGeneratedAt,
                                                Comparator.reverseOrder()
                                        )
                        )
                        .limit(PRIORITY_LIMIT)
                        .map(InventoryRecommendationResponse::from)
                        .toList();

        return new OperatorAiDashboardResponse(
                LocalDateTime.now(),
                latest.getDatasetVersion(),
                new OperatorAiDashboardResponse.LatestModel(
                        latest.getId(),
                        latest.getModelName(),
                        latest.getModelVersion(),
                        latest.getRunType().name(),
                        latest.getMae(),
                        latest.getRmse(),
                        latest.getCompletedAt()
                ),
                new OperatorAiDashboardResponse.ForecastSummary(
                        forecastSeries,
                        totalPredictedDemand
                ),
                new OperatorAiDashboardResponse.RecommendationSummary(
                        openCount,
                        highCount,
                        mediumCount,
                        lowCount,
                        additionalQuantity
                ),
                comparison,
                priorities
        );
    }

    private int riskPriority(InventoryRecommendation recommendation) {
        return switch (recommendation.getRiskLevel()) {
            case HIGH -> 3;
            case MEDIUM -> 2;
            case LOW -> 1;
        };
    }

    private OperatorAiDashboardResponse.ModelMetric toMetric(ModelRun run) {
        return new OperatorAiDashboardResponse.ModelMetric(
                run.getId(),
                run.getModelName(),
                run.getRunType().name(),
                run.getMae(),
                run.getRmse()
        );
    }

    private OperatorAiDashboardResponse emptyDashboard() {
        return new OperatorAiDashboardResponse(
                LocalDateTime.now(),
                null,
                null,
                new OperatorAiDashboardResponse.ForecastSummary(
                        0,
                        BigDecimal.ZERO
                ),
                new OperatorAiDashboardResponse.RecommendationSummary(
                        0,
                        0,
                        0,
                        0,
                        0
                ),
                List.of(),
                List.of()
        );
    }
}
