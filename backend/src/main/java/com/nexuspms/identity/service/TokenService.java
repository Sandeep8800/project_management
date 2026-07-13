package com.nexuspms.identity.service;

import com.nexuspms.identity.domain.RefreshToken;
import com.nexuspms.identity.domain.User;
import com.nexuspms.identity.repository.RefreshTokenRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

/**
 * HLD S6/S7: short-lived JWT access token + refresh-token rotation. No server-side
 * sticky sessions -- app tier stays stateless (HLD S10). Refresh tokens are stored
 * hashed, never raw (Database Design S4.4).
 */
@Service
public class TokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final SecretKey signingKey;
    private final long accessTokenTtlMinutes;
    private final long refreshTokenTtlDays;

    public TokenService(RefreshTokenRepository refreshTokenRepository,
                         @Value("${nexus.jwt.secret}") String jwtSecret,
                         @Value("${nexus.jwt.access-token-ttl-minutes}") long accessTokenTtlMinutes,
                         @Value("${nexus.jwt.refresh-token-ttl-days}") long refreshTokenTtlDays) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.signingKey = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenTtlMinutes = accessTokenTtlMinutes;
        this.refreshTokenTtlDays = refreshTokenTtlDays;
    }

    public String issueAccessToken(User user) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(user.getId().toString())
                .claim("email", user.getEmail())
                .claim("defaultRole", user.getDefaultRole())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(accessTokenTtlMinutes, ChronoUnit.MINUTES)))
                .signWith(signingKey, SignatureAlgorithm.HS256)
                .compact();
    }

    public String issueRefreshToken(User user) {
        String rawToken = UUID.randomUUID() + "." + UUID.randomUUID();
        Instant expiresAt = Instant.now().plus(refreshTokenTtlDays, ChronoUnit.DAYS);
        refreshTokenRepository.save(new RefreshToken(user.getId(), hash(rawToken), expiresAt));
        return rawToken;
    }

    public UUID parseSubject(String accessToken) {
        String subject = Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(accessToken)
                .getPayload()
                .getSubject();
        return UUID.fromString(subject);
    }

    public RefreshToken findValidRefreshToken(String rawToken) {
        return refreshTokenRepository.findByTokenHash(hash(rawToken))
                .filter(RefreshToken::isValid)
                .orElse(null);
    }

    public void revoke(RefreshToken token) {
        token.revoke();
        refreshTokenRepository.save(token);
    }

    private String hash(String raw) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return Base64.getEncoder().encodeToString(digest.digest(raw.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
