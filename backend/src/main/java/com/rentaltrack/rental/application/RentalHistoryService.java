package com.rentaltrack.rental.application;

import com.rentaltrack.global.api.PageResponse;
import com.rentaltrack.global.error.ResourceNotFoundException;
import com.rentaltrack.member.repository.MemberRepository;
import com.rentaltrack.rental.api.RentalHistoryItemResponse;
import com.rentaltrack.rental.domain.RentalContract;
import com.rentaltrack.rental.domain.RentalContractStatus;
import com.rentaltrack.rental.repository.RentalContractRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class RentalHistoryService {

    private static final int MAX_PAGE_SIZE = 100;

    private final MemberRepository memberRepository;
    private final RentalContractRepository rentalContractRepository;

    public RentalHistoryService(
            MemberRepository memberRepository,
            RentalContractRepository rentalContractRepository
    ) {
        this.memberRepository = memberRepository;
        this.rentalContractRepository = rentalContractRepository;
    }

    public PageResponse<RentalHistoryItemResponse> getMemberRentalHistory(
            Long memberId,
            RentalContractStatus status,
            int page,
            int size
    ) {
        validatePageRequest(page, size);

        if (!memberRepository.existsById(memberId)) {
            throw new ResourceNotFoundException("Member not found: " + memberId);
        }

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(
                        Sort.Order.desc("requestedAt"),
                        Sort.Order.desc("id")
                )
        );

        Page<RentalContract> rentals = status == null
                ? rentalContractRepository.findAllByMember_Id(memberId, pageable)
                : rentalContractRepository.findAllByMember_IdAndStatus(memberId, status, pageable);

        return PageResponse.from(rentals, RentalHistoryItemResponse::from);
    }

    private void validatePageRequest(int page, int size) {
        if (page < 0) {
            throw new IllegalArgumentException("page must be 0 or greater");
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("size must be between 1 and " + MAX_PAGE_SIZE);
        }
    }
}
