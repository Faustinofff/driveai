# ADR-011: JWT y control de acceso por rol

Fecha: 2026-11-14. Estado: documentada para revision del equipo.

## Contexto

Separar identidad, permisos y propiedad de carritos/ordenes.

## Decision

Mantener JWT firmado, validacion de issuer/expiracion, BCrypt, filtro stateless, @PreAuthorize para ADMIN y propiedad derivada de UserPrincipal. El registro crea CUSTOMER.

## Evidencia en codigo

backend/src/main/java/com/driveai/security/; service/AuthService.java; controller/CarController.java; service/OrderService.java

## Verificacion

JwtServiceTest; AuthServiceTest; ApiSecurityTest; ApiContractIT. Las pruebas nuevas requieren ejecutar la suite; su existencia no acredita ejecucion exitosa.

## Consecuencias y alternativas

JWT no cifra datos; no incluir secretos. Token en localStorage mantiene riesgo ante XSS. La API decide permisos; ocultar botones no es control de acceso. Revocacion inmediata no esta implementada.
