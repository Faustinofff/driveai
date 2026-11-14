# ADR-009: Idempotency Key en checkout

Fecha: 2026-11-14. Estado: documentada para revision del equipo.

## Contexto

Reintentos del navegador no deben duplicar compras.

## Decision

Mantener reserva atomica (usuario, clave), hash de request y respuesta persistida mediante IdempotencyService; checkout conserva su transaccion.

## Evidencia en codigo

backend/src/main/java/com/driveai/service/IdempotencyService.java; repository/IdempotencyRepository.java; controller/OrderController.java; src/main/resources/db/migration/V7__idempotency.sql

## Verificacion

IdempotencyServiceTest; PersistenceIT; ApiContractIT. Las pruebas nuevas requieren ejecutar la suite; su existencia no acredita ejecucion exitosa.

## Consecuencias y alternativas

La clave es opcional: sin ella el cliente puede crear dos ordenes. Es alcance por usuario y contenido; no sustituye locks de stock ni deduplicacion del consumidor.
