package com.rentaltrack.auth.application;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtTokenProvider {

  private final SecretKey secretKey;
  private final long accessTokenSeconds;

  public JwtTokenProvider(
    @Value("${rentaltrack.auth.jwt.secret-base64}") String secretBase64,
    @Value(
      "${rentaltrack.auth.jwt.access-token-seconds}"
    ) long accessTokenSeconds
  ) {
    this.secretKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secretBase64));
    this.accessTokenSeconds = accessTokenSeconds;
  }

  public String issue(Long memberId) {
    Instant now = Instant.now();
    return Jwts.builder()
      .issuer("rentaltrack-erd-v2")
      .subject(memberId.toString())
      .issuedAt(Date.from(now))
      .expiration(Date.from(now.plusSeconds(accessTokenSeconds)))
      .signWith(secretKey)
      .compact();
  }

  public Long parseMemberId(String token) {
    try {
      Claims claims = Jwts.parser()
        .verifyWith(secretKey)
        .requireIssuer("rentaltrack-erd-v2")
        .build()
        .parseSignedClaims(token)
        .getPayload();

      return Long.valueOf(claims.getSubject());
    } catch (JwtException | IllegalArgumentException exception) {
      throw new IllegalArgumentException(
        "Invalid or expired access token",
        exception
      );
    }
  }

  public long accessTokenSeconds() {
    return accessTokenSeconds;
  }
}
