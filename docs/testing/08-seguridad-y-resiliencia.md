# 08 · Seguridad, resiliencia y análisis estático (eje E)

## 1. Modelo de amenazas (resumen)

| Amenaza | Vector | Control implementado | Verificación |
| --- | --- | --- | --- |
| Robo de sesión | JWT manipulable | Firma HMAC, issuer, expiración, rechazo de `alg=none` | `JwtServiceTest` (7) |
| BOLA / IDOR | `userId`/`sessionId` del cliente | Propiedad derivada del JWT; 404 en recursos ajenos | `CartServiceTest`, `OrderServiceTest`, `ApiContractIT`, E2E |
| BFLA | CUSTOMER llamando endpoints admin | `@PreAuthorize` + reglas de `SecurityConfig` | `ApiSecurityTest`, `ApiContractIT`, E2E |
| Mass assignment | `role`, `id`, `version` en el body | DTOs de escritura sin campos sensibles | `ApiContractIT`, `CarServiceTest` |
| Fuerza bruta | Login ilimitado | Rate limit 5/min por IP → 429 + `Retry-After` | `LoginRateLimitServiceTest` |
| Inyección SQL | Parámetros de filtros | JPA/Criteria parametrizado | `CarSpecificationTest` |
| XSS | Datos del LLM / catálogo | React escapa por defecto; sin `dangerouslySetInnerHTML` | E2E IA |
| XXE | XML SOAP malicioso | DTD y entidades externas deshabilitadas | `SoapVehicleClientTest` |
| Fuga de información | Stack traces / errores internos | Problem Details sin stack trace | `ApiSecurityTest`, `ApiContractIT` |
| CORS abierto | Origen arbitrario | Allowlist explícita, sin comodín con credenciales | `SecurityConfig` |
| Clickjacking / sniffing | iframes, MIME | `frame-ancestors 'none'`, `X-Content-Type-Options` | `ApiSecurityTest` |
| Secretos en repo | `.env`, historial | `.env` ignorado, `.env.example` sin secretos | Gitleaks / revisión manual |

## 2. Autenticación y autorización

- Contraseñas con **BCrypt**.
- JWT firmado; el filtro construye un `UserPrincipal` desde `uid`/`role` y rechaza roles
  desconocidos.
- Validaciones cubiertas por tests: firma alterada, token expirado, issuer incorrecto,
  `alg=none`, secreto corto y rol inválido.
- Endpoints de administración protegidos en el backend, no solo ocultos en la UI.
- El frontend muestra "Acceso restringido" a un CUSTOMER en `#/admin`, pero la
  autorización real la aplica el backend (403).

## 3. BOLA / IDOR

Antes del refactor, el carrito y las órdenes aceptaban `sessionId`/`userId` del cliente.
Ahora:

- `CartController` y `OrderController` obtienen el usuario de `@AuthenticationPrincipal`.
- Los DTOs ya no aceptan `sessionId`.
- Un usuario B que intenta leer o modificar un recurso de A recibe **404** (no se filtra
  la existencia del recurso).

Verificado en `CartServiceTest`, `OrderServiceTest`, `ApiContractIT` y E2E.

## 4. Mass assignment

`CarWriteRequest`, `RegisterRequest`, `CheckoutRequest` y `CartAddRequest` solo exponen
los campos permitidos. Un body con `id`, `version` o `role` no modifica esos valores;
`ApiContractIT` lo comprueba creando un auto con `role: ADMIN` e `id` arbitrario.

## 5. Cabeceras y CORS

`SecurityConfig` agrega:

- `Content-Security-Policy`
- `Strict-Transport-Security`
- `X-Content-Type-Options: nosniff`
- `X-Frame-Options: DENY` / `frame-ancestors 'none'`
- `Referrer-Policy`
- CORS con `CORS_ALLOWED_ORIGINS` (default `http://localhost:5173,http://localhost:4173`),
  sin `*` cuando hay credenciales.

Actuator expone solo `health` e `info`.

## 6. Escaneo de secretos (Gitleaks)

- `.env` y `.tools/` están en `.gitignore`; solo se versiona `.env.example` con valores
  vacíos/placeholder.
- Se hizo una búsqueda manual de los secretos reales (JWT y `GROQ_API_KEY`) en todo el
  historial: **0 coincidencias**.
- **Hallazgo D-01:** la contraseña de demo (ver `ADMIN_PASSWORD` en `.env`) aparece **1 vez** en el
  historial (commit `3f19436`, README). El código actual ya no la contiene. Eliminarla
  requiere reescribir la historia, acción que **no se ejecutó** por no contar con
  autorización explícita del equipo.
- Por ese motivo, en CI Gitleaks se ejecuta con `continue-on-error: true` y el hallazgo
  queda documentado. **G6 no puede declararse aprobado hasta sanear el historial con
  autorización.**

## 7. Dependencias y DAST

| Herramienta | Estado | Observación |
| --- | --- | --- |
| **SpotBugs 4.8.6.6** | ✅ 0 hallazgos | `mvn -B spotbugs:check` con `failOnError=true`; ver §8 |
| **Trivy** (dependencias) | ⛔ NO EJECUTADO local | Definido en CI (`severity CRITICAL,HIGH`, bloqueante) |
| **OWASP ZAP baseline** | ⛔ NO EJECUTADO local | Requiere stack levantado; se documenta checklist manual |
| **Gitleaks** | ⚠️ manual + CI no bloqueante | D-01 pendiente de autorización |

