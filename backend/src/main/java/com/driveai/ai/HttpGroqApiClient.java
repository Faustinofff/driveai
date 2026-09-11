package com.driveai.ai;

import com.driveai.config.GroqApiProperties;
import com.driveai.exception.GroqApiException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;
import java.util.Map;

/**
 * Implementación real del cliente Groq usando RestClient de Spring 6.
 * Traduce errores HTTP (401/403/429/5xx), timeouts y JSON corrupto a
 * {@link GroqApiException} con mensajes amigables.
 */
public class HttpGroqApiClient implements GroqApiClient {

    private final RestClient restClient;
    private final GroqApiProperties properties;
    private final ObjectMapper objectMapper;

    public HttpGroqApiClient(
            RestClient.Builder builder, GroqApiProperties properties,
            org.springframework.http.client.ClientHttpRequestFactory requestFactory) {
        this.properties = properties;
        this.objectMapper = new ObjectMapper();
        this.restClient = builder
                .baseUrl(properties.baseUrl())
                .requestFactory(requestFactory)
                .defaultHeaders(headers -> headers.set(HttpHeaders.AUTHORIZATION, "Bearer " + properties.apiKey()))
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    @Override
    public String chatCompletion(String systemPrompt, String userPrompt) {
        Map<String, Object> body = Map.of(
                "model", properties.model(),
                "temperature", properties.temperature(),
                "max_tokens", properties.maxTokens(),
                "response_format", Map.of("type", "json_object"),
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userPrompt)));

        String raw;
        try {
            raw = restClient.post()
                    .uri("/chat/completions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(String.class);
        } catch (RestClientResponseException ex) {
            throw mapHttpError(ex);
        } catch (RestClientException ex) {
            throw new GroqApiException(
                    "El servicio de IA no respondió a tiempo. Inténtalo de nuevo en un momento.", ex);
        }
        return extractContent(raw);
    }

    private GroqApiException mapHttpError(RestClientResponseException ex) {
        int status = ex.getStatusCode().value();
        if (status == 429) {
            return new GroqApiException(
                    "El servicio de IA está saturado en este momento. Inténtalo de nuevo en unos segundos.", ex);
        }
        if (status == 401 || status == 403) {
            return new GroqApiException(
                    "No se pudo autenticar con el servicio de IA. Verifica que GROQ_API_KEY esté configurada correctamente.", ex);
        }
        return new GroqApiException("El servicio de IA no está disponible por el momento.", ex);
    }

    private String extractContent(String rawResponse) {
        if (rawResponse == null || rawResponse.isBlank()) {
            throw new GroqApiException("El servicio de IA devolvió una respuesta vacía.");
        }
        try {
            JsonNode root = objectMapper.readTree(rawResponse);
            JsonNode content = root.path("choices").path(0).path("message").path("content");
            String text = content.asText(null);
            if (text == null || text.isBlank()) {
                throw new GroqApiException("El servicio de IA no devolvió contenido utilizable.");
            }
            return text;
        } catch (JsonProcessingException e) {
            throw new GroqApiException("El servicio de IA devolvió una respuesta malformada.", e);
        }
    }
}