package com.rentaltrack.damage.api;

import com.rentaltrack.damage.domain.DamageRecord;

public record DamageResponse(
        Long id,
        Long conditionRecordId,
        String damageType,
        String damageLocation,
        String severity,
        String description,
        String comparisonResult,
        Long matchedBeforeDamageId
) {
    public static DamageResponse from(DamageRecord damage) {
        return new DamageResponse(
                damage.getId(),
                damage.getConditionRecord().getId(),
                damage.getDamageType().name(),
                damage.getDamageLocation(),
                damage.getSeverity().name(),
                damage.getDescription(),
                damage.getComparisonResult().name(),
                damage.getMatchedBeforeDamage() == null ? null : damage.getMatchedBeforeDamage().getId()
        );
    }
}
