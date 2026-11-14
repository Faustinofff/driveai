# ADR-006: Specification y variacion de criterios

Fecha: 2026-11-14. Estado: documentada para revision del equipo.

## Contexto

El catalogo combina filtros opcionales sin multiplicar endpoints ni consultas fijas.

## Decision

Componer predicados en CarSpecification y entregar una Specification<Car> a JpaSpecificationExecutor; CarService conserva su contrato.

## Evidencia en codigo

backend/src/main/java/com/driveai/specification/CarSpecification.java; CarFilters.java; repository/CarRepository.java

## Verificacion

CarSpecificationTest; CarServiceTest. Las pruebas nuevas requieren ejecutar la suite; su existencia no acredita ejecucion exitosa.

## Consecuencias y alternativas

El patron preciso es Specification. La primera memoria lo llamaba Strategy por variar criterios; no existe una familia propia de clases Strategy. No agregarla solo para justificar un nombre.
