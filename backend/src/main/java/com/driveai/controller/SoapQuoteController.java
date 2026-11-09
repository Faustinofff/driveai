package com.driveai.controller;

import com.driveai.soap.SoapVehicleClient;
import com.driveai.soap.VehicleQuote;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/soap")
@RequiredArgsConstructor
@Tag(name = "Cotización SOAP", description = "Integración con servicio SOAP externo (timeout, retry, circuit breaker y fallback)")
public class SoapQuoteController {

    private final SoapVehicleClient soapVehicleClient;

    @Operation(summary = "Obtiene la cotización SOAP de un vehículo; si el proveedor falla devuelve un fallback controlado")
    @GetMapping("/quote/{carId}")
    public VehicleQuote quote(@PathVariable Long carId) {
        return soapVehicleClient.getQuote(carId);
    }
}
