# 04 · Pruebas de integración con dependencias reales (eje B)

## 1. Objetivo

Validar el comportamiento del sistema contra **PostgreSQL 16 y RabbitMQ reales**
(no mocks), incluyendo migraciones, constraints, concurrencia, mensajería y contrato
HTTP completo. Se usa **Testcontainers**, que levanta contenedores efímeros por corrida.

## 2. Comando

```bash
cd backend
mvn -B verify -Pintegration
```

El perfil `integration` activa **Failsafe**, que ejecuta las clases `*IT.java` en la fase
`integration-test` y publica el resultado en `backend/target/failsafe-reports/`.

> **Estado local: ⛔ NO EJECUTADO.** El entorno de desarrollo usado para esta entrega no
> tiene Docker daemon ni virtualización habilitada, por lo que Testcontainers no puede
> iniciar contenedores. Las clases compilan y están listas; el pipeline de CI
> (`.github/workflows/ci.yml`, job `backend-integration`) las ejecuta en un runner con
> Docker. No se reportan como aprobadas.

## 3. Suites implementadas

### 3.1 `PersistenceIT` — PostgreSQL real

| Test | Verifica |
| --- | --- |
| Migraciones Flyway | Aplica V1…V8 sobre base limpia y valida el esquema |
| Unicidad de carrito por usuario | Un usuario no puede tener dos carritos (`unique user_id`) |
| Versión optimista | `@Version` incrementa y detecta actualización concurrente |
| Propiedad de órdenes | Una orden pertenece a un único `user_id` y se consulta por dueño |
| Reserva atómica de idempotencia | `INSERT … ON CONFLICT` devuelve 1/0 según concurrencia |
| Deduplicación de mensajes | `processed_messages` impide procesar dos veces el mismo `message_id` |
| Integridad referencial | FKs y constraints de stock/precio se respetan |

### 3.2 `MessagingIT` — RabbitMQ real

| Test | Verifica |
| --- | --- |
| Outbox → broker → consumidor | Evento `PENDING` se publica (`SENT`) y el consumidor lo procesa |
| Consumidor idempotente | Tres entregas del mismo `eventId` producen un solo efecto |
| Mensaje inválido → DLQ | Un evento sin campos obligatorios termina en `driveai.orders.dlq` |
| Caída y recuperación del broker | Con RabbitMQ detenido el evento queda `PENDING`; al volver se publica solo |

La suite detiene y vuelve a levantar el contenedor de RabbitMQ (`rabbit.stop()` /
`rabbit.start()`) y usa **Awaitility** con timeouts explícitos. El orden de los tests está
fijado con `@TestMethodOrder` para que la recuperación ocurra al final.

### 3.3 `ApiContractIT` — API completa con PostgreSQL y RabbitMQ reales

| Test | Verifica |
| --- | --- |
| OpenAPI | `/v3/api-docs` expone todos los paths críticos y el esquema `CarResponse` |
| Auth | Registro 201, duplicado 409, credenciales inválidas 401, validación 400 |
| Catálogo | Listado 200 con `version`/`stock`, detalle 404 Problem Details |
| Autorización | Carrito anónimo 401; CUSTOMER creando auto 403 (BFLA) |
| ETag / mass assignment | `If-Match` viejo → 412; `id`/`version`/`role` ignorados |
| BOLA carrito | Usuario B no ve ni modifica el carrito de A (404) |
| BOLA órdenes | Usuario B no ve la orden de A (404) |
| Idempotencia | Misma `Idempotency-Key` devuelve la misma orden |
| Transiciones | Cancelar dos veces → 409 |
| Salud | `/actuator/health` responde `UP` |

## 4. Prueba de contrato que sí se ejecuta localmente

`WsdlContractTest` no requiere Docker y valida el WSDL versionado del mock SOAP:

```bash
mvn -B test -Dtest=WsdlContractTest
# Tests run: 3, Failures: 0, Errors: 0, Skipped: 0
```

Comprueba:

- `targetNamespace = http://driveai.com/soap/vehicles`
- operación `getVehicleQuote`, `portType`, `binding` y `service`
- mensajes `GetVehicleQuoteInput/Output` y `VehicleQuoteFaultMessage`
- binding SOAP document/literal y `address` terminado en `/soap/vehicles`

## 5. Dependencias externas simuladas

| Dependencia | Estrategia |
| --- | --- |
| Groq Cloud | WireMock en unitarias (`HttpGroqApiClientTest`, `GroqAdvisorServiceTest`) |
| Proveedor SOAP | WireMock + WSDL versionado + mock en Docker Compose |
| PostgreSQL | Testcontainers `postgres:16-alpine` (IT) / servicio en CI (E2E) |
| RabbitMQ | Testcontainers `rabbitmq:3.13-management-alpine` (IT) / servicio en CI (E2E) |

## 6. Criterio de aceptación

- Todas las clases `*IT` deben pasar en CI con Docker.
- Cualquier fallo debe dejar el reporte de Failsafe como artefacto.
- La suite debe ser repetible: contenedores efímeros y datos con UUID para evitar
  colisiones entre corridas.
