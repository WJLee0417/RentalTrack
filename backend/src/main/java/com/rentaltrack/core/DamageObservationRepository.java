package com.rentaltrack.core;

import jakarta.persistence.LockModeType;
import java.util.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface DamageObservationRepository
  extends JpaRepository<DamageObservation, Long>
{
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select x from DamageObservation x where x.id=:id")
  Optional<DamageObservation> lock(@Param("id") Long id);

  List<DamageObservation> findAllByConditionIdOrderByIdAsc(Long conditionId);
}
