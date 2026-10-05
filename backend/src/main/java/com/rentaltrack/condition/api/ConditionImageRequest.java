package com.rentaltrack.condition.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record ConditionImageRequest(
        @NotBlank @Size(max = 1000) String imageUrl,
        @NotBlank @Size(max = 500) String storageKey,
        @Positive int captureOrder,
        @Size(max = 30) String viewpoint,
        @NotNull @PastOrPresent LocalDateTime capturedAt
) {
}
