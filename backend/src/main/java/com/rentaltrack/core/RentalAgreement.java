package com.rentaltrack.core;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "rental_contract")
public class RentalAgreement {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(
    name = "contract_id",
    nullable = false,
    columnDefinition = "BIGINT UNSIGNED"
  )
  Long id;

  @Column(name = "contract_no", nullable = false, length = 50)
  String contractNo;

  @Column(
    name = "member_id",
    nullable = false,
    columnDefinition = "BIGINT UNSIGNED"
  )
  Long memberId;

  @Column(
    name = "item_id",
    nullable = false,
    columnDefinition = "BIGINT UNSIGNED"
  )
  Long itemId;

  @Column(
    name = "requested_at",
    nullable = false,
    columnDefinition = "DATETIME(3)"
  )
  LocalDateTime requestedAt;

  @Column(
    name = "rental_start_at",
    nullable = true,
    columnDefinition = "DATETIME(3)"
  )
  LocalDateTime rentalStartAt;

  @Column(
    name = "rental_end_at",
    nullable = true,
    columnDefinition = "DATETIME(3)"
  )
  LocalDateTime rentalEndAt;

  @Column(
    name = "actual_return_at",
    nullable = true,
    columnDefinition = "DATETIME(3)"
  )
  LocalDateTime actualReturnAt;

  @Column(name = "contract_status_cd", nullable = false, length = 20)
  String contractStatusCd;

  @Column(
    name = "created_at",
    nullable = false,
    columnDefinition = "DATETIME(3)"
  )
  LocalDateTime createdAt = LocalDateTime.now();
}
