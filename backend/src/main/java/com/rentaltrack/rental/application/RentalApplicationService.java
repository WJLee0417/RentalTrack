package com.rentaltrack.rental.application;

import com.rentaltrack.global.error.ResourceNotFoundException;
import com.rentaltrack.location.domain.RentalLocation;
import com.rentaltrack.location.repository.RentalLocationRepository;
import com.rentaltrack.member.domain.Member;
import com.rentaltrack.member.domain.MemberStatus;
import com.rentaltrack.member.repository.MemberRepository;
import com.rentaltrack.product.domain.RentalProduct;
import com.rentaltrack.product.repository.RentalProductRepository;
import com.rentaltrack.rental.api.PickupRequest;
import com.rentaltrack.rental.api.RentalRequest;
import com.rentaltrack.rental.api.RentalResponse;
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

    public RentalApplicationService(
            MemberRepository memberRepository,
            RentalLocationRepository rentalLocationRepository,
            RentalProductRepository rentalProductRepository,
            RentalContractRepository rentalContractRepository
    ) {
        this.memberRepository = memberRepository;
        this.rentalLocationRepository = rentalLocationRepository;
        this.rentalProductRepository = rentalProductRepository;
        this.rentalContractRepository = rentalContractRepository;
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

        RentalContract saved = rentalContractRepository.save(contract);
        return RentalResponse.from(saved);
    }

    @Transactional
    public RentalResponse pickup(Long rentalContractId, PickupRequest request) {
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

        if (!request.dueAt().isAfter(now)) {
            throw new IllegalArgumentException("dueAt must be after pickup time");
        }

        product.startRental();
        contract.startRental(now, request.dueAt());

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
