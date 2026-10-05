package com.rentaltrack.rental.api;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record StartRentalRequest(
        @NotNull
        @Future
        LocalDateTime dueAt
) {
}
