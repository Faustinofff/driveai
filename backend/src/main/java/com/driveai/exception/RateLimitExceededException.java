package com.driveai.exception;

import lombok.Getter;

@Getter
public class RateLimitExceededException extends RuntimeException {

    private final long retryAfterSeconds;

    public RateLimitExceededException(long retryAfterSeconds) {
        super("Demasiadas solicitudes. Inténtalo de nuevo en unos segundos.");
        this.retryAfterSeconds = retryAfterSeconds;
    }
}