package com.ssafy.billisan.admin.service;

import com.ssafy.billisan.admin.domain.AdminAccount;
import com.ssafy.billisan.admin.dto.AdminLoginRequest;
import com.ssafy.billisan.admin.dto.AdminLoginResponse;
import com.ssafy.billisan.admin.repository.AdminAccountRepository;
import com.ssafy.billisan.global.exception.InvalidAdminCredentialsException;
import com.ssafy.billisan.global.security.JwtProvider;
import com.ssafy.billisan.global.security.Role;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;

@Service
public class AdminAuthService {

    private final AdminAccountRepository adminAccountRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final long idleExpirationSeconds;
    private final long absoluteExpirationSeconds;

    public AdminAuthService(
            AdminAccountRepository adminAccountRepository,
            PasswordEncoder passwordEncoder,
            JwtProvider jwtProvider,
            @Value("${jwt.admin.idle-expiration-seconds}") long idleExpirationSeconds,
            @Value("${jwt.admin.absolute-expiration-seconds}") long absoluteExpirationSeconds) {
        this.adminAccountRepository = adminAccountRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtProvider = jwtProvider;
        this.idleExpirationSeconds = idleExpirationSeconds;
        this.absoluteExpirationSeconds = absoluteExpirationSeconds;
    }

    public AdminLoginResponse login(AdminLoginRequest request) {
        AdminAccount admin = adminAccountRepository.findByLoginId(request.loginId())
                .orElseThrow(() -> new InvalidAdminCredentialsException("아이디 또는 비밀번호가 올바르지 않습니다."));

        if (!passwordEncoder.matches(request.password(), admin.getPasswordHash())) {
            throw new InvalidAdminCredentialsException("아이디 또는 비밀번호가 올바르지 않습니다.");
        }

        Instant now = Instant.now();
        Instant idleExpiresAt = now.plusSeconds(idleExpirationSeconds);
        Instant absoluteExpiresAt = now.plusSeconds(absoluteExpirationSeconds);

        String token = jwtProvider.generateToken(
                admin.getAdminId().toString(),
                Map.of(Role.CLAIM_KEY, Role.ADMIN.claimValue()),
                absoluteExpirationSeconds);

        return AdminLoginResponse.of(token, admin.getAdminId(), admin.getLoginId(), idleExpiresAt, absoluteExpiresAt);
    }
}
