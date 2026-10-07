package com.rentaltrack.damage.application;

import com.rentaltrack.condition.domain.ConditionRecord;
import com.rentaltrack.condition.domain.ConditionRecordType;
import com.rentaltrack.condition.repository.ConditionImageRepository;
import com.rentaltrack.condition.repository.ConditionRecordRepository;
import com.rentaltrack.damage.api.DamageCreateRequest;
import com.rentaltrack.damage.api.DamageResponse;
import com.rentaltrack.damage.domain.DamageComparisonResult;
import com.rentaltrack.damage.domain.DamageRecord;
import com.rentaltrack.damage.repository.DamageRecordRepository;
import com.rentaltrack.global.error.ResourceNotFoundException;
import com.rentaltrack.rental.domain.RentalContract;
import com.rentaltrack.rental.domain.RentalContractStatus;
import com.rentaltrack.rental.repository.RentalContractRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class DamageApplicationService {

    private final RentalContractRepository rentalContractRepository;
    private final ConditionRecordRepository conditionRecordRepository;
    private final ConditionImageRepository conditionImageRepository;
    private final DamageRecordRepository damageRecordRepository;

    public DamageApplicationService(
            RentalContractRepository rentalContractRepository,
            ConditionRecordRepository conditionRecordRepository,
            ConditionImageRepository conditionImageRepository,
            DamageRecordRepository damageRecordRepository
    ) {
        this.rentalContractRepository = rentalContractRepository;
        this.conditionRecordRepository = conditionRecordRepository;
        this.conditionImageRepository = conditionImageRepository;
        this.damageRecordRepository = damageRecordRepository;
    }

    @Transactional
    public DamageResponse addBeforeDamage(Long rentalContractId, DamageCreateRequest request) {
        RentalContract contract = lockedContract(rentalContractId);
        if (contract.getStatus() != RentalContractStatus.PICKED_UP) {
            throw new IllegalStateException("BEFORE damage can be recorded only before actual use");
        }

        ConditionRecord before = condition(rentalContractId, ConditionRecordType.BEFORE_RENTAL);
        return DamageResponse.from(damageRecordRepository.save(new DamageRecord(
                before,
                request.damageType(),
                normalizeLocation(request.damageLocation()),
                request.severity(),
                request.description(),
                DamageComparisonResult.BASELINE
        )));
    }

    @Transactional
    public DamageResponse addAfterDamage(Long rentalContractId, DamageCreateRequest request) {
        RentalContract contract = lockedContract(rentalContractId);
        if (contract.getStatus() != RentalContractStatus.RETURNED) {
            throw new IllegalStateException("AFTER damage can be recorded only after return");
        }

        ConditionRecord after = condition(rentalContractId, ConditionRecordType.AFTER_RENTAL);
        return DamageResponse.from(damageRecordRepository.save(new DamageRecord(
                after,
                request.damageType(),
                normalizeLocation(request.damageLocation()),
                request.severity(),
                request.description(),
                DamageComparisonResult.UNRESOLVED
        )));
    }

    @Transactional
    public List<DamageResponse> compare(Long rentalContractId) {
        RentalContract contract = lockedContract(rentalContractId);
        if (contract.getStatus() != RentalContractStatus.RETURNED) {
            throw new IllegalStateException("Damage comparison can start only after return");
        }

        ConditionRecord before = condition(rentalContractId, ConditionRecordType.BEFORE_RENTAL);
        ConditionRecord after = condition(rentalContractId, ConditionRecordType.AFTER_RENTAL);

        if (conditionImageRepository.countByConditionRecordId(after.getId()) == 0) {
            throw new IllegalStateException("At least one AFTER_RENTAL image is required before comparison");
        }

        List<DamageRecord> beforeDamages =
                damageRecordRepository.findAllByConditionRecordIdOrderByIdAsc(before.getId());
        List<DamageRecord> afterDamages =
                damageRecordRepository.findAllByConditionRecordIdOrderByIdAsc(after.getId());

        List<Long> usedBeforeIds = new ArrayList<>();

        for (DamageRecord afterDamage : afterDamages) {
            if (afterDamage.getComparisonResult() != DamageComparisonResult.UNRESOLVED) {
                continue;
            }

            DamageRecord matched = beforeDamages.stream()
                    .filter(beforeDamage -> beforeDamage.getComparisonResult() == DamageComparisonResult.BASELINE)
                    .filter(beforeDamage -> !usedBeforeIds.contains(beforeDamage.getId()))
                    .filter(beforeDamage -> sameDamage(beforeDamage, afterDamage))
                    .findFirst()
                    .orElse(null);

            if (matched == null) {
                afterDamage.markNew();
            } else {
                afterDamage.markExisting(matched);
                usedBeforeIds.add(matched.getId());
            }
        }

        contract.beginInspection();

        return afterDamages.stream()
                .map(DamageResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<DamageResponse> getAfterDamages(Long rentalContractId) {
        ConditionRecord after = condition(rentalContractId, ConditionRecordType.AFTER_RENTAL);
        return damageRecordRepository.findAllByConditionRecordIdOrderByIdAsc(after.getId()).stream()
                .map(DamageResponse::from)
                .toList();
    }

    private boolean sameDamage(DamageRecord before, DamageRecord after) {
        return before.getDamageType() == after.getDamageType()
                && before.getDamageLocation().equalsIgnoreCase(after.getDamageLocation());
    }

    private String normalizeLocation(String location) {
        return location.trim().toUpperCase();
    }

    private ConditionRecord condition(Long rentalContractId, ConditionRecordType type) {
        return conditionRecordRepository.findByRentalContractIdAndRecordType(rentalContractId, type)
                .orElseThrow(() -> new ResourceNotFoundException(
                        type + " condition not found for rental: " + rentalContractId
                ));
    }

    private RentalContract lockedContract(Long rentalContractId) {
        return rentalContractRepository.findByIdForUpdate(rentalContractId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Rental contract not found: " + rentalContractId
                ));
    }
}
