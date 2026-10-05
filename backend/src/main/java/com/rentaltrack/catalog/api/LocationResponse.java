package com.rentaltrack.catalog.api;

import com.rentaltrack.location.domain.RentalLocation;

import java.math.BigDecimal;

public record LocationResponse(
        Long id,
        String code,
        String name,
        String address,
        BigDecimal latitude,
        BigDecimal longitude
) {
    public static LocationResponse from(RentalLocation location) {
        return new LocationResponse(
                location.getId(),
                location.getCode(),
                location.getName(),
                location.getAddress(),
                location.getLatitude(),
                location.getLongitude()
        );
    }
}
