package com.rentaltrack.core;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "damage_photo")
public class DamagePhoto {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(
    name = "damage_photo_id",
    nullable = false,
    columnDefinition = "BIGINT UNSIGNED"
  )
  Long id;

  @Column(
    name = "damage_id",
    nullable = false,
    columnDefinition = "BIGINT UNSIGNED"
  )
  Long damageId;

  @Column(
    name = "photo_id",
    nullable = false,
    columnDefinition = "BIGINT UNSIGNED"
  )
  Long photoId;

  @Column(name = "bbox_x_ratio", nullable = true, precision = 5, scale = 4)
  BigDecimal bboxXRatio;

  @Column(name = "bbox_y_ratio", nullable = true, precision = 5, scale = 4)
  BigDecimal bboxYRatio;

  @Column(name = "bbox_width_ratio", nullable = true, precision = 5, scale = 4)
  BigDecimal bboxWidthRatio;

  @Column(name = "bbox_height_ratio", nullable = true, precision = 5, scale = 4)
  BigDecimal bboxHeightRatio;

  @Column(
    name = "created_at",
    nullable = false,
    columnDefinition = "DATETIME(3)"
  )
  LocalDateTime createdAt = LocalDateTime.now();
}
