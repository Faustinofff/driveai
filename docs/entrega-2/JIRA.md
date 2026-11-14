# Actualizacion de Jira pendiente de acceso

No se crearon ni cerraron tickets. Vincular con historias existentes antes de crear duplicados.
Completar responsables y claves reales en el tablero del equipo.

| Trabajo | Estado honesto propuesto | Criterio de aceptacion y evidencia |
|---|---|---|
| State en entidad central | En revision | Matriz de transiciones, cambio invalido devuelve 409 por HTTP, entidad sin setter de estado; ADR-007 y OrderStateTest |
| Serializar cancelaciones | En revision | Dos solicitudes simultaneas: una valida, otra conflicto; stock y evento se registran una vez; OrderConcurrencyIT |
| Confirmaciones de RabbitMQ | En revision | ACK sin retorno marca SENT; NACK/NO_ROUTE/timeout mantiene PENDING; OutboxPublisherTest y MessagingIT |
| ADRs de patrones | En revision | 11 ADRs con contexto, decision, consecuencia, ruta y prueba; revision del equipo |
| JWT y roles | Por validar | 401 sin JWT, 403 CUSTOMER en alta de auto, ADMIN autorizado; suite y smoke exitosos |
| Demo punta a punta desplegada | Pendiente | Compose saludable, Playwright exitoso y smoke --messaging con evidencia para orden concreta |
| Incorporar feedback de diseño | Bloqueado por informacion | Adjuntar comentario docente literal, cambio asociado y prueba; no cerrar por suposicion |
| Cierre entrega parcial 2 | Pendiente | Resultados de verificacion publicados y tickets anteriores vinculados, sin declarar pruebas no ejecutadas |

Descripcion reutilizable para las correcciones: mantener React/Spring/PostgreSQL y contratos actuales; reforzar invariantes del dominio y transacciones sin agregar servicios ni frameworks nuevos.
