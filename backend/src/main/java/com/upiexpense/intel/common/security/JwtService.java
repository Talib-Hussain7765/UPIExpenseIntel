package com.upiexpense.intel.common.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

    private final SecretKey secretKey;
    private final long expirationMinutes;

    public JwtService(
            @Value("${app.security.jwt-secret}") String jwtSecret,
            @Value("${app.security.jwt-expiration-minutes}") long expirationMinutes
    ) {
        this.secretKey = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        this.expirationMinutes = expirationMinutes;
    }

    public String generateToken(CurrentUser user) {
        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(expiresInSeconds());

        return Jwts.builder()
                .subject(user.email())
                .claim("userId", user.id().toString())
                .claim("fullName", user.fullName())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .signWith(secretKey)
                .compact();
    }

    public String extractEmail(String token) {
        return claims(token).getSubject();
    }

    public boolean isValid(String token, CurrentUser user) {
        String email = extractEmail(token);
        return user.email().equalsIgnoreCase(email) && claims(token).getExpiration().after(new Date());
    }

    public long expiresInSeconds() {
        return expirationMinutes * 60;
    }

    private Claims claims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
