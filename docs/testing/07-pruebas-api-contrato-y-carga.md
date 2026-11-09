# 07 · Pruebas de API, contrato y carga (eje D)

## 1. Pruebas de API

### 1.1 Colección REST Client / IntelliJ HTTP

Archivo: `api-tests.http` (versionado).

Cubre 22 requests con casos positivos y negativos:

- Health, catálogo paginado, filtros combinados, detalle y 404.
- Registro, login válido, login inválido (401) y validación (400).
- Carrito sin token (401), alta, consulta, actualización y borrado.
- Checkout con `Idempotency-Key`, replay y conflicto.
- Historial, detalle propio, cancelación y transición inválida (409).
- CUSTOMER creando auto (403, BFLA).
- Cotización SOAP y asesor IA (503 controlado sin clave).
- OpenAPI.

Uso: abrir el archivo en VS Code (REST Client) o IntelliJ y ejecutar en orden. El login
guarda el token en la variable `@customerToken`.

### 1.2 Colección Postman/Newman

Archivo: `postman/DriveAI.postman_collection.json` (17 requests con scripts de test).

```bash
npm install -g newman
newman run postman/DriveAI.postman_collection.json \
  --env-var baseUrl=http://localhost:8080
```

Incluye aserciones de estado, `Content-Type: application/problem+json`, ETag,
idempotencia, BOLA y BFLA. Se ejecuta en CI en el job `api-newman`
(`workflow_dispatch`, no bloqueante por defecto).

### 1.3 Pruebas de contrato automatizadas

| Suite | Requiere Docker | Estado local |
| --- | --- | --- |
| `WsdlContractTest` | No | ✅ 3/3 |
| `ApiContractIT` | Sí (Testcontainers) | ⛔ NO EJECUTADO |

`ApiContractIT` valida, contra la aplicación completa:

- `/v3/api-docs` contiene todos los paths y el esquema `CarResponse` (incluido `version`).
- Auth: 201, 409, 401, 400 con Problem Details.
- Catálogo: 200 con `version`/`stock`; 404.
- 401/403 por rol.
- ETag/412 y mass assignment.
- BOLA en carrito y órdenes.
- `Idempotency-Key`.
- 409 en cancelación repetida.
- `/actuator/health` = `UP`.

## 2. Contrato OpenAPI

- Documento: `GET /v3/api-docs`
- UI: `GET /swagger-ui.html`
- Generado con SpringDoc 2.6.0 a partir de los controladores y DTOs.
- El contrato se valida en `ApiContractIT` (paths y esquemas) y en la colección Postman.

## 3. Contrato SOAP

- WSDL versionado: `docker/soap-mock/__files/vehicles.wsdl`.
- Mock: WireMock en `docker/soap-mock/mappings/` con tres respuestas:
  - éxito (`carId` normal),
  - fault de negocio (`carId=999`),
  - fault técnico (`carId=998`).
- El cliente usa **HTTP/1.1** explícito para evitar el upgrade a HTTP/2 contra mocks.
- `WsdlContractTest` valida operación, mensajes, binding y address.

## 4. Prueba de carga (k6)

Archivo: `load-tests/k6.js`.

### Escenario

| Parámetro | Valor |
| --- | --- |
| Ejecutor | `constant-vus` |
| Usuarios virtuales | **50** |
| Duración | **5 minutos** |
| Endpoints | `GET /api/cars`, `GET /api/cars/{id}`, `GET /api/cart` (JWT) |
| Usuario | Se crea en `setup()` vía `/api/auth/register` |

No se mide `/api/auth/login` para no mezclar el rate limit anti fuerza bruta (5/min)
con la medición de performance.

### Umbrales

```js
thresholds: {
  http_req_duration: ['p(95)<500'],
  http_req_failed: ['rate<0.01'],
  business_errors: ['rate<0.01'],
}
```

### Ejecución

```bash
k6 run load-tests/k6.js
BASE_URL=http://localhost:8080 k6 run load-tests/k6.js
```

El `handleSummary` escribe `load-tests/summary.json` con latencias, errores e
iteraciones.

### Estado local

> **✅ EJECUTADO** el 2026-11-08 con k6 v2.3.0 y el backend nativo (PostgreSQL 16 en
> `localhost:5433`, RabbitMQ ausente). Salida en `load-tests/summary.json`.

| Métrica | Resultado | Umbral | Estado |
| --- | --- | --- | --- |
| Usuarios virtuales | 50 | 50 | ✅ |
| Duración | 5 min 0 s | 5 min | ✅ |
| Iteraciones completas | **14.950** | — | — |
| Requests HTTP | **14.952** | — | — |
| p95 de latencia | **7,11 ms** | < 500 ms | ✅ |
| Latencia media | 4,47 ms | — | — |
| Latencia máx. | 339,41 ms | — | — |
| `http_req_failed` | **0 %** | < 1 % | ✅ |
| `business_errors` | **0 %** | < 1 % | ✅ |
| Checks `status es 200` | 14.950/14.950 | 100 % | ✅ |
| Checks `respuesta JSON` | 14.950/14.950 | 100 % | ✅ |
| Checks `latencia < 500 ms` | 14.950/14.950 | 100 % | ✅ |

k6 terminó con **exit code 0**: los tres umbrales (`p(95)<500`,
`http_req_failed<0.01`, `business_errors<0.01`) se cumplieron.

> Nota de honestidad: la corrida se hizo contra el backend nativo **sin RabbitMQ**
> (health `DOWN` por el broker, no por la API). El tráfico medido son endpoints de
> lectura de catálogo/detalle y lectura de carrito autenticado; no incluye checkout ni
> publicación de eventos. La medición de performance es válida para esos endpoints y
> queda reproducible con el comando de la sección anterior.

### Cómo interpretar el resultado

- `p(95) < 500 ms` y `rate < 1 %` deben cumplirse para considerar aprobada la carga.
- Si falla, revisar pool de conexiones de Hikari, índices de catálogo y logs del backend.
- El resumen JSON se adjunta como evidencia de la corrida.

## 5. Resumen de estado

| Actividad | Estado |
| --- | --- |
| `api-tests.http` | ✅ versionado (ejecución manual) |
| Postman/Newman | ✅ versionado (CI opcional) |
| `WsdlContractTest` | ✅ 3/3 |
| `ApiContractIT` | ⛔ NO EJECUTADO (Docker) |
| OpenAPI | ✅ generado y validado por contrato |
| k6 50 VU / 5 min | ✅ p95 7,11 ms · error 0 % (14.950 iteraciones) |
