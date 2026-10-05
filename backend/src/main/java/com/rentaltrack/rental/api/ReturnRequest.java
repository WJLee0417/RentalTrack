package com.rentaltrack.rental.api;

import jakarta.validation.constraints.NotNull;

public record ReturnRequest(
        @NotNull Long returnLocationId
) {
}
