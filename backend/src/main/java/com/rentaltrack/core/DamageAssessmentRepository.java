package com.rentaltrack.core;

import jakarta.persistence.LockModeType;
import java.util.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface DamageAssessmentRepository
  extends JpaRepository<DamageAssessment, Long>
{
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select x from DamageAssessment x where x.id=:id")
  Optional<DamageAssessment> lock(@Param("id") Long id);

  List<DamageAssessment> findAllByInspectionIdOrderByIdAsc(Long inspectionId);
  Optional<DamageAssessment> findByInspectionIdAndReturnDamageId(
    Long inspectionId,
    Long damageId
  );
}
