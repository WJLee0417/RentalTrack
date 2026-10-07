package com.rentaltrack.rental.api;

import com.rentaltrack.rental.domain.RentalContract;

import java.time.LocalDateTime;

public record RentalHistoryItemResponse(
        Long rentalContractId,
        String contractCode,
        Long rentalProductId,
        String productName,
        Long categoryId,
        String categoryCode,
        String categoryName,
        Long pickupLocationId,
        String pickupLocationName,
        Long returnLocationId,
        String returnLocationName,
        String status,
        LocalDateTime requestedAt,
        LocalDateTime pickedUpAt,
        LocalDateTime rentedAt,
        LocalDateTime dueAt,
        LocalDateTime returnedAt,
        LocalDateTime completedAt
) {
    public static RentalHistoryItemResponse from(RentalContract contract) {
        return new RentalHistoryItemResponse(
                contract.getId(),
                contract.getContractCode(),
                contract.getRentalProduct().getId(),
                contract.getRentalProduct().getDisplayName(),
                contract.getRentalProduct().getCategory().getId(),
                contract.getRentalProduct().getCategory().getCode(),
                contract.getRentalProduct().getCategory().getName(),
                contract.getPickupLocation().getId(),
                contract.getPickupLocation().getName(),
                contract.getReturnLocation() == null ? null : contract.getReturnLocation().getId(),
                contract.getReturnLocation() == null ? null : contract.getReturnLocation().getName(),
                contract.getStatus().name(),
                contract.getRequestedAt(),
                contract.getPickedUpAt(),
                contract.getRentedAt(),
                contract.getDueAt(),
                contract.getReturnedAt(),
                contract.getCompletedAt()
        );
    }
}
