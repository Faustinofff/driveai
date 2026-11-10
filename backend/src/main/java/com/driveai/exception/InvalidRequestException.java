package com.driveai.exception;

/**
 * Dato de entrada inválido detectado fuera de Bean Validation (por ejemplo, un
 * header). Se traduce a HTTP 400 en {@code GlobalExceptionHandler}.
 *
 * <p>Extiende {@link IllegalArgumentException} por compatibilidad con el código
 * que ya la capturaba como tal.
 */
public class InvalidRequestException extends IllegalArgumentException {

    public InvalidRequestException(String message) {
        super(message);
    }
}
