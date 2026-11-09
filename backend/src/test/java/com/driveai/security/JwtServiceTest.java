package com.driveai.security;

import com.driveai.config.AppProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private static final String SECRET = Base64.getEncoder()
            .encodeToString("0123456789abcdef0123456789abcdef0123456789abcdef".getBytes(StandardCharsets.UTF_8));

    private static JwtService service(String issuer, long minutes) {
        return new JwtService(new AppProperties(
                new AppProperties.Jwt(SECRET, minutes, issuer),
                new AppProperties.Admin("", ""),
                new AppProperties.Cors(java.util.List.of("http://localhost:5173"))));
    }

    @Test
    void generatedTokenContainsIdentityClaimsAndIssuer() {
        JwtService jwtService = service("driveai", 60);

        String token = jwtService.generateToken(7L, "ana@driveai.test", "CUSTOMER");
        Claims claims = jwtService.parse(token);

        assertThat(claims.getSubject()).isEqualTo("ana@driveai.test");
        assertThat(((Number) claims.get("uid")).longValue()).isEqualTo(7L);
        assertThat(claims.get("role", String.class)).isEqualTo("CUSTOMER");
        assertThat(claims.getIssuer()).isEqualTo("driveai");
        assertThat(claims.getExpiration()).isAfter(claims.getIssuedAt());
    }

    @Test
    void extractEmailReturnsNullForTamperedToken() {
        JwtService jwtService = service("driveai", 60);
        String token = jwtService.generateToken(7L, "ana@driveai.test", "CUSTOMER");
        String tampered = token.substring(0, token.length() - 2) + "xx";

        assertThat(jwtService.extractEmail(tampered)).isNull();
    }

    @Test
    void tokenWithWrongIssuerIsRejected() {
        JwtService issuerA = service("issuer-a", 60);
        JwtService issuerB = service("issuer-b", 60);
        String token = issuerA.generateToken(7L, "ana@driveai.test", "CUSTOMER");

        assertThatThrownBy(() -> issuerB.parse(token)).isInstanceOf(JwtException.class);
    }

    @Test
    void expiredTokenIsRejected() {
        JwtService jwtService = service("driveai", -1);
        String token = jwtService.generateToken(7L, "ana@driveai.test", "CUSTOMER");

        assertThatThrownBy(() -> jwtService.parse(token)).isInstanceOf(JwtException.class);
    }

    @Test
    void unsignedAlgNoneTokenIsRejected() {
        JwtService jwtService = service("driveai", 60);
        Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();
        String header = encoder.encodeToString("{\"alg\":\"none\",\"typ\":\"JWT\"}".getBytes(StandardCharsets.UTF_8));
        String payload = encoder.encodeToString(
                ("{\"sub\":\"ana@driveai.test\",\"iss\":\"driveai\",\"uid\":7,\"role\":\"ADMIN\"}")
                        .getBytes(StandardCharsets.UTF_8));
        String unsigned = header + "." + payload + ".";

        assertThatThrownBy(() -> jwtService.parse(unsigned)).isInstanceOf(JwtException.class);
    }

    @Test
    void shortSecretFailsFastWithClearMessage() {
        assertThatThrownBy(() -> new JwtService(new AppProperties(
                new AppProperties.Jwt("short", 60, "driveai"),
                new AppProperties.Admin("", ""),
                new AppProperties.Cors(java.util.List.of()))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("32 bytes");
    }

    @Test
    void blankSecretGeneratesRandomKeyAndIssuerDefaultsToDriveai() {
        JwtService jwtService = new JwtService(new AppProperties(
                new AppProperties.Jwt("", 60, "  "),
                new AppProperties.Admin("", ""),
                new AppProperties.Cors(java.util.List.of())));

        String token = jwtService.generateToken(1L, "x@driveai.test", "ADMIN");

        assertThat(jwtService.issuer()).isEqualTo("driveai");
        assertThat(jwtService.extractEmail(token)).isEqualTo("x@driveai.test");
    }
}
