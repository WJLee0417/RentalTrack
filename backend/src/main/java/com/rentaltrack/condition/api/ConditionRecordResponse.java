package com.rentaltrack.condition.api;

import com.rentaltrack.condition.domain.ConditionImage;
import com.rentaltrack.condition.domain.ConditionRecord;

import java.time.LocalDateTime;
import java.util.List;

public record ConditionRecordResponse(
        Long id,
        Long rentalContractId,
        String recordType,
        Long recordedByMemberId,
        String summary,
        LocalDateTime recordedAt,
        List<ConditionImageResponse> images
) {
    public static ConditionRecordResponse from(
            ConditionRecord record,
            List<ConditionImage> images
    ) {
        return new ConditionRecordResponse(
                record.getId(),
                record.getRentalContract().getId(),
                record.getRecordType().name(),
                record.getRecordedByMember() == null ? null : record.getRecordedByMember().getId(),
                record.getSummary(),
                record.getRecordedAt(),
                images.stream().map(ConditionImageResponse::from).toList()
        );
    }
}
