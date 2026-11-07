package com.rentaltrack.core;

import jakarta.persistence.LockModeType;
import java.util.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface ReturnInspectionRepository
  extends JpaRepository<ReturnInspection, Long>
{
  @Query("select x.contractId from ReturnInspection x where x.id=:id")
  Optional<Long> contractIdFor(@Param("id") Long id);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select x from ReturnInspection x where x.id=:id")
  Optional<ReturnInspection> lock(@Param("id") Long id);

  Optional<ReturnInspection> findByContractId(Long contractId);
}