### Checklist ZAP manual (para ejecutar con el stack arriba)

```bash
# Baseline pasivo
docker run --rm -t -v "$(pwd)/zap:/zap/wrk" \
  ghcr.io/zaproxy/zap-stable zap-baseline.py \
  -t http://host.docker.internal:8080 -r zap-baseline.html

# Active scan autenticado (requiere token de un usuario de prueba)
docker run --rm -t -v "$(pwd)/zap:/zap/wrk" \
  ghcr.io/zaproxy/zap-stable zap-full-scan.py \
  -t http://host.docker.internal:8080 -r zap-full.html
```

Puntos a revisar en el reporte:

- Ausencia de `Content-Security-Policy` / `HSTS` (ya configurados).
- CORS permisivo (allowlist).
- Cookies sin `HttpOnly`/`Secure` (no se usan cookies: JWT en `Authorization`).
- Divulgación de versiones en cabeceras.
- Formularios sin anti-CSRF (API stateless con Bearer; no aplica).
- XSS reflejado en parámetros de búsqueda.

## 8. Análisis estático (SpotBugs)

```bash
cd backend
mvn -B spotbugs:check
```

Resultado real:

```
BugInstance size is 0
Error size is 0
No errors/warnings found
BUILD SUCCESS
```

Se configuró `backend/config/spotbugs-exclude.xml` para falsos positivos justificados
(inyección de dependencias de Spring, records DTO, constructor fail-fast de `JwtService`,
saltos de línea en text blocks de XML/SOAP). El filtro está versionado y es auditable.

## 9. Resiliencia

### 9.1 Integración SOAP

| Requisito | Implementación | Evidencia |
| --- | --- | --- |
| Timeout 5 s | `JdkClientHttpRequestFactory` + `HttpClient` HTTP/1.1 | `SoapVehicleClientTest` |
| Reintento acotado | Resilience4j `retry` (2 intentos, 300 ms) solo para fallos técnicos | `SoapVehicleClientTest` |
| Circuit breaker | Resilience4j `circuitbreaker` con exclusión de faults de negocio | configuración + tests |
| Fallback | Respuesta controlada si el proveedor no responde | `SoapVehicleClientTest` |
| Fault de negocio | Se mapea a 409 y **no** se reintenta ni se oculta | `SoapVehicleClientTest` |
| XXE | Parser con DTD/entidades externas deshabilitadas | `SoapVehicleClientTest` |

### 9.2 Mensajería

| Requisito | Implementación | Evidencia |
| --- | --- | --- |
| Outbox transaccional | `outbox_events` en la misma transacción de la orden | `OutboxServiceTest` |
| Publicación confiable | `OutboxPublisher` reintenta y marca `SENT` | `OutboxPublisherTest` |
| Broker caído | El evento queda `PENDING` y se reintenta; se recupera solo al volver (los fallos técnicos no lo descartan) | `OutboxPublisherTest`, `MessagingIT` |
| Consumidor idempotente | `processed_messages` deduplica por `message_id` | `OrderEventsListenerTest`, `MessagingIT` |
| DLQ | Exchange/queue con dead-letter exchange | `MessagingConfigTest`, `MessagingIT` |
| Mensaje inválido | Se rechaza y va a la DLQ | `MessagingIT` |

### 9.3 Transacciones e idempotencia

- Checkout transaccional con bloqueo pesimista y descuento de stock.
- Si falla, **rollback**: no queda orden ni stock descontado.
- `Idempotency-Key` respaldada por base (`idempotency_keys`):
  - replay devuelve la misma orden,
  - concurrencia resuelve con `INSERT … ON CONFLICT`,
  - mismo key con payload distinto → 409,
  - si la operación falla, la clave se libera.
- ETag/`If-Match` para concurrencia optimista en autos → 412.
- Máquina de estados de órdenes → 409 ante transiciones inválidas.

### 9.4 Errores uniformes

Todos los errores usan **RFC 9457 / RFC 7807** (`application/problem+json`) con
`type`, `title`, `status`, `detail` e `instance`, sin stack traces ni datos internos.
Códigos cubiertos: 400, 401, 403, 404, 409, 412, 422, 429, 500, 503.

## 10. Resultado consolidado

| Control | Estado |
| --- | --- |
| JWT (firma, expiración, issuer, `alg=none`) | ✅ |
| BOLA/IDOR carrito y órdenes | ✅ |
| BFLA admin | ✅ |
| Mass assignment | ✅ |
| Rate limit login / IA | ✅ |
| Cabeceras de seguridad y CORS | ✅ |
| XXE | ✅ |
| SpotBugs | ✅ 0 hallazgos |
| Gitleaks | ⚠️ hallazgo histórico D-01 |
| Trivy | ⛔ NO EJECUTADO local (CI) |
| ZAP | ⛔ NO EJECUTADO local |
| Resiliencia SOAP / broker / outbox | ✅ (unitarias) / ⛔ IT en CI |
