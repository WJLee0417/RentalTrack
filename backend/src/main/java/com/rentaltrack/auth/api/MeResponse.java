package com.rentaltrack.auth.api;

import com.rentaltrack.auth.domain.AuthenticatedMember;

public record MeResponse(
        Long memberId,
        String email,
        String name,
        String role
) {
    public static MeResponse from(AuthenticatedMember member) {
        return new MeResponse(
                member.memberId(),
                member.email(),
                member.name(),
                member.role().name()
        );
    }
}
