package com.rentaltrack.damage.api;

import com.rentaltrack.damage.domain.DamageSeverity;
import com.rentaltrack.damage.domain.DamageType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record DamageCreateRequest(
        @NotNull DamageType damageType,

        @NotBlank
        @Size(max = 100)
        String damageLocation,

        @NotNull DamageSeverity severity,

        @Size(max = 500)
        String description
) {
}
