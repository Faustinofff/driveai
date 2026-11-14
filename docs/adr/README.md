# Registro de decisiones de arquitectura

Las decisiones se reconstruyen del codigo del commit e0551e9f3811cfb299925ad5a3a87627cceb0452 y de las correcciones de esta entrega. Estado pendiente de revision por el equipo; no se atribuye aprobacion docente.

| ADR | Decision |
|---|---|
| [001](001-capas.md) | Arquitectura en capas |
| [002](002-repository-dao.md) | Repository / DAO |
| [003](003-facade.md) | Facade de casos de uso |
| [004](004-adapter.md) | Adapter para integraciones |
| [005](005-factory.md) | Factories de construccion |
| [006](006-specification-strategy.md) | Specification y variacion de criterios |
| [007](007-state.md) | State en Order y exclusión mutua |
| [008](008-outbox-mensajeria.md) | Transactional Outbox, Pub/Sub e Idempotent Consumer |
| [009](009-idempotencia.md) | Idempotency Key en checkout |
| [010](010-resiliencia.md) | Retry, Circuit Breaker y fallback |
| [011](011-jwt-rbac.md) | JWT y control de acceso por rol |

Se distingue el nombre preciso del patron de las etiquetas de la primera memoria (especialmente Factory y Strategy). Una carpeta no demuestra por si sola un patron: cada ADR identifica implementacion, consecuencia y prueba.
