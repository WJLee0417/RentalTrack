package com.ssafy.billisan.settlement.service;

import com.ssafy.billisan.global.exception.UserNotFoundException;
import com.ssafy.billisan.settlement.domain.Settlement;
import com.ssafy.billisan.settlement.dto.SettlementPendingResponse;
import com.ssafy.billisan.settlement.repository.SettlementRepository;
import com.ssafy.billisan.user.domain.UserAccount;
import com.ssafy.billisan.user.repository.UserAccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/** 12-R SETTLEMENT-001 — 인증 사용자 본인의 미정산 조회 전용. */
@Service
@Transactional(readOnly = true)
public class SettlementAppService {

    private final UserAccountRepository userAccountRepository;
    private final SettlementRepository settlementRepository;

    public SettlementAppService(UserAccountRepository userAccountRepository, SettlementRepository settlementRepository) {
        this.userAccountRepository = userAccountRepository;
        this.settlementRepository = settlementRepository;
    }

    public SettlementPendingResponse pending(UUID userRef) {
        UserAccount user = requireUser(userRef);
        List<Settlement> pending =
                settlementRepository.findByUserIdAndStatusOrderByCreatedAtDesc(user.getUserId(), Settlement.Status.PENDING);

        long totalOutstanding = pending.stream()
                .mapToLong(s -> Math.max(s.getAmount() - s.getPaidAmount(), 0))
                .sum();

        List<SettlementPendingResponse.Item> items = pending.stream()
                .map(SettlementPendingResponse.Item::of)
                .toList();

        return new SettlementPendingResponse(items, totalOutstanding, totalOutstanding > 0);
    }

    private UserAccount requireUser(UUID userRef) {
        return userAccountRepository.findByUserRef(userRef)
                .orElseThrow(() -> new UserNotFoundException("userRef로 사용자를 찾을 수 없습니다: " + userRef));
    }
}
