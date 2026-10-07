package com.rentaltrack.condition.api;

import com.rentaltrack.condition.domain.ConditionImage;

import java.time.LocalDateTime;

public record ConditionImageResponse(
        Long id,
        String storageKey,
        String originalFilename,
        String contentType,
        Long fileSize,
        int captureOrder,
        String viewpoint,
        LocalDateTime capturedAt,
        String contentPath
) {
    public static ConditionImageResponse from(ConditionImage image) {
        Long rentalContractId = image.getConditionRecord().getRentalContract().getId();
        String segment = image.getConditionRecord().getRecordType().name().equals("BEFORE_RENTAL")
                ? "before"
                : "after";

        return new ConditionImageResponse(
                image.getId(),
                image.getStorageKey(),
                image.getOriginalFilename(),
                image.getContentType(),
                image.getFileSize(),
                image.getCaptureOrder(),
                image.getViewpoint(),
                image.getCapturedAt(),
                "/api/v1/rentals/" + rentalContractId
                        + "/conditions/" + segment
                        + "/images/" + image.getId()
                        + "/content"
        );
    }
}
