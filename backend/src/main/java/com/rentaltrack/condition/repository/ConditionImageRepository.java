package com.rentaltrack.condition.repository;

import com.rentaltrack.condition.domain.ConditionImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ConditionImageRepository extends JpaRepository<ConditionImage, Long> {

    long countByConditionRecordId(Long conditionRecordId);

    List<ConditionImage> findAllByConditionRecordIdOrderByCaptureOrderAsc(Long conditionRecordId);
}
