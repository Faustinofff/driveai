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
 *
 * <p>Validaciones aplicadas al parsear: firma, algoritmo HMAC (rechaza
 * {@code alg=none}), expiración e issuer. El secreto proviene de
 * {@code app.jwt.secret}; si no se configura en desarrollo se genera uno
 * aleatorio (los tokens dejan de valer al reiniciar). En producción el
 * arranque exige {@code JWT_SECRET} mediante {@code application.yml}.
 */
@Component
public class JwtService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final String DEFAULT_ISSUER = "driveai";

    private final SecretKey key;
    private final long expirationMillis;
    private final String issuer;

    public JwtService(AppProperties properties) {
        byte[] secretBytes = decodeSecret(properties.jwt().secret());
        this.key = Keys.hmacShaKeyFor(secretBytes);
        this.expirationMillis = properties.jwt().expirationMinutes() * 60_000L;
        this.issuer = properties.jwt().issuer() == null || properties.jwt().issuer().isBlank()
                ? DEFAULT_ISSUER
                : properties.jwt().issuer().trim();
    }

    public String generateToken(Long userId, String email, String role) {
        Date issuedAt = new Date();
        Date expiresAt = new Date(issuedAt.getTime() + expirationMillis);
        return Jwts.builder()
                .issuer(issuer)
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

    /**
     * Parsea y valida el token. Lanza {@link RuntimeException} (jjwt) si la
     * firma, el algoritmo, el issuer o la expiración no son válidos.
     */
    public Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .requireIssuer(issuer)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String issuer() {
        return issuer;
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
