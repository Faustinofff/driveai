package com.driveai.exception;

/**
 * Fault técnico o fallo de transporte del servicio SOAP. Se traduce a un
 * fallback controlado para el usuario (HTTP 200 con disponibilidad=false o 503).
 */
public class SoapTechnicalException extends RuntimeException {

    public SoapTechnicalException(String message, Throwable cause) {
        super(message, cause);
    }
}
