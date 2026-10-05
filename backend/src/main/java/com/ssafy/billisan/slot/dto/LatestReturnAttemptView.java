package com.ssafy.billisan.slot.dto;

import com.ssafy.billisan.returns.domain.ReturnAttempt;
import java.util.UUID;

public record LatestReturnAttemptView(
        UUID returnAttemptId,
        UUID rentalId,
        String status
) {
    public static LatestReturnAttemptView of(ReturnAttempt returnAttempt) {
        return new LatestReturnAttemptView(
                returnAttempt.getReturnAttemptId(),
                returnAttempt.getRentalId(),
                returnAttempt.getStatus().name());
    }
}
