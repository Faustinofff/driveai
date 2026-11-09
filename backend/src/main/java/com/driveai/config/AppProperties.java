package com.driveai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "app")
public record AppProperties(
        Jwt jwt,
        Admin admin,
        Cors cors) {

    public record Jwt(String secret, long expirationMinutes, String issuer) {
    }

    public record Admin(String email, String password) {
    }

    /**
     * Orígenes permitidos para CORS. Se configuran por entorno y nunca se usa
     * comodín junto con credenciales.
     */
    public record Cors(List<String> allowedOrigins) {
    }
}