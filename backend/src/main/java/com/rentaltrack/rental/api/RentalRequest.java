package com.rentaltrack.rental.api;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record RentalRequest(
        @NotNull @Positive Long memberId,
        @NotNull @Positive Long rentalProductId,
        @NotNull @Positive Long pickupLocationId
) {
}
