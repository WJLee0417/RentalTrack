package com.rentaltrack.access.application;

import com.rentaltrack.auth.domain.AuthenticatedMember;
import com.rentaltrack.global.error.AccessDeniedException;
import com.rentaltrack.global.error.ResourceNotFoundException;
import com.rentaltrack.member.domain.MemberRole;
import com.rentaltrack.rental.repository.RentalContractRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AccessControlService {

    private final RentalContractRepository rentalContractRepository;

    public AccessControlService(
            RentalContractRepository rentalContractRepository
    ) {
        this.rentalContractRepository = rentalContractRepository;
    }

    public void requireSelfOrOperator(
            AuthenticatedMember actor,
            Long memberId
    ) {
        if (actor.role() == MemberRole.OPERATOR) {
            return;
        }

        if (!actor.memberId().equals(memberId)) {
            throw new AccessDeniedException(
                    "Users can access only their own resources"
            );
        }
    }

    public void requireRentalOwnerOrOperator(
            AuthenticatedMember actor,
            Long rentalContractId
    ) {
        if (actor.role() == MemberRole.OPERATOR) {
            return;
        }

        Long ownerId = rentalContractRepository.findMemberIdById(rentalContractId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Rental contract not found: " + rentalContractId
                ));

        if (!actor.memberId().equals(ownerId)) {
            throw new AccessDeniedException(
                    "Users can access only their own rental contracts"
            );
        }
    }

    public void requireOperator(AuthenticatedMember actor) {
        if (actor.role() != MemberRole.OPERATOR) {
            throw new AccessDeniedException("Operator role is required");
        }
    }
}
