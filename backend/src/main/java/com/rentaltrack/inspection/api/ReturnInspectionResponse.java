package com.rentaltrack.inspection.api;

import com.rentaltrack.inspection.domain.ReturnInspection;

import java.time.LocalDateTime;

public record ReturnInspectionResponse(
        Long id,
        Long rentalContractId,
        String result,
        String note,
        LocalDateTime inspectedAt
) {
    public static ReturnInspectionResponse from(ReturnInspection inspection) {
        return new ReturnInspectionResponse(
                inspection.getId(),
                inspection.getRentalContract().getId(),
                inspection.getResult().name(),
                inspection.getNote(),
                inspection.getInspectedAt()
        );
    }
}
