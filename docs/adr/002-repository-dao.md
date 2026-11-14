# ADR-002: Repository / DAO

Fecha: 2026-11-14. Estado: documentada para revision del equipo.

## Contexto

No repetir acceso SQL y transacciones dentro de controladores.

## Decision

Mantener Spring Data JPA como abstraccion de persistencia; consultas y locks se declaran en repositories.

## Evidencia en codigo

backend/src/main/java/com/driveai/repository/CarRepository.java; OrderRepository.java; CartRepository.java

## Verificacion

PersistenceIT; OrderConcurrencyIT; CarServiceTest. Las pruebas nuevas requieren ejecutar la suite; su existencia no acredita ejecucion exitosa.

## Consecuencias y alternativas

Depende de JPA/PostgreSQL; cambiar de motor exige verificar queries nativas, Flyway y locks. Las interfaces son Repository, no DAOs manuales.
