package com.rentaltrack.core;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "condition_photo")
public class ConditionPhoto {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(
    name = "photo_id",
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

  @Column(name = "photo_seq", nullable = false)
  Integer photoSeq;

  @Column(name = "storage_key", nullable = false, length = 500)
  String storageKey;

  @Column(name = "original_file_name", nullable = false, length = 255)
  String originalFileName;

  @Column(name = "mime_type", nullable = false, length = 100)
  String mimeType;

  @Column(
    name = "file_size",
    nullable = false,
    columnDefinition = "BIGINT UNSIGNED"
  )
  Long fileSize;

  @Column(
    name = "file_hash",
    nullable = false,
    length = 64,
    columnDefinition = "CHAR(64)"
  )
  String fileHash;

  @Column(name = "capture_angle_cd", nullable = false, length = 30)
  String captureAngleCd;

  @Column(
    name = "captured_at",
    nullable = true,
    columnDefinition = "DATETIME(3)"
  )
  LocalDateTime capturedAt;

  @Column(
    name = "created_at",
    nullable = false,
    columnDefinition = "DATETIME(3)"
  )
  LocalDateTime createdAt = LocalDateTime.now();
}
