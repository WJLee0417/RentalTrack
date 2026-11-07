package com.rentaltrack.core;

import jakarta.persistence.LockModeType;
import java.util.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface ConditionPhotoRepository
  extends JpaRepository<ConditionPhoto, Long>
{
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select x from ConditionPhoto x where x.id=:id")
  Optional<ConditionPhoto> lock(@Param("id") Long id);

  List<ConditionPhoto> findAllByConditionIdOrderByPhotoSeqAsc(Long conditionId);
}
