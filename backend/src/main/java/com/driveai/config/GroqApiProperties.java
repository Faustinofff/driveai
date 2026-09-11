package com.driveai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "groq.api")
public record GroqApiProperties(
        String baseUrl,
        String apiKey,
        String model,
        int connectTimeoutSeconds,
        int readTimeoutSeconds,
        int maxTokens,
        double temperature) {
}