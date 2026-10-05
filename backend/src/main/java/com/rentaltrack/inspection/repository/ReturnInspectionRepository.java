package com.rentaltrack.inspection.repository;

import com.rentaltrack.inspection.domain.ReturnInspection;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ReturnInspectionRepository extends JpaRepository<ReturnInspection, Long> {

    Optional<ReturnInspection> findByRentalContractId(Long rentalContractId);

    boolean existsByRentalContractId(Long rentalContractId);
}
