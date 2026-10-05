package com.rentaltrack.condition.api;

import com.rentaltrack.condition.domain.ConditionImage;

import java.time.LocalDateTime;

public record ConditionImageResponse(
        Long id,
        String imageUrl,
        String storageKey,
        int captureOrder,
        String viewpoint,
        LocalDateTime capturedAt
) {
    public static ConditionImageResponse from(ConditionImage image) {
        return new ConditionImageResponse(
                image.getId(),
                image.getImageUrl(),
                image.getStorageKey(),
                image.getCaptureOrder(),
                image.getViewpoint(),
                image.getCapturedAt()
        );
    }
}
