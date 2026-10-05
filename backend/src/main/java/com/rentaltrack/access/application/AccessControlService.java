package com.rentaltrack.access.application;

import com.rentaltrack.global.error.AccessDeniedException;
import com.rentaltrack.global.error.AuthenticationRequiredException;
import com.rentaltrack.global.error.ResourceNotFoundException;
import com.rentaltrack.member.domain.Member;
import com.rentaltrack.member.domain.MemberRole;
import com.rentaltrack.member.domain.MemberStatus;
import com.rentaltrack.member.repository.MemberRepository;
import com.rentaltrack.rental.repository.RentalContractRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AccessControlService {

    private final MemberRepository memberRepository;
    private final RentalContractRepository rentalContractRepository;

    public AccessControlService(
            MemberRepository memberRepository,
            RentalContractRepository rentalContractRepository
    ) {
        this.memberRepository = memberRepository;
        this.rentalContractRepository = rentalContractRepository;
    }

    public void requireSelfOrOperator(Long actorId, Long memberId) {
        Member actor = activeActor(actorId);

        if (actor.getRole() == MemberRole.OPERATOR) {
            return;
        }

        if (!actor.getId().equals(memberId)) {
            throw new AccessDeniedException("Users can access only their own resources");
        }
    }

    public void requireRentalOwnerOrOperator(Long actorId, Long rentalContractId) {
        Member actor = activeActor(actorId);

        if (actor.getRole() == MemberRole.OPERATOR) {
            return;
        }

        Long ownerId = rentalContractRepository.findMemberIdById(rentalContractId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Rental contract not found: " + rentalContractId
                ));

        if (!actor.getId().equals(ownerId)) {
            throw new AccessDeniedException("Users can access only their own rental contracts");
        }
    }

    public void requireOperator(Long actorId) {
        Member actor = activeActor(actorId);

        if (actor.getRole() != MemberRole.OPERATOR) {
            throw new AccessDeniedException("Operator role is required");
        }
    }

    private Member activeActor(Long actorId) {
        if (actorId == null) {
            throw new AuthenticationRequiredException("X-Actor-Id header is required");
        }

        Member actor = memberRepository.findById(actorId)
                .orElseThrow(() -> new AuthenticationRequiredException("Unknown actor"));

        if (actor.getStatus() != MemberStatus.ACTIVE) {
            throw new AuthenticationRequiredException("Actor is not active");
        }

        return actor;
    }
}
