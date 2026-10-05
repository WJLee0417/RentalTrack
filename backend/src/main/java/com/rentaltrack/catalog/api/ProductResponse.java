package com.rentaltrack.catalog.api;

import com.rentaltrack.product.domain.RentalProduct;

public record ProductResponse(
        Long id,
        String assetCode,
        String displayName,
        Long categoryId,
        String categoryCode,
        String categoryName,
        Long locationId,
        String locationName,
        String status
) {
    public static ProductResponse from(RentalProduct product) {
        return new ProductResponse(
                product.getId(),
                product.getAssetCode(),
                product.getDisplayName(),
                product.getCategory().getId(),
                product.getCategory().getCode(),
                product.getCategory().getName(),
                product.getCurrentLocation().getId(),
                product.getCurrentLocation().getName(),
                product.getStatus().name()
        );
    }
}
