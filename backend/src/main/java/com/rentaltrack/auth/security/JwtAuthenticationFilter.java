package com.rentaltrack.auth.security;

import tools.jackson.databind.ObjectMapper;
import com.rentaltrack.auth.application.JwtTokenProvider;
import com.rentaltrack.auth.domain.AuthenticatedMember;
import com.rentaltrack.global.error.ApiErrorResponse;
import com.rentaltrack.member.domain.MemberStatus;
import com.rentaltrack.member.repository.MemberRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final MemberRepository memberRepository;
    private final ObjectMapper objectMapper;

    public JwtAuthenticationFilter(
            JwtTokenProvider jwtTokenProvider,
            MemberRepository memberRepository,
            ObjectMapper objectMapper
    ) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.memberRepository = memberRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String authorization = request.getHeader("Authorization");

        if (authorization == null || !authorization.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            String token = authorization.substring(7).trim();
            Long memberId = jwtTokenProvider.parseMemberId(token);

            var member = memberRepository.findById(memberId)
                    .filter(found -> found.getStatus() == MemberStatus.ACTIVE)
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Authenticated member is not active"
                    ));

            var principal = new AuthenticatedMember(
                    member.getId(),
                    member.getEmail(),
                    member.getName(),
                    member.getRole()
            );

            var authentication = new UsernamePasswordAuthenticationToken(
                    principal,
                    null,
                    List.of(new SimpleGrantedAuthority(
                            "ROLE_" + member.getRole().name()
                    ))
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);
            filterChain.doFilter(request, response);
        } catch (RuntimeException exception) {
            SecurityContextHolder.clearContext();
            writeUnauthorized(response, request);
        }
    }

    private void writeUnauthorized(
            HttpServletResponse response,
            HttpServletRequest request
    ) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        objectMapper.writeValue(
                response.getWriter(),
                new ApiErrorResponse(
                        "UNAUTHORIZED",
                        "Invalid or expired access token",
                        request.getRequestURI(),
                        LocalDateTime.now()
                )
        );
    }
}
