package com.rentaltrack.rental.api;

import jakarta.validation.constraints.NotNull;

public record RentalRequest(
        @NotNull Long memberId,
        @NotNull Long rentalProductId,
        @NotNull Long pickupLocationId
) {
}
