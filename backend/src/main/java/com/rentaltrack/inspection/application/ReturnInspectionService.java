package com.rentaltrack.inspection.application;

import com.rentaltrack.condition.domain.ConditionRecord;
import com.rentaltrack.condition.domain.ConditionRecordType;
import com.rentaltrack.condition.repository.ConditionRecordRepository;
import com.rentaltrack.damage.domain.DamageComparisonResult;
import com.rentaltrack.damage.repository.DamageRecordRepository;
import com.rentaltrack.global.error.ResourceNotFoundException;
import com.rentaltrack.inspection.api.ReturnInspectionRequest;
import com.rentaltrack.inspection.api.ReturnInspectionResponse;
import com.rentaltrack.inspection.domain.ReturnInspection;
import com.rentaltrack.inspection.domain.ReturnInspectionResult;
import com.rentaltrack.inspection.repository.ReturnInspectionRepository;
import com.rentaltrack.product.domain.RentalProduct;
import com.rentaltrack.product.repository.RentalProductRepository;
import com.rentaltrack.rental.domain.RentalContract;
import com.rentaltrack.rental.domain.RentalContractStatus;
import com.rentaltrack.rental.repository.RentalContractRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class ReturnInspectionService {

    private final RentalContractRepository rentalContractRepository;
    private final RentalProductRepository rentalProductRepository;
    private final ConditionRecordRepository conditionRecordRepository;
    private final DamageRecordRepository damageRecordRepository;
    private final ReturnInspectionRepository returnInspectionRepository;

    public ReturnInspectionService(
            RentalContractRepository rentalContractRepository,
            RentalProductRepository rentalProductRepository,
            ConditionRecordRepository conditionRecordRepository,
            DamageRecordRepository damageRecordRepository,
            ReturnInspectionRepository returnInspectionRepository
    ) {
        this.rentalContractRepository = rentalContractRepository;
        this.rentalProductRepository = rentalProductRepository;
        this.conditionRecordRepository = conditionRecordRepository;
        this.damageRecordRepository = damageRecordRepository;
        this.returnInspectionRepository = returnInspectionRepository;
    }

    @Transactional
    public ReturnInspectionResponse inspect(
            Long rentalContractId,
            ReturnInspectionRequest request
    ) {
        RentalContract contract = rentalContractRepository.findByIdForUpdate(rentalContractId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Rental contract not found: " + rentalContractId
                ));

        if (contract.getStatus() != RentalContractStatus.INSPECTING) {
            throw new IllegalStateException("Return inspection can be processed only in INSPECTING state");
        }

        ConditionRecord after = conditionRecordRepository.findByRentalContractIdAndRecordType(
                        rentalContractId,
                        ConditionRecordType.AFTER_RENTAL
                )
                .orElseThrow(() -> new ResourceNotFoundException(
                        "AFTER_RENTAL condition not found for rental: " + rentalContractId
                ));

        if (damageRecordRepository.existsByConditionRecordIdAndComparisonResult(
                after.getId(),
                DamageComparisonResult.UNRESOLVED
        )) {
            throw new IllegalStateException("Unresolved AFTER damage remains");
        }

        boolean hasNewDamage = damageRecordRepository.existsByConditionRecordIdAndComparisonResult(
                after.getId(),
                DamageComparisonResult.NEW
        );

        validateResult(request.result(), hasNewDamage);

        LocalDateTime now = LocalDateTime.now();

        ReturnInspection inspection = returnInspectionRepository.findByRentalContractId(rentalContractId)
                .map(existing -> {
                    if (existing.getResult() != ReturnInspectionResult.REVIEW_REQUIRED) {
                        throw new IllegalStateException("Final return inspection already exists");
                    }
                    existing.revise(request.result(), request.note(), now);
                    return existing;
                })
                .orElseGet(() -> returnInspectionRepository.save(new ReturnInspection(
                        contract,
                        request.result(),
                        request.note(),
                        now
                )));

        if (request.result() != ReturnInspectionResult.REVIEW_REQUIRED) {
            RentalProduct product = rentalProductRepository.findByIdForUpdate(
                            contract.getRentalProduct().getId()
                    )
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Rental product not found: " + contract.getRentalProduct().getId()
                    ));

            boolean maintenanceRequired =
                    request.result() == ReturnInspectionResult.NEW_DAMAGE_CONFIRMED;

            product.completeInspection(maintenanceRequired);
            contract.completeInspection(now);
        }

        return ReturnInspectionResponse.from(inspection);
    }

    @Transactional(readOnly = true)
    public ReturnInspectionResponse getInspection(Long rentalContractId) {
        return returnInspectionRepository.findByRentalContractId(rentalContractId)
                .map(ReturnInspectionResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Return inspection not found for rental: " + rentalContractId
                ));
    }

    private void validateResult(ReturnInspectionResult result, boolean hasNewDamage) {
        if (result == ReturnInspectionResult.NO_NEW_DAMAGE && hasNewDamage) {
            throw new IllegalArgumentException("NO_NEW_DAMAGE cannot be selected when NEW damage exists");
        }

        if (result == ReturnInspectionResult.NEW_DAMAGE_CONFIRMED && !hasNewDamage) {
            throw new IllegalArgumentException("NEW_DAMAGE_CONFIRMED requires at least one NEW damage");
        }
    }
}
