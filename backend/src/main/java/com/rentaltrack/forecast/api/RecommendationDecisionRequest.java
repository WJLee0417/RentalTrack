package com.rentaltrack.forecast.api;

import com.rentaltrack.forecast.domain.RecommendationStatus;
import jakarta.validation.constraints.NotNull;

public record RecommendationDecisionRequest(
        @NotNull RecommendationStatus decision
) {
}
