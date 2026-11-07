package com.rentaltrack.legacy;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;

public record LegacyManifest(
  @NotNull @Size(max = 1000) List<@Valid MemberMapping> members,
  @NotNull @Size(max = 100) List<@Valid ProductMapping> products,
  @NotNull @Size(max = 1000) List<@Valid InspectorMapping> inspections,
  @NotNull @Size(max = 1000) List<@Valid LocationMapping> damageLocations
) {
  public record MemberMapping(
    @NotNull @Positive Long legacyMemberId,
    @Pattern(regexp = "[a-zA-Z0-9._-]{3,50}") String loginId,
    @Positive Long existingMemberId
  ) {}

  public record ProductMapping(
    @NotNull @Positive Long legacyProductId,
    @Positive Long businessId,
    String categoryCd,
    @Size(max = 40) String assetCode
  ) {}

  public record InspectorMapping(
    @NotNull @Positive Long legacyInspectionId,
    @Positive Long inspectorMemberId,
    @Positive Long legacyInspectorMemberId
  ) {}

  public record LocationMapping(
    @NotBlank String source,
    @NotBlank String target
  ) {}

  public record ApplyInput(
    @NotNull @Valid LegacyManifest manifest,
    @NotBlank @Pattern(regexp = "[0-9a-f]{64}") String planToken
  ) {}
}
