package com.driveai.soap;

import java.math.BigDecimal;

/**
 * Cotización obtenida del servicio SOAP externo. Si {@code fallback} es true,
 * el servicio no estaba disponible y se devuelve el precio publicado del
 * catálogo con un mensaje claro para el usuario.
 */
public record VehicleQuote(
        Long carId,
        boolean available,
        BigDecimal quote,
        String currency,
        String message,
        boolean fallback) {
}
