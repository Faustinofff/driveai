# ADR-003: Facade de casos de uso

Fecha: 2026-11-14. Estado: documentada para revision del equipo.

## Contexto

Checkout y asesor IA requieren coordinar varias operaciones sin exponerlas al controller.

## Decision

OrderService y GroqAdvisorService ofrecen operaciones de negocio; el primero coordina stock, orden, carrito y outbox en una transaccion.

## Evidencia en codigo

backend/src/main/java/com/driveai/service/OrderService.java; GroqAdvisorService.java

## Verificacion

OrderServiceTest; GroqAdvisorServiceTest. Las pruebas nuevas requieren ejecutar la suite; su existencia no acredita ejecucion exitosa.

## Consecuencias y alternativas

Una fachada no debe convertirse en un servicio universal. Mantener casos de uso por responsabilidad; no trasladar reglas de estado a controllers.
