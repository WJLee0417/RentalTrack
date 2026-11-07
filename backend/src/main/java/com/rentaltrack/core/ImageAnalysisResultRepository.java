package com.rentaltrack.core;

import jakarta.persistence.LockModeType;
import java.util.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface ImageAnalysisResultRepository
  extends JpaRepository<ImageAnalysisResult, Long>
{
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select x from ImageAnalysisResult x where x.id=:id")
  Optional<ImageAnalysisResult> lock(@Param("id") Long id);

  List<ImageAnalysisResult> findAllByPhotoIdOrderByIdAsc(Long photoId);
}
