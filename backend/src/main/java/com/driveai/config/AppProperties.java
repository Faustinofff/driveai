package com.driveai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record AppProperties(
        Jwt jwt,
        Admin admin) {

    public record Jwt(String secret, long expirationMinutes) {
    }

    public record Admin(String email, String password) {
    }
}