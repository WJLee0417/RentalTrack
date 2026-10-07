package com.rentaltrack.rental.application;

import com.rentaltrack.condition.domain.ConditionRecordType;
import com.rentaltrack.condition.repository.ConditionImageRepository;
import com.rentaltrack.condition.repository.ConditionRecordRepository;
import com.rentaltrack.global.error.ResourceNotFoundException;
import com.rentaltrack.location.domain.RentalLocation;
import com.rentaltrack.location.repository.RentalLocationRepository;
import com.rentaltrack.member.domain.Member;
import com.rentaltrack.member.domain.MemberStatus;
import com.rentaltrack.member.repository.MemberRepository;
import com.rentaltrack.product.domain.RentalProduct;
import com.rentaltrack.product.repository.RentalProductRepository;
import com.rentaltrack.rental.api.RentalRequest;
import com.rentaltrack.rental.api.RentalResponse;
import com.rentaltrack.rental.api.ReturnRequest;
import com.rentaltrack.rental.api.StartRentalRequest;
import com.rentaltrack.rental.domain.RentalContract;
import com.rentaltrack.rental.repository.RentalContractRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class RentalApplicationService {

    private final MemberRepository memberRepository;
    private final RentalLocationRepository rentalLocationRepository;
    private final RentalProductRepository rentalProductRepository;
    private final RentalContractRepository rentalContractRepository;
    private final ConditionRecordRepository conditionRecordRepository;
    private final ConditionImageRepository conditionImageRepository;

    public RentalApplicationService(
            MemberRepository memberRepository,
            RentalLocationRepository rentalLocationRepository,
            RentalProductRepository rentalProductRepository,
            RentalContractRepository rentalContractRepository,
            ConditionRecordRepository conditionRecordRepository,
            ConditionImageRepository conditionImageRepository
    ) {
        this.memberRepository = memberRepository;
        this.rentalLocationRepository = rentalLocationRepository;
        this.rentalProductRepository = rentalProductRepository;
        this.rentalContractRepository = rentalContractRepository;
        this.conditionRecordRepository = conditionRecordRepository;
        this.conditionImageRepository = conditionImageRepository;
    }

    @Transactional
    public RentalResponse requestRental(RentalRequest request) {
        Member member = memberRepository.findById(request.memberId())
                .orElseThrow(() -> new ResourceNotFoundException("Member not found: " + request.memberId()));

        if (member.getStatus() != MemberStatus.ACTIVE) {
            throw new IllegalStateException("Only ACTIVE members can request a rental");
        }

        RentalLocation pickupLocation = rentalLocationRepository.findById(request.pickupLocationId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Rental location not found: " + request.pickupLocationId()
                ));

        if (!pickupLocation.isActive()) {
            throw new IllegalStateException("Inactive rental location cannot be used for pickup");
        }

        RentalProduct product = rentalProductRepository.findByIdForUpdate(request.rentalProductId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Rental product not found: " + request.rentalProductId()
                ));

        if (!product.getCurrentLocation().getId().equals(pickupLocation.getId())) {
            throw new IllegalArgumentException("Product is not stored at the requested pickup location");
        }

        if (!product.getCategory().isActive()) {
            throw new IllegalStateException("Inactive product category cannot be rented");
        }

        LocalDateTime now = LocalDateTime.now();

        product.reserve();

        RentalContract contract = new RentalContract(
                newContractCode(),
                member,
                product,
                pickupLocation,
                now
        );
        contract.approve(now);

        return RentalResponse.from(rentalContractRepository.save(contract));
    }

    @Transactional
    public RentalResponse pickup(Long rentalContractId) {
        RentalContract contract = rentalContractRepository.findByIdForUpdate(rentalContractId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Rental contract not found: " + rentalContractId
                ));

        RentalProduct product = rentalProductRepository.findByIdForUpdate(
                        contract.getRentalProduct().getId()
                )
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Rental product not found: " + contract.getRentalProduct().getId()
                ));

        LocalDateTime now = LocalDateTime.now();

        product.markPickedUp();
        contract.markPickedUp(now);

        return RentalResponse.from(contract);
    }

    @Transactional
    public RentalResponse startActualUse(Long rentalContractId, StartRentalRequest request) {
        RentalContract contract = rentalContractRepository.findByIdForUpdate(rentalContractId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Rental contract not found: " + rentalContractId
                ));

        var beforeRecord = conditionRecordRepository.findByRentalContractIdAndRecordType(
                        rentalContractId,
                        ConditionRecordType.BEFORE_RENTAL
                )
                .orElseThrow(() -> new IllegalStateException(
                        "BEFORE_RENTAL condition must be recorded before starting actual use"
                ));

        if (conditionImageRepository.countByConditionRecordId(beforeRecord.getId()) == 0) {
            throw new IllegalStateException(
                    "At least one BEFORE_RENTAL image is required before starting actual use"
            );
        }

        LocalDateTime now = LocalDateTime.now();
        contract.startRental(now, request.dueAt());

        return RentalResponse.from(contract);
    }

    @Transactional
    public RentalResponse returnRental(Long rentalContractId, ReturnRequest request) {
        RentalContract contract = rentalContractRepository.findByIdForUpdate(rentalContractId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Rental contract not found: " + rentalContractId
                ));

        RentalLocation returnLocation = rentalLocationRepository.findById(request.returnLocationId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Rental location not found: " + request.returnLocationId()
                ));

        if (!returnLocation.isActive()) {
            throw new IllegalStateException("Inactive rental location cannot accept returns");
        }

        RentalProduct product = rentalProductRepository.findByIdForUpdate(
                        contract.getRentalProduct().getId()
                )
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Rental product not found: " + contract.getRentalProduct().getId()
                ));

        LocalDateTime now = LocalDateTime.now();
        contract.markReturned(returnLocation, now);
        product.markReturned(returnLocation);

        return RentalResponse.from(contract);
    }

    @Transactional(readOnly = true)
    public RentalResponse getRental(Long rentalContractId) {
        RentalContract contract = rentalContractRepository.findDetailById(rentalContractId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Rental contract not found: " + rentalContractId
                ));

        return RentalResponse.from(contract);
    }

    private String newContractCode() {
        return "RT-" + UUID.randomUUID()
                .toString()
                .replace("-", "")
                .substring(0, 20)
                .toUpperCase();
    }
}
