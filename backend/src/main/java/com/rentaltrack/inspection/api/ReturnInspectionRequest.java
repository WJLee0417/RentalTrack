package com.rentaltrack.inspection.api;

import com.rentaltrack.inspection.domain.ReturnInspectionResult;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ReturnInspectionRequest(
        @NotNull ReturnInspectionResult result,

        @Size(max = 1000)
        String note
) {
}
