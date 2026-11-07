package com.rentaltrack.core;

import jakarta.persistence.LockModeType;
import java.util.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface RentalAgreementRepository
  extends JpaRepository<RentalAgreement, Long>
{
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select x from RentalAgreement x where x.id=:id")
  Optional<RentalAgreement> lock(@Param("id") Long id);

  List<RentalAgreement> findAllByMemberIdOrderByIdDesc(Long memberId);
  boolean existsByItemIdAndContractStatusCdIn(
    Long itemId,
    Collection<String> statuses
  );
}
