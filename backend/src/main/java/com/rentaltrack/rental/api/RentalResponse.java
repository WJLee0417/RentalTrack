package com.rentaltrack.rental.api;

import com.rentaltrack.rental.domain.RentalContract;

import java.time.LocalDateTime;

public record RentalResponse(
        Long id,
        String contractCode,
        Long memberId,
        Long rentalProductId,
        String productName,
        Long pickupLocationId,
        String pickupLocationName,
        String status,
        LocalDateTime requestedAt,
        LocalDateTime approvedAt,
        LocalDateTime pickedUpAt,
        LocalDateTime rentedAt,
        LocalDateTime dueAt
) {
    public static RentalResponse from(RentalContract contract) {
        return new RentalResponse(
                contract.getId(),
                contract.getContractCode(),
                contract.getMember().getId(),
                contract.getRentalProduct().getId(),
                contract.getRentalProduct().getDisplayName(),
                contract.getPickupLocation().getId(),
                contract.getPickupLocation().getName(),
                contract.getStatus().name(),
                contract.getRequestedAt(),
                contract.getApprovedAt(),
                contract.getPickedUpAt(),
                contract.getRentedAt(),
                contract.getDueAt()
        );
    }
}
