package com.driveai.exception;

/**
 * Fault de negocio devuelto por el servicio SOAP (ej. vehículo no disponible).
 * Se traduce a HTTP 409 con un mensaje amigable; nunca se filtra el XML crudo.
 */
public class SoapBusinessException extends RuntimeException {

    public SoapBusinessException(String message) {
        super(message);
    }
}
