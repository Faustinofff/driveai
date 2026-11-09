package com.driveai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.soap")
public record SoapProperties(String baseUrl, String path) {
}
