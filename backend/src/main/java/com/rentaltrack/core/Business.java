package com.rentaltrack.core;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "business")
public class Business {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(
    name = "business_id",
    nullable = false,
    columnDefinition = "BIGINT UNSIGNED"
  )
  Long id;

  @Column(name = "business_no", nullable = false, length = 10)
  String businessNo;

  @Column(name = "business_name", nullable = false, length = 200)
  String businessName;

  @Column(name = "business_status_cd", nullable = false, length = 20)
  String businessStatusCd;

  @Column(name = "contact_email", nullable = true, length = 254)
  String contactEmail;

  @Column(
    name = "created_at",
    nullable = false,
    columnDefinition = "DATETIME(3)"
  )
  LocalDateTime createdAt = LocalDateTime.now();
}
