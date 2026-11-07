package com.rentaltrack.core;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public final class Requests {

  private Requests() {}

  public record BusinessInput(
    @NotBlank @Pattern(regexp = "[0-9]{10}") String businessNo,
    @NotBlank @Size(max = 200) String businessName,
    @Email @Size(max = 254) String contactEmail
  ) {}

  public record ItemInput(
    @NotNull @Positive Long businessId,
    @NotBlank @Size(max = 40) String assetCode,
    @NotBlank @Size(max = 200) String itemName,
    @NotBlank String categoryCd,
    @Size(max = 1000) String description
  ) {}

  public record RentalInput(@NotNull @Positive Long itemId) {}

  public record StartInput(
    @NotNull LocalDateTime rentalStartAt,
    @NotNull LocalDateTime rentalEndAt
  ) {}

  public record ReturnInput(@NotNull LocalDateTime actualReturnAt) {}

  public record ConditionInput(
    @NotBlank String recordStageCd,
    @Size(max = 1000) String recordNote
  ) {}

  public record NoteInput(@Size(max = 1000) String recordNote) {}

  public record DamageInput(
    @NotBlank String damageLocationCd,
    @NotBlank String damageTypeCd,
    @NotBlank String severityCd,
    @Size(max = 1000) String damageDescription
  ) {}

  public record Box(
    @NotNull @DecimalMin("0") @DecimalMax("1") BigDecimal x,
    @NotNull @DecimalMin("0") @DecimalMax("1") BigDecimal y,
    @NotNull
    @DecimalMin(value = "0", inclusive = false)
    @DecimalMax("1")
    BigDecimal width,
    @NotNull
    @DecimalMin(value = "0", inclusive = false)
    @DecimalMax("1")
    BigDecimal height
  ) {
    public void check() {
      if (
        x == null ||
        y == null ||
        width == null ||
        height == null ||
        x.signum() < 0 ||
        y.signum() < 0 ||
        width.signum() <= 0 ||
        height.signum() <= 0 ||
        x.add(width).compareTo(BigDecimal.ONE) > 0 ||
        y.add(height).compareTo(BigDecimal.ONE) > 0
      ) throw new IllegalArgumentException(
        "Bounding box must be inside the image"
      );
    }
  }

  public record EvidenceInput(
    @NotNull @Positive Long photoId,
    @Valid Box box
  ) {}

  public record AnalysisInput(
    @NotBlank
    @Pattern(
      regexp = "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}"
    )
    String analysisRunId,
    @NotNull @Positive Integer detectionSeq,
    @NotBlank @Size(max = 100) String modelName,
    @NotBlank @Size(max = 50) String modelVersion,
    @NotBlank String predictedDamageTypeCd,
    @NotNull @DecimalMin("0") @DecimalMax("1") BigDecimal confidenceScore,
    @NotNull @Valid Box box,
    @NotNull LocalDateTime analyzedAt
  ) {}

  public record InspectionInput(
    @NotNull @Positive Long intakeConditionId,
    @NotNull @Positive Long returnConditionId,
    @Size(max = 1000) String inspectionNote
  ) {}

  public record AssessmentInput(
    @NotNull @Positive Long returnDamageId,
    @Positive Long intakeDamageId,
    @NotBlank String assessmentResultCd,
    @NotBlank @Size(max = 1000) String assessmentReason
  ) {}
}
