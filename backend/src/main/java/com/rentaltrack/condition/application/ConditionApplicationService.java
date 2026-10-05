package com.rentaltrack.condition.application;

import com.rentaltrack.condition.api.AfterConditionRequest;
import com.rentaltrack.condition.api.BeforeConditionRequest;
import com.rentaltrack.condition.api.ConditionImageMetadataRequest;
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
import com.rentaltrack.storage.application.ConditionImageStorageService;
import com.rentaltrack.storage.application.StoredImage;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ConditionApplicationService {

    private final RentalContractRepository rentalContractRepository;
    private final ConditionRecordRepository conditionRecordRepository;
    private final ConditionImageRepository conditionImageRepository;
    private final ConditionImageStorageService conditionImageStorageService;

    public ConditionApplicationService(
            RentalContractRepository rentalContractRepository,
            ConditionRecordRepository conditionRecordRepository,
            ConditionImageRepository conditionImageRepository,
            ConditionImageStorageService conditionImageStorageService
    ) {
        this.rentalContractRepository = rentalContractRepository;
        this.conditionRecordRepository = conditionRecordRepository;
        this.conditionImageRepository = conditionImageRepository;
        this.conditionImageStorageService = conditionImageStorageService;
    }

    @Transactional
    public ConditionRecordResponse createBeforeCondition(
            Long rentalContractId,
            BeforeConditionRequest request
    ) {
        RentalContract contract = lockedContract(rentalContractId);
        requireStatus(contract, RentalContractStatus.PICKED_UP,
                "BEFORE_RENTAL condition can be recorded only after pickup and before actual use");

        ConditionRecord saved = createCondition(
                contract,
                ConditionRecordType.BEFORE_RENTAL,
                request.summary()
        );
        return ConditionRecordResponse.from(saved, List.of());
    }

    @Transactional
    public ConditionRecordResponse createAfterCondition(
            Long rentalContractId,
            AfterConditionRequest request
    ) {
        RentalContract contract = lockedContract(rentalContractId);
        requireStatus(contract, RentalContractStatus.RETURNED,
                "AFTER_RENTAL condition can be recorded only after return");

        ConditionRecord saved = createCondition(
                contract,
                ConditionRecordType.AFTER_RENTAL,
                request.summary()
        );
        return ConditionRecordResponse.from(saved, List.of());
    }

    @Transactional
    public ConditionImageResponse addBeforeConditionImage(
            Long rentalContractId,
            ConditionImageMetadataRequest metadata,
            MultipartFile file
    ) {
        RentalContract contract = lockedContract(rentalContractId);
        requireStatus(contract, RentalContractStatus.PICKED_UP,
                "BEFORE_RENTAL images can be added only before actual use");
        return addImage(rentalContractId, ConditionRecordType.BEFORE_RENTAL, metadata, file);
    }

    @Transactional
    public ConditionImageResponse addAfterConditionImage(
            Long rentalContractId,
            ConditionImageMetadataRequest metadata,
            MultipartFile file
    ) {
        RentalContract contract = lockedContract(rentalContractId);
        requireStatus(contract, RentalContractStatus.RETURNED,
                "AFTER_RENTAL images can be added only before inspection begins");
        return addImage(rentalContractId, ConditionRecordType.AFTER_RENTAL, metadata, file);
    }

    @Transactional(readOnly = true)
    public ConditionRecordResponse getBeforeCondition(Long rentalContractId) {
        return getCondition(rentalContractId, ConditionRecordType.BEFORE_RENTAL);
    }

    @Transactional(readOnly = true)
    public ConditionRecordResponse getAfterCondition(Long rentalContractId) {
        return getCondition(rentalContractId, ConditionRecordType.AFTER_RENTAL);
    }

    @Transactional(readOnly = true)
    public byte[] loadImage(Long rentalContractId, Long imageId) {
        ConditionImage image = conditionImageRepository.findById(imageId)
                .orElseThrow(() -> new ResourceNotFoundException("Condition image not found: " + imageId));

        if (!image.getConditionRecord().getRentalContract().getId().equals(rentalContractId)) {
            throw new ResourceNotFoundException("Condition image not found for rental: " + rentalContractId);
        }

        return conditionImageStorageService.load(image.getStorageKey());
    }

    @Transactional(readOnly = true)
    public ConditionImage getImageMetadata(Long rentalContractId, Long imageId) {
        ConditionImage image = conditionImageRepository.findById(imageId)
                .orElseThrow(() -> new ResourceNotFoundException("Condition image not found: " + imageId));

        if (!image.getConditionRecord().getRentalContract().getId().equals(rentalContractId)) {
            throw new ResourceNotFoundException("Condition image not found for rental: " + rentalContractId);
        }

        return image;
    }

    private ConditionRecord createCondition(
            RentalContract contract,
            ConditionRecordType type,
            String summary
    ) {
        if (conditionRecordRepository.existsByRentalContractIdAndRecordType(
                contract.getId(),
                type
        )) {
            throw new IllegalStateException(type + " condition already exists");
        }

        return conditionRecordRepository.save(new ConditionRecord(
                contract,
                type,
                contract.getMember(),
                summary,
                LocalDateTime.now()
        ));
    }

    private ConditionImageResponse addImage(
            Long rentalContractId,
            ConditionRecordType type,
            ConditionImageMetadataRequest metadata,
            MultipartFile file
    ) {
        ConditionRecord record = conditionRecordRepository.findByRentalContractIdAndRecordType(
                        rentalContractId,
                        type
                )
                .orElseThrow(() -> new IllegalStateException(
                        "Create " + type + " condition before adding images"
                ));

        StoredImage stored = conditionImageStorageService.store(
                rentalContractId,
                type,
                file
        );

        ConditionImage image = new ConditionImage(
                record,
                stored.storageKey(),
                stored.originalFilename(),
                stored.contentType(),
                stored.fileSize(),
                metadata.captureOrder(),
                metadata.viewpoint(),
                metadata.capturedAt()
        );

        return ConditionImageResponse.from(conditionImageRepository.save(image));
    }

    private ConditionRecordResponse getCondition(
            Long rentalContractId,
            ConditionRecordType type
    ) {
        ConditionRecord record = conditionRecordRepository.findByRentalContractIdAndRecordType(
                        rentalContractId,
                        type
                )
                .orElseThrow(() -> new ResourceNotFoundException(
                        type + " condition not found for rental: " + rentalContractId
                ));

        List<ConditionImage> images =
                conditionImageRepository.findAllByConditionRecordIdOrderByCaptureOrderAsc(record.getId());

        return ConditionRecordResponse.from(record, images);
    }

    private RentalContract lockedContract(Long rentalContractId) {
        return rentalContractRepository.findByIdForUpdate(rentalContractId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Rental contract not found: " + rentalContractId
                ));
    }

    private void requireStatus(
            RentalContract contract,
            RentalContractStatus expected,
            String message
    ) {
        if (contract.getStatus() != expected) {
            throw new IllegalStateException(message);
        }
    }
}
