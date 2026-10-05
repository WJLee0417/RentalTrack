package com.rentaltrack.damage.repository;

import com.rentaltrack.damage.domain.DamageComparisonResult;
import com.rentaltrack.damage.domain.DamageRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DamageRecordRepository extends JpaRepository<DamageRecord, Long> {

    List<DamageRecord> findAllByConditionRecordIdOrderByIdAsc(Long conditionRecordId);

    boolean existsByConditionRecordIdAndComparisonResult(
            Long conditionRecordId,
            DamageComparisonResult comparisonResult
    );
}
