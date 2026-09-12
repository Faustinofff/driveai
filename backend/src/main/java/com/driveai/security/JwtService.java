package com.driveai.security;

import com.driveai.config.AppProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Date;

/**
 * Emite y valida tokens JWT (firma HMAC; con secreto ≥48 bytes jjwt emplea HS384).
 * El secreto proviene de {@code app.jwt.secret}; si no se configura, se genera
 * uno aleatorio (los tokens dejan de valer al reiniciar).
 */
@Component
public class JwtService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final SecretKey key;
    private final long expirationMillis;

    public JwtService(AppProperties properties) {
        byte[] secretBytes = decodeSecret(properties.jwt().secret());
        this.key = Keys.hmacShaKeyFor(secretBytes);
        this.expirationMillis = properties.jwt().expirationMinutes() * 60_000L;
    }

    public String generateToken(Long userId, String email, String role) {
        Date issuedAt = new Date();
        Date expiresAt = new Date(issuedAt.getTime() + expirationMillis);
        return Jwts.builder()
                .subject(email)
                .claim("uid", userId)
                .claim("role", role)
                .issuedAt(issuedAt)
                .expiration(expiresAt)
                .signWith(key)
                .compact();
    }

    /**
     * @return el username (email) del token, o {@code null} si es inválido/expirado.
     */
    public String extractEmail(String token) {
        try {
            return parse(token).getSubject();
        } catch (RuntimeException ex) {
            return null;
        }
    }

    public Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private static byte[] decodeSecret(String secret) {
        if (secret == null || secret.isBlank()) {
            byte[] random = new byte[48];
            SECURE_RANDOM.nextBytes(random);
            return random;
        }
        byte[] bytes;
        try {
            bytes = Base64.getDecoder().decode(secret);
        } catch (IllegalArgumentException ex) {
            bytes = secret.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        }
        if (bytes.length < 32) {
            throw new IllegalArgumentException(
                    "JWT_SECRET debe tener al menos 32 bytes (codificalo en Base64).");
        }
        return bytes;
    }
}