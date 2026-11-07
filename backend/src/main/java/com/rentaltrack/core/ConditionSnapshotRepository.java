package com.rentaltrack.core;

import jakarta.persistence.LockModeType;
import java.util.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface ConditionSnapshotRepository
  extends JpaRepository<ConditionSnapshot, Long>
{
  @Query("select x.contractId from ConditionSnapshot x where x.id=:id")
  Optional<Long> contractIdFor(@Param("id") Long id);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select x from ConditionSnapshot x where x.id=:id")
  Optional<ConditionSnapshot> lock(@Param("id") Long id);

  List<ConditionSnapshot> findAllByContractIdOrderByRecordStageCdAscRecordRevisionNoAsc(
    Long contractId
  );
  Optional<ConditionSnapshot> findTopByContractIdAndRecordStageCdOrderByRecordRevisionNoDesc(
    Long contractId,
    String stage
  );
}
