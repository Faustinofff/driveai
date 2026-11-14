# ADR-005: Factories de construccion

Fecha: 2026-11-14. Estado: documentada para revision del equipo.

## Contexto

Clientes HTTP y seguridad requieren construccion coherente con configuracion.

## Decision

Centralizar construccion en metodos @Bean de SecurityConfig, RestClientConfig y SoapConfig; usar factories estaticas from para DTOs.

## Evidencia en codigo

backend/src/main/java/com/driveai/config/RestClientConfig.java; SoapConfig.java; security/SecurityConfig.java; dto/common/PageResponse.java

## Verificacion

ApiSecurityTest; HttpGroqApiClientTest; ApiContractIT. Las pruebas nuevas requieren ejecutar la suite; su existencia no acredita ejecucion exitosa.

## Consecuencias y alternativas

Es uso de factories y del contenedor de inyeccion de dependencias; no afirmar una jerarquia Abstract Factory o Factory Method GoF inexistente.
