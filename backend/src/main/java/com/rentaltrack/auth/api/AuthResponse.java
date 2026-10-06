package com.rentaltrack.auth.api;

import com.rentaltrack.member.domain.Member;

public record AuthResponse(
        String accessToken,
        String tokenType,
        long expiresIn,
        Long memberId,
        String email,
        String name,
        String role
) {
    public static AuthResponse of(
            String accessToken,
            long expiresIn,
            Member member
    ) {
        return new AuthResponse(
                accessToken,
                "Bearer",
                expiresIn,
                member.getId(),
                member.getEmail(),
                member.getName(),
                member.getRole().name()
        );
    }
}
