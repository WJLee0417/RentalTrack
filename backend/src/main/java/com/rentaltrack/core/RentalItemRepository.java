package com.rentaltrack.core;

import jakarta.persistence.LockModeType;
import java.util.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface RentalItemRepository extends JpaRepository<RentalItem, Long> {
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select x from RentalItem x where x.id=:id")
  Optional<RentalItem> lock(@Param("id") Long id);

  List<RentalItem> findAllByOrderByIdDesc();
}
