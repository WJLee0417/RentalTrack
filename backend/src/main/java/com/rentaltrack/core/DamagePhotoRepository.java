package com.rentaltrack.core;

import jakarta.persistence.LockModeType;
import java.util.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface DamagePhotoRepository
  extends JpaRepository<DamagePhoto, Long>
{
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select x from DamagePhoto x where x.id=:id")
  Optional<DamagePhoto> lock(@Param("id") Long id);

  List<DamagePhoto> findAllByDamageIdOrderByIdAsc(Long damageId);
  boolean existsByDamageIdAndPhotoId(Long damageId, Long photoId);
}
