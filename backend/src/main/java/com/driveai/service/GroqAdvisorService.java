package com.driveai.service;

import com.driveai.ai.GroqApiClient;
import com.driveai.domain.model.AiRecommendationLog;
import com.driveai.domain.model.Car;
import com.driveai.dto.ai.AiRecommendResponse;
import com.driveai.dto.car.CarResponse;
import com.driveai.exception.GroqApiException;
import com.driveai.rate.RateLimitService;
import com.driveai.repository.AiRecommendationLogRepository;
import com.driveai.repository.CarRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Orquestador del asesor de compra con LLM.
 *
 * <p>Flujo:
 * <ol>
 *   <li>Rate limiting por sessionId/IP (Bucket4j).</li>
 *   <li>Inyección de un catálogo JSON compacto (solo stock &gt; 0) en el system prompt.</li>
 *   <li>Llamada a Groq exigiendo JSON válido.</li>
 *   <li><b>Anti-alucinación:</b> los ids devueltos se contrastan contra la base; si el
 *       modelo inventó ids se filtran silenciosamente.</li>
 *   <li>Persistencia del prompt/respuesta en {@code ai_recommendation_logs}.</li>
 * </ol>
 */
@Service
@RequiredArgsConstructor
public class GroqAdvisorService {

    private static final int MAX_RECOMMENDATIONS = 5;

    private final GroqApiClient groqApiClient;
    private final CarRepository carRepository;
    private final AiRecommendationLogRepository logRepository;
    private final RateLimitService rateLimitService;
    private final ObjectMapper objectMapper;

    private record ParsedRecommendation(String reasoning, List<Long> carIds) {
    }

    public AiRecommendResponse recommend(String prompt, String sessionId, String rateLimitKey) {
        rateLimitService.check(rateLimitKey);

        List<Car> inStock = carRepository.findByStockGreaterThan(0);
        String systemPrompt = buildSystemPrompt(inStock);

        String raw;
        try {
            raw = groqApiClient.chatCompletion(systemPrompt, prompt);
        } catch (GroqApiException ex) {
            persistLog(sessionId, prompt, null, "ERROR", ex.getMessage());
            throw ex;
        }

        try {
            ParsedRecommendation parsed = parseResponse(raw);
            List<CarResponse> recommendations = resolveRecommendations(parsed.carIds());
            persistLog(sessionId, prompt, raw, parsed.carIds(), "SUCCESS", null);
            return new AiRecommendResponse(sessionId, parsed.reasoning(), recommendations);
        } catch (GroqApiException ex) {
            persistLog(sessionId, prompt, raw, "ERROR", ex.getMessage());
            throw ex;
        } catch (RuntimeException ex) {
            GroqApiException wrapped = new GroqApiException(
                    "La respuesta del asistente no pudo procesarse. Reformula tu consulta.", ex);
            persistLog(sessionId, prompt, raw, "ERROR", wrapped.getMessage());
            throw wrapped;
        }
    }

    private ParsedRecommendation parseResponse(String raw) {
        JsonNode root;
        try {
            root = objectMapper.readTree(raw);
        } catch (JsonProcessingException e) {
            throw new GroqApiException("El asistente devolvió una respuesta inválida. Inténtalo de nuevo.", e);
        }
        if (root == null || !root.isObject()) {
            throw new GroqApiException("El asistente devolvió una respuesta inválida. Inténtalo de nuevo.");
        }
        JsonNode idsNode = root.get("recommendedCarIds");
        if (idsNode == null || !idsNode.isArray()) {
            throw new GroqApiException("El asistente no devolvió recomendaciones válidas.");
        }
        List<Long> carIds = new ArrayList<>();
        for (JsonNode idNode : idsNode) {
            if (idNode != null && idNode.isNumber()) {
                carIds.add(idNode.asLong());
            } else if (idNode != null && idNode.isTextual()) {
                try {
                    carIds.add(Long.parseLong(idNode.asText().trim()));
                } catch (NumberFormatException ignored) {
                    // ids no numéricos se descartan: nunca llegan a la consulta.
                }
            }
        }
        JsonNode reasoningNode = root.get("reasoning");
        String reasoning = reasoningNode == null ? null : reasoningNode.asText(null);
        return new ParsedRecommendation(reasoning, carIds);
    }

    private List<CarResponse> resolveRecommendations(List<Long> rawIds) {
        if (rawIds.isEmpty()) {
            return List.of();
        }
        List<Long> distinctIds = rawIds.stream().distinct().limit(MAX_RECOMMENDATIONS).toList();
        Map<Long, Car> availableById = carRepository.findAllById(distinctIds).stream()
                .filter(car -> car.getStock() > 0)
                .collect(Collectors.toMap(Car::getId, Function.identity(), (a, b) -> a));
        return distinctIds.stream()
                .map(availableById::get)
                .filter(Objects::nonNull)
                .map(CarResponse::from)
                .toList();
    }

    private String buildSystemPrompt(List<Car> inStock) {
        List<Map<String, Object>> catalog = inStock.stream()
                .map(car -> Map.<String, Object>of(
                        "id", car.getId(),
                        "brand", car.getBrand(),
                        "model", car.getModel(),
                        "year", car.getYear(),
                        "price", car.getPrice(),
                        "fuelType", car.getFuelType().name(),
                        "fuelConsumption", car.getFuelConsumption(),
                        "bodyType", car.getBodyType().name()))
                .toList();

        String catalogJson;
        try {
            catalogJson = objectMapper.writeValueAsString(catalog);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("No se pudo serializar el catálogo para el prompt", e);
        }

        return """
                Eres el asesor de compra de DriveAI, un e-commerce de autos usados.

                Recibes un catálogo JSON de vehículos disponibles y la consulta del cliente.
                Responde ÚNICAMENTE en formato JSON válido con exactamente dos claves:
                {
                  "reasoning": "Explicación breve y clara en español (máximo 160 caracteres) de por qué recomiendas cada auto.",
                  "recommendedCarIds": [1, 2, 3]
                }

                Reglas:
                - Elige como máximo 5 autos del catálogo.
                - PROHIBIDO inventar ids: únicamente puedes usar ids presentes en el catálogo.
                - recommendedCarIds debe ser un array de enteros.
                - Ajusta tus recomendaciones a la consulta (precio, consumo, carrocería, combustible, transmisión, año).
                - Si nada del catálogo encaja, devuelve un reasoning honesto y recommendedCarIds vacío.

                Catálogo disponible (JSON):
                %s
                """.formatted(catalogJson);
    }

    private void persistLog(String sessionId, String prompt, String response,
                            List<Long> carIds, String status, String errorMessage) {
        String idsJson = null;
        if (carIds != null) {
            try {
                idsJson = objectMapper.writeValueAsString(carIds);
            } catch (JsonProcessingException ignored) {
                idsJson = null;
            }
        }
        persistLog(sessionId, prompt, response, idsJson, status, errorMessage);
    }

    private void persistLog(String sessionId, String prompt, String response,
                            String status, String errorMessage) {
        persistLog(sessionId, prompt, response, (String) null, status, errorMessage);
    }

    private void persistLog(String sessionId, String prompt, String response,
                            String carIdsJson, String status, String errorMessage) {
        AiRecommendationLog log = AiRecommendationLog.builder()
                .sessionId(sessionId)
                .prompt(prompt)
                .response(response)
                .recommendedCarIds(carIdsJson)
                .status(status)
                .errorMessage(errorMessage)
                .build();
        logRepository.save(log);
    }
}