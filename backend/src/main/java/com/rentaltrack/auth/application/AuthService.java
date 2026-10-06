package com.rentaltrack.auth.application;

import com.rentaltrack.auth.api.AuthResponse;
import com.rentaltrack.auth.api.LoginRequest;
import com.rentaltrack.auth.api.RegisterRequest;
import com.rentaltrack.global.error.AuthenticationRequiredException;
import com.rentaltrack.member.domain.Member;
import com.rentaltrack.member.domain.MemberStatus;
import com.rentaltrack.member.repository.MemberRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
public class AuthService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthService(
            MemberRepository memberRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenProvider jwtTokenProvider
    ) {
        this.memberRepository = memberRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.email());

        if (memberRepository.existsByEmailIgnoreCase(email)) {
            throw new IllegalStateException("Email is already registered");
        }

        Member member = memberRepository.save(new Member(
                email,
                request.name().trim(),
                passwordEncoder.encode(request.password())
        ));

        return issue(member);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        Member member = memberRepository.findByEmailIgnoreCase(normalizeEmail(request.email()))
                .orElseThrow(() -> new AuthenticationRequiredException(
                        "Email or password is invalid"
                ));

        if (member.getStatus() != MemberStatus.ACTIVE) {
            throw new AuthenticationRequiredException("Member is not active");
        }

        if (member.getPasswordHash() == null
                || !passwordEncoder.matches(request.password(), member.getPasswordHash())) {
            throw new AuthenticationRequiredException("Email or password is invalid");
        }

        return issue(member);
    }

    private AuthResponse issue(Member member) {
        return AuthResponse.of(
                jwtTokenProvider.issue(member.getId()),
                jwtTokenProvider.accessTokenSeconds(),
                member
        );
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
