package com.rentaltrack.condition.repository;

import com.rentaltrack.condition.domain.ConditionRecord;
import com.rentaltrack.condition.domain.ConditionRecordType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ConditionRecordRepository extends JpaRepository<ConditionRecord, Long> {

    Optional<ConditionRecord> findByRentalContractIdAndRecordType(
            Long rentalContractId,
            ConditionRecordType recordType
    );

    boolean existsByRentalContractIdAndRecordType(
            Long rentalContractId,
            ConditionRecordType recordType
    );
}
