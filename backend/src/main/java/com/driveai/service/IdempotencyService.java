package com.driveai.service;

import com.driveai.domain.model.IdempotencyRecord;
import com.driveai.exception.ConflictException;
import com.driveai.repository.IdempotencyRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Implementación de Idempotency-Key respaldada por base de datos.
 *
 * <p>Garantías:
 * <ul>
 *   <li>Dos POST concurrentes con la misma clave crean un único recurso.</li>
 *   <li>El reintento devuelve exactamente la misma respuesta almacenada.</li>
 *   <li>Reusar la clave con un cuerpo distinto responde 409.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class IdempotencyService {

    private static final int MAX_KEY_LENGTH = 255;
    private static final int WAIT_ATTEMPTS = 50;
    private static final long WAIT_MILLIS = 100L;

    private final IdempotencyRepository repository;
    private final ObjectMapper objectMapper;

    public <T> T execute(Long userId, String key, Object request, Class<T> responseType, Supplier<T> action) {
        if (key == null || key.isBlank()) {
            return action.get();
        }
        if (key.length() > MAX_KEY_LENGTH) {
            throw new IllegalArgumentException("Idempotency-Key no puede superar 255 caracteres.");
        }

        String hash = hash(request);
        boolean reserved = repository.insertReservation(userId, key, hash) == 1;
        if (!reserved) {
            return replay(userId, key, hash, responseType);
        }

        try {
            T response = action.get();
            String body = objectMapper.writeValueAsString(response);
            repository.complete(userId, key, 200, body);
            return response;
        } catch (RuntimeException ex) {
            // Si la operación falla, liberamos la clave para permitir un reintento.
            repository.deleteReservation(userId, key);
            throw ex;
        } catch (JsonProcessingException ex) {
            repository.deleteReservation(userId, key);
            throw new IllegalStateException("No se pudo serializar la respuesta idempotente", ex);
        }
    }

    private <T> T replay(Long userId, String key, String hash, Class<T> responseType) {
        for (int attempt = 0; attempt < WAIT_ATTEMPTS; attempt++) {
            Optional<IdempotencyRecord> existing = repository.findByUserIdAndIdempotencyKey(userId, key);
            if (existing.isPresent()) {
                IdempotencyRecord record = existing.get();
                if (!record.getRequestHash().equals(hash)) {
                    throw new ConflictException(
                            "La Idempotency-Key ya fue usada con un cuerpo de solicitud distinto.");
                }
                if (record.getResponseBody() != null) {
                    try {
                        return objectMapper.readValue(record.getResponseBody(), responseType);
                    } catch (JsonProcessingException ex) {
                        throw new IllegalStateException("Respuesta idempotente corrupta", ex);
                    }
                }
            }
            if (attempt < WAIT_ATTEMPTS - 1) {
                try {
                    Thread.sleep(WAIT_MILLIS);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        throw new ConflictException(
                "Ya hay una solicitud en curso con la misma Idempotency-Key. Reintenta en unos segundos.");
    }

    private String hash(Object request) {
        try {
            byte[] json = objectMapper.writeValueAsBytes(request);
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(json);
            return HexFormat.of().formatHex(digest);
        } catch (JsonProcessingException | NoSuchAlgorithmException ex) {
            throw new IllegalStateException("No se pudo calcular el hash de idempotencia", ex);
        }
    }

    static String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
