# ADR-004: Adapter para integraciones

Fecha: 2026-11-14. Estado: documentada para revision del equipo.

## Contexto

El negocio no debe conocer HTTP, JSON del proveedor o XML SOAP.

## Decision

GroqApiClient define el puerto y HttpGroqApiClient adapta HTTP. SoapVehicleClient encapsula protocolo, parser y errores externos.

## Evidencia en codigo

backend/src/main/java/com/driveai/ai/GroqApiClient.java; HttpGroqApiClient.java; soap/SoapVehicleClient.java

## Verificacion

HttpGroqApiClientTest; SoapVehicleClientTest. Las pruebas nuevas requieren ejecutar la suite; su existencia no acredita ejecucion exitosa.

## Consecuencias y alternativas

Permite fakes en tests y sustituir transporte. La semantica de otro proveedor aun requerira adaptar su respuesta; un DTO.from es un mapper, no evidencia suficiente de Adapter GoF.
