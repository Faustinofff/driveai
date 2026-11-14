# ADR-010: Retry, Circuit Breaker y fallback

Fecha: 2026-11-14. Estado: documentada para revision del equipo.

## Contexto

Un proveedor externo lento no debe bloquear indefinidamente ni recibir reintentos ilimitados.

## Decision

Conservar timeouts de clientes, retry acotado y circuit breakers Resilience4j para Groq y SOAP; errores de negocio no se tratan como indisponibilidad tecnica.

## Evidencia en codigo

backend/src/main/java/com/driveai/service/GroqAdvisorService.java; soap/SoapVehicleClient.java; src/main/resources/application.yml

## Verificacion

GroqAdvisorServiceTest; SoapVehicleClientTest; HttpGroqApiClientTest. Las pruebas nuevas requieren ejecutar la suite; su existencia no acredita ejecucion exitosa.

## Consecuencias y alternativas

Timeouts por intento no equivalen a deadline global. Fallback no confirma certificaciones ni inventa vehiculos. El SOAP actual cotiza desde un endpoint separado: no afirmar que valida certificacion durante checkout.
