package com.rentaltrack.auth.domain;

import com.rentaltrack.member.domain.MemberRole;

public record AuthenticatedMember(
        Long memberId,
        String email,
        String name,
        MemberRole role
) {
}
