package com.rentaltrack.forecast.application;

import com.rentaltrack.forecast.api.InventoryRecommendationResponse;
import com.rentaltrack.forecast.domain.InventoryRecommendation;
import com.rentaltrack.forecast.domain.RecommendationStatus;
import com.rentaltrack.forecast.repository.InventoryRecommendationRepository;
import com.rentaltrack.global.error.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class OperatorRecommendationDecisionService {

    private final InventoryRecommendationRepository inventoryRecommendationRepository;

    public OperatorRecommendationDecisionService(
            InventoryRecommendationRepository inventoryRecommendationRepository
    ) {
        this.inventoryRecommendationRepository = inventoryRecommendationRepository;
    }

    @Transactional
    public InventoryRecommendationResponse decide(
            Long recommendationId,
            RecommendationStatus decision
    ) {
        if (decision == null || decision == RecommendationStatus.OPEN) {
            throw new IllegalArgumentException(
                    "Recommendation decision must be ACCEPTED or DISMISSED"
            );
        }

        InventoryRecommendation recommendation =
                inventoryRecommendationRepository.findDetailByIdForUpdate(recommendationId)
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "Inventory recommendation not found: " + recommendationId
                        ));

        recommendation.decide(decision, LocalDateTime.now());

        return InventoryRecommendationResponse.from(recommendation);
    }
}
