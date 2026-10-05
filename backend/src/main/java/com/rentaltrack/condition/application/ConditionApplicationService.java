package com.rentaltrack.condition.application;

import com.rentaltrack.condition.api.BeforeConditionRequest;
import com.rentaltrack.condition.api.ConditionImageRequest;
import com.rentaltrack.condition.api.ConditionImageResponse;
import com.rentaltrack.condition.api.ConditionRecordResponse;
import com.rentaltrack.condition.domain.ConditionImage;
import com.rentaltrack.condition.domain.ConditionRecord;
import com.rentaltrack.condition.domain.ConditionRecordType;
import com.rentaltrack.condition.repository.ConditionImageRepository;
import com.rentaltrack.condition.repository.ConditionRecordRepository;
import com.rentaltrack.global.error.ResourceNotFoundException;
import com.rentaltrack.rental.domain.RentalContract;
import com.rentaltrack.rental.domain.RentalContractStatus;
import com.rentaltrack.rental.repository.RentalContractRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ConditionApplicationService {

    private final RentalContractRepository rentalContractRepository;
    private final ConditionRecordRepository conditionRecordRepository;
    private final ConditionImageRepository conditionImageRepository;

    public ConditionApplicationService(
            RentalContractRepository rentalContractRepository,
            ConditionRecordRepository conditionRecordRepository,
            ConditionImageRepository conditionImageRepository
    ) {
        this.rentalContractRepository = rentalContractRepository;
        this.conditionRecordRepository = conditionRecordRepository;
        this.conditionImageRepository = conditionImageRepository;
    }

    @Transactional
    public ConditionRecordResponse createBeforeCondition(
            Long rentalContractId,
            BeforeConditionRequest request
    ) {
        RentalContract contract = rentalContractRepository.findByIdForUpdate(rentalContractId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Rental contract not found: " + rentalContractId
                ));

        requirePickedUp(contract);

        if (conditionRecordRepository.existsByRentalContractIdAndRecordType(
                rentalContractId,
                ConditionRecordType.BEFORE_RENTAL
        )) {
            throw new IllegalStateException("BEFORE_RENTAL condition already exists");
        }

        ConditionRecord record = new ConditionRecord(
                contract,
                ConditionRecordType.BEFORE_RENTAL,
                contract.getMember(),
                request.summary(),
                LocalDateTime.now()
        );

        ConditionRecord saved = conditionRecordRepository.save(record);
        return ConditionRecordResponse.from(saved, List.of());
    }

    @Transactional
    public ConditionImageResponse addBeforeConditionImage(
            Long rentalContractId,
            ConditionImageRequest request
    ) {
        RentalContract contract = rentalContractRepository.findByIdForUpdate(rentalContractId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Rental contract not found: " + rentalContractId
                ));

        requirePickedUp(contract);

        ConditionRecord record = conditionRecordRepository.findByRentalContractIdAndRecordType(
                        rentalContractId,
                        ConditionRecordType.BEFORE_RENTAL
                )
                .orElseThrow(() -> new IllegalStateException(
                        "Create BEFORE_RENTAL condition before adding images"
                ));

        ConditionImage image = new ConditionImage(
                record,
                request.imageUrl(),
                request.storageKey(),
                request.captureOrder(),
                request.viewpoint(),
                request.capturedAt()
        );

        return ConditionImageResponse.from(conditionImageRepository.save(image));
    }

    @Transactional(readOnly = true)
    public ConditionRecordResponse getBeforeCondition(Long rentalContractId) {
        ConditionRecord record = conditionRecordRepository.findByRentalContractIdAndRecordType(
                        rentalContractId,
                        ConditionRecordType.BEFORE_RENTAL
                )
                .orElseThrow(() -> new ResourceNotFoundException(
                        "BEFORE_RENTAL condition not found for rental: " + rentalContractId
                ));

        List<ConditionImage> images =
                conditionImageRepository.findAllByConditionRecordIdOrderByCaptureOrderAsc(record.getId());

        return ConditionRecordResponse.from(record, images);
    }

    private void requirePickedUp(RentalContract contract) {
        if (contract.getStatus() != RentalContractStatus.PICKED_UP) {
            throw new IllegalStateException(
                    "BEFORE_RENTAL condition can be recorded only after pickup and before actual use"
            );
        }
    }
}
