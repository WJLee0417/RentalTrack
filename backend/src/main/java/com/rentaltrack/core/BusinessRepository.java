package com.rentaltrack.core;

import jakarta.persistence.LockModeType;
import java.util.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface BusinessRepository extends JpaRepository<Business, Long> {
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select x from Business x where x.id=:id")
  Optional<Business> lock(@Param("id") Long id);

  List<Business> findAllByBusinessStatusCdOrderByIdAsc(String status);
}
