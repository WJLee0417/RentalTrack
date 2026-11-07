package com.rentaltrack.core;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "damage_observation")
public class DamageObservation {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(
    name = "damage_id",
    nullable = false,
    columnDefinition = "BIGINT UNSIGNED"
  )
  Long id;

  @Column(
    name = "condition_id",
    nullable = false,
    columnDefinition = "BIGINT UNSIGNED"
  )
  Long conditionId;

  @Column(name = "damage_location_cd", nullable = false, length = 40)
  String damageLocationCd;

  @Column(name = "damage_type_cd", nullable = false, length = 40)
  String damageTypeCd;

  @Column(name = "damage_description", nullable = true, length = 1000)
  String damageDescription;

  @Column(name = "severity_cd", nullable = false, length = 20)
  String severityCd;

  @Column(
    name = "created_at",
    nullable = false,
    columnDefinition = "DATETIME(3)"
  )
  LocalDateTime createdAt = LocalDateTime.now();
}
