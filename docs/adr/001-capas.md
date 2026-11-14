# ADR-001: Arquitectura en capas

Fecha: 2026-11-14. Estado: documentada para revision del equipo.

## Contexto

React necesita evolucionar sin acceder a PostgreSQL; el negocio necesita pruebas sin HTTP.

## Decision

Conservar el monolito modular Spring: Controller → Service → Repository y adaptadores de infraestructura. React consume REST mediante services/api.ts; Flyway gobierna el esquema.

## Evidencia en codigo

backend/src/main/java/com/driveai/controller/OrderController.java; service/OrderService.java; repository/OrderRepository.java; frontend/src/services/api.ts

## Verificacion

ApiContractIT; OrderServiceTest; frontend/e2e/cart-checkout.spec.ts. Las pruebas nuevas requieren ejecutar la suite; su existencia no acredita ejecucion exitosa.

## Consecuencias y alternativas

No dividir en microservicios para este alcance: agregaria despliegues y consistencia distribuida sin necesidad. La separacion por paquetes no equivale a arquitectura hexagonal estricta.
