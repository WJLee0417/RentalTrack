package com.rentaltrack.condition.api;

import jakarta.validation.constraints.Size;

public record BeforeConditionRequest(
        @Size(max = 500)
        String summary
) {
}
