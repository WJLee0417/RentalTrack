package com.rentaltrack.core;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "rental_item")
public class RentalItem {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(
    name = "item_id",
    nullable = false,
    columnDefinition = "BIGINT UNSIGNED"
  )
  Long id;

  @Column(
    name = "business_id",
    nullable = false,
    columnDefinition = "BIGINT UNSIGNED"
  )
  Long businessId;

  @Column(name = "asset_code", nullable = false, length = 40)
  String assetCode;

  @Column(name = "item_name", nullable = false, length = 200)
  String itemName;

  @Column(name = "category_cd", nullable = false, length = 30)
  String categoryCd;

  @Column(name = "item_status_cd", nullable = false, length = 20)
  String itemStatusCd;

  @Column(name = "description", nullable = true, length = 1000)
  String description;

  @Column(
    name = "created_at",
    nullable = false,
    columnDefinition = "DATETIME(3)"
  )
  LocalDateTime createdAt = LocalDateTime.now();
}
