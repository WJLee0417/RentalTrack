package com.ssafy.billisan.user.dto;

import com.ssafy.billisan.user.domain.UserAccount;
import java.util.List;
import java.util.UUID;

public record AuthResultResponse(
        UUID userRef,
        String userId,
        String name,
        boolean eligible,
        List<String> blockingReasons) {

    public static AuthResultResponse of(UserAccount user) {
        return new AuthResultResponse(
                user.getUserRef(),
                user.getUserId(),
                user.getName(),
                user.isRentalEligible(),
                List.of());
    }
}
