package com.rentaltrack.core;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "condition_record")
public class ConditionSnapshot {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(
    name = "condition_id",
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

  @Column(name = "record_stage_cd", nullable = false, length = 10)
  String recordStageCd;

  @Column(name = "record_revision_no", nullable = false)
  Integer recordRevisionNo;

  @Column(
    name = "recorded_at",
    nullable = false,
    columnDefinition = "DATETIME(3)"
  )
  LocalDateTime recordedAt;

  @Column(name = "record_note", nullable = true, length = 1000)
  String recordNote;

  @Column(
    name = "confirmed_yn",
    nullable = false,
    length = 1,
    columnDefinition = "CHAR(1)"
  )
  String confirmedYn;

  @Column(
    name = "created_at",
    nullable = false,
    columnDefinition = "DATETIME(3)"
  )
  LocalDateTime createdAt = LocalDateTime.now();
}
