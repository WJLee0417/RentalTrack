package com.rentaltrack.forecast.application;

import com.rentaltrack.forecast.api.DemandForecastResponse;
import com.rentaltrack.forecast.api.InventoryRecommendationResponse;
import com.rentaltrack.forecast.api.ModelRunResponse;
import com.rentaltrack.forecast.domain.ModelRunStatus;
import com.rentaltrack.forecast.domain.ModelRunType;
import com.rentaltrack.forecast.domain.RecommendationStatus;
import com.rentaltrack.forecast.domain.RiskLevel;
import com.rentaltrack.forecast.repository.DemandForecastRepository;
import com.rentaltrack.forecast.repository.InventoryRecommendationRepository;
import com.rentaltrack.forecast.repository.ModelRunRepository;
import com.rentaltrack.global.api.PageResponse;
import com.rentaltrack.global.error.ResourceNotFoundException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class OperatorForecastQueryService {

    private static final int MAX_PAGE_SIZE = 100;

    private final ModelRunRepository modelRunRepository;
    private final DemandForecastRepository demandForecastRepository;
    private final InventoryRecommendationRepository inventoryRecommendationRepository;

    public OperatorForecastQueryService(
            ModelRunRepository modelRunRepository,
            DemandForecastRepository demandForecastRepository,
            InventoryRecommendationRepository inventoryRecommendationRepository
    ) {
        this.modelRunRepository = modelRunRepository;
        this.demandForecastRepository = demandForecastRepository;
        this.inventoryRecommendationRepository = inventoryRecommendationRepository;
    }

    public PageResponse<ModelRunResponse> getModelRuns(
            ModelRunType runType,
            ModelRunStatus status,
            String datasetVersion,
            int page,
            int size
    ) {
        validatePage(page, size);

        var pageable = PageRequest.of(
                page,
                size,
                Sort.by(
                        Sort.Order.desc("startedAt"),
                        Sort.Order.desc("id")
                )
        );

        return PageResponse.from(
                modelRunRepository.findAllFiltered(
                        runType,
                        status,
                        normalize(datasetVersion),
                        pageable
                ),
                ModelRunResponse::from
        );
    }

    public ModelRunResponse getModelRun(Long modelRunId) {
        return modelRunRepository.findById(modelRunId)
                .map(ModelRunResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Model run not found: " + modelRunId
                ));
    }

    public PageResponse<DemandForecastResponse> getForecasts(
            Long modelRunId,
            Long locationId,
            Long categoryId,
            int page,
            int size
    ) {
        validatePage(page, size);

        var pageable = PageRequest.of(
                page,
                size,
                Sort.by(
                        Sort.Order.desc("generatedAt"),
                        Sort.Order.desc("id")
                )
        );

        return PageResponse.from(
                demandForecastRepository.findAllFiltered(
                        modelRunId,
                        locationId,
                        categoryId,
                        pageable
                ),
                DemandForecastResponse::from
        );
    }

    public DemandForecastResponse getForecast(Long forecastId) {
        return demandForecastRepository.findDetailById(forecastId)
                .map(DemandForecastResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Demand forecast not found: " + forecastId
                ));
    }

    public PageResponse<InventoryRecommendationResponse> getRecommendations(
            RecommendationStatus status,
            RiskLevel riskLevel,
            Long locationId,
            Long categoryId,
            int page,
            int size
    ) {
        validatePage(page, size);

        var pageable = PageRequest.of(
                page,
                size,
                Sort.by(
                        Sort.Order.desc("generatedAt"),
                        Sort.Order.desc("id")
                )
        );

        return PageResponse.from(
                inventoryRecommendationRepository.findAllFiltered(
                        status,
                        riskLevel,
                        locationId,
                        categoryId,
                        pageable
                ),
                InventoryRecommendationResponse::from
        );
    }

    public InventoryRecommendationResponse getRecommendation(Long recommendationId) {
        return inventoryRecommendationRepository.findDetailById(recommendationId)
                .map(InventoryRecommendationResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Inventory recommendation not found: " + recommendationId
                ));
    }

    private void validatePage(int page, int size) {
        if (page < 0) {
            throw new IllegalArgumentException("page must be 0 or greater");
        }

        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException(
                    "size must be between 1 and " + MAX_PAGE_SIZE
            );
        }
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
