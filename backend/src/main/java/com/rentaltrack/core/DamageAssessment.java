package com.rentaltrack.core;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "damage_assessment")
public class DamageAssessment {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(
    name = "assessment_id",
    nullable = false,
    columnDefinition = "BIGINT UNSIGNED"
  )
  Long id;

  @Column(
    name = "inspection_id",
    nullable = false,
    columnDefinition = "BIGINT UNSIGNED"
  )
  Long inspectionId;

  @Column(
    name = "return_damage_id",
    nullable = false,
    columnDefinition = "BIGINT UNSIGNED"
  )
  Long returnDamageId;

  @Column(
    name = "intake_damage_id",
    nullable = true,
    columnDefinition = "BIGINT UNSIGNED"
  )
  Long intakeDamageId;

  @Column(name = "assessment_result_cd", nullable = false, length = 20)
  String assessmentResultCd;

  @Column(name = "assessment_reason", nullable = true, length = 1000)
  String assessmentReason;

  @Column(
    name = "created_at",
    nullable = false,
    columnDefinition = "DATETIME(3)"
  )
  LocalDateTime createdAt = LocalDateTime.now();
}
