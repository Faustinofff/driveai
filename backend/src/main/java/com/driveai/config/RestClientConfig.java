package com.driveai.config;

import com.driveai.ai.GroqApiClient;
import com.driveai.ai.HttpGroqApiClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.JdkClientHttpRequestFactory;

import java.time.Duration;

@Configuration
public class RestClientConfig {

    @Bean
    public ClientHttpRequestFactory groqRequestFactory(GroqApiProperties properties) {
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(properties.connectTimeoutSeconds()));
        factory.setReadTimeout(Duration.ofSeconds(properties.readTimeoutSeconds()));
        return factory;
    }

    @Bean
    public GroqApiClient groqApiClient(
            org.springframework.web.client.RestClient.Builder builder, GroqApiProperties properties) {
        return new HttpGroqApiClient(builder, properties, groqRequestFactory(properties));
    }
}