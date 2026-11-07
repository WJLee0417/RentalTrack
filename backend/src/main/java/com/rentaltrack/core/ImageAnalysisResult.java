package com.rentaltrack.core;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "image_analysis_result")
public class ImageAnalysisResult {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(
    name = "analysis_result_id",
    nullable = false,
    columnDefinition = "BIGINT UNSIGNED"
  )
  Long id;

  @Column(
    name = "photo_id",
    nullable = false,
    columnDefinition = "BIGINT UNSIGNED"
  )
  Long photoId;

  @Column(
    name = "analysis_run_id",
    nullable = false,
    length = 36,
    columnDefinition = "CHAR(36)"
  )
  String analysisRunId;

  @Column(name = "detection_seq", nullable = false)
  Integer detectionSeq;

  @Column(name = "model_name", nullable = false, length = 100)
  String modelName;

  @Column(name = "model_version", nullable = false, length = 50)
  String modelVersion;

  @Column(name = "predicted_damage_type_cd", nullable = false, length = 40)
  String predictedDamageTypeCd;

  @Column(name = "confidence_score", nullable = false, precision = 5, scale = 4)
  BigDecimal confidenceScore;

  @Column(name = "bbox_x_ratio", nullable = false, precision = 5, scale = 4)
  BigDecimal bboxXRatio;

  @Column(name = "bbox_y_ratio", nullable = false, precision = 5, scale = 4)
  BigDecimal bboxYRatio;

  @Column(name = "bbox_width_ratio", nullable = false, precision = 5, scale = 4)
  BigDecimal bboxWidthRatio;

  @Column(
    name = "bbox_height_ratio",
    nullable = false,
    precision = 5,
    scale = 4
  )
  BigDecimal bboxHeightRatio;

  @Column(
    name = "analyzed_at",
    nullable = false,
    columnDefinition = "DATETIME(3)"
  )
  LocalDateTime analyzedAt;
}
