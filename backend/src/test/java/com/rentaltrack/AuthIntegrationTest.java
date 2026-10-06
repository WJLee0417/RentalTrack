package com.rentaltrack;

import com.rentaltrack.auth.api.LoginRequest;
import com.rentaltrack.auth.api.RegisterRequest;
import com.rentaltrack.auth.application.AuthService;
import com.rentaltrack.auth.application.JwtTokenProvider;
import com.rentaltrack.member.repository.MemberRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class AuthIntegrationTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @AfterEach
    void tearDown() {
        jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 0");
        jdbcTemplate.execute("TRUNCATE TABLE member_account");
        jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 1");
    }

    @Test
    void registerHashesPasswordAndReturnsJwt() {
        var response = authService.register(new RegisterRequest(
                "Operator@example.com",
                "password123!",
                "Operator Candidate"
        ));

        var saved = memberRepository.findById(response.memberId()).orElseThrow();

        assertThat(saved.getEmail()).isEqualTo("operator@example.com");
        assertThat(saved.getPasswordHash()).isNotEqualTo("password123!");
        assertThat(saved.getRole().name()).isEqualTo("USER");
        assertThat(jwtTokenProvider.parseMemberId(response.accessToken()))
                .isEqualTo(saved.getId());
    }

    @Test
    void loginReturnsJwtForCorrectPassword() {
        var registered = authService.register(new RegisterRequest(
                "user@example.com",
                "password123!",
                "User"
        ));

        var loggedIn = authService.login(new LoginRequest(
                "USER@example.com",
                "password123!"
        ));

        assertThat(jwtTokenProvider.parseMemberId(loggedIn.accessToken()))
                .isEqualTo(registered.memberId());
    }

    @Test
    void loginRejectsWrongPassword() {
        authService.register(new RegisterRequest(
                "user@example.com",
                "password123!",
                "User"
        ));

        assertThatThrownBy(() -> authService.login(new LoginRequest(
                "user@example.com",
                "wrong-password"
        ))).isInstanceOf(com.rentaltrack.global.error.AuthenticationRequiredException.class);
    }
}
