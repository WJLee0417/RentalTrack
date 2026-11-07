package com.rentaltrack.core;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "return_inspection")
public class ReturnInspection {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(
    name = "inspection_id",
    nullable = false,
    columnDefinition = "BIGINT UNSIGNED"
  )
  Long id;

  @Column(
    name = "contract_id",
    nullable = false,
    columnDefinition = "BIGINT UNSIGNED"
  )
  Long contractId;

  @Column(
    name = "intake_condition_id",
    nullable = false,
    columnDefinition = "BIGINT UNSIGNED"
  )
  Long intakeConditionId;

  @Column(
    name = "return_condition_id",
    nullable = false,
    columnDefinition = "BIGINT UNSIGNED"
  )
  Long returnConditionId;

  @Column(
    name = "inspector_member_id",
    nullable = false,
    columnDefinition = "BIGINT UNSIGNED"
  )
  Long inspectorMemberId;

  @Column(name = "inspection_status_cd", nullable = false, length = 20)
  String inspectionStatusCd;

  @Column(name = "overall_result_cd", nullable = true, length = 20)
  String overallResultCd;

  @Column(
    name = "inspected_at",
    nullable = true,
    columnDefinition = "DATETIME(3)"
  )
  LocalDateTime inspectedAt;

  @Column(name = "inspection_note", nullable = true, length = 1000)
  String inspectionNote;

  @Column(
    name = "created_at",
    nullable = false,
    columnDefinition = "DATETIME(3)"
  )
  LocalDateTime createdAt = LocalDateTime.now();
}
