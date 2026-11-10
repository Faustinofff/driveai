# 10 · Registro de defectos

Entregable 5. Severidades según la **sección 9 de los criterios de la cátedra**
(reemplazan la escala S1–S4 usada antes en `01-plan-de-pruebas.md`):

| Severidad | Definición | Máximo abierto al cierre |
| --- | --- | --- |
| Bloqueante | Impide usar el flujo principal o hace caer el sistema | 0 |
| Crítica | Pérdida/corrupción de datos, falla de seguridad explotable, HU Must incumplida | 0 |
| Mayor | Funcionalidad afectada con alternativa disponible | 3, con workaround documentado |
| Menor / Cosmética | Impacto bajo, visual o de texto | Sin límite, registrados |

Equivalencia con la escala anterior: S1 → Bloqueante · S2 → Crítica · S3 → Mayor · S4 → Menor.

> **Columna "Id Jira":** completar con la clave real de cada issue (G5). Cada defecto
> tiene que estar en Jira con el mismo estado, responsable y vínculo a la HU.

## Resumen

| Severidad | Abiertos | Corregidos, pendientes de verificación | Cerrados |
| --- | --- | --- | --- |
| Bloqueante | 0 | 0 | 0 |
| Crítica | **1** (D-01) | 2 (DEF-07, DEF-08) | 0 |
| Mayor | 2 (D-05, DEF-13) | 4 (DEF-10, DEF-11, DEF-12, DEF-14) | 2 (D-04, DEF-09) |
| Menor | 1 (D-03) | 0 | 0 |

> ⚠️ **D-01 abierto con severidad Crítica impide aprobar** (máximo 0, y además G6 es
> eliminatorio). Ver `14-saneamiento-historial-D01.md`.

## Detalle

### D-01 · Contraseña de demo en el historial de Git

| Campo | Valor |
| --- | --- |
| Id Jira | _completar_ |
| Severidad / Prioridad | **Crítica** / Alta |
| HU | Transversal (G6) |
| Detectado por | Gitleaks + búsqueda manual |
| Pasos | `gitleaks detect --source . --log-opts="--all"` sobre el repo clonado completo |
| Esperado | 0 secretos en todo el historial |
| Obtenido | 1 hallazgo en el commit `3f19436` (README) |
| Evidencia | Log del job `security-scan` (Gitleaks con `continue-on-error`) |
| Estado | **Abierto** — requiere reescritura de historia acordada por el equipo + rotación |
| Tratamiento | Procedimiento en `14-saneamiento-historial-D01.md`; luego quitar `continue-on-error` del paso Gitleaks |

### DEF-07 · Bypass del rate limit de login rotando la IP o `X-Forwarded-For`

| Campo | Valor |
| --- | --- |
| Id Jira | _completar_ |
| Severidad / Prioridad | **Crítica** (falla de seguridad explotable: fuerza bruta ilimitada) / Alta |
| HU | HU-02 · escenario "Demasiados intentos" |
| Detectado por | Revisión de código asistida por IA (Claude) |
| Pasos | 1) `POST /api/auth/login` 5 veces con contraseña errónea → 429. 2) Repetir agregando `X-Forwarded-For: 1.2.3.<n>` con un `n` distinto en cada request |
| Esperado | 429 a partir del intento N, sin importar la IP declarada |
| Obtenido | Cada IP nueva crea un bucket nuevo: intentos ilimitados contra la misma cuenta. `nginx.conf` anexaba (`$proxy_add_x_forwarded_for`) y el backend toma el primer valor, que controla el cliente |
| Causa raíz | La clave del bucket era solo `IP + email`; no había cuota por cuenta. El test unitario usaba siempre la misma IP, por eso no lo detectó |
| Corrección | `LoginRateLimitService`: segunda cuota por cuenta (`app.login-rate-limit.per-account-per-minute`, 15/min por defecto). `nginx.conf`: `X-Forwarded-For $remote_addr` (sobrescribe) |
| Tests | `LoginRateLimitServiceTest#rotatingIpsAgainstSameAccountIsBlockedByAccountQuota`, `#accountQuotaIsNeverLowerThanIpQuota`, `#defaultAccountQuotaIsThreeTimesIpQuota` |
| Estado | **Corregido — verificado localmente 10/11** (unitarias 174/174) · pendiente CI |
| Efecto colateral aceptado | Un atacante puede bloquear 1 minuto el login de una cuenta concreta (trade-off estándar; documentar en el informe de seguridad) |

### DEF-08 · Bypass del rate limit del asesor IA cambiando `sessionId`

| Campo | Valor |
| --- | --- |
| Id Jira | _completar_ |
| Severidad / Prioridad | **Crítica** (abuso de costo del proveedor Groq; control de seguridad evadible) / Alta |
| HU | HU-08 · escenario "Rate limit" |
| Detectado por | Revisión de código asistida por IA (Claude) |
| Pasos | `POST /api/ai/recommend` autenticado, enviando un `sessionId` distinto (o un `X-Forwarded-For` distinto) en cada request |
| Esperado | 429 al superar la cuota del usuario |
| Obtenido | La clave era `sessionId + IP`, ambos controlados por el cliente: nunca se llega al límite |
| Causa raíz | La identidad para la cuota salía del body, no del JWT |
| Corrección | `AiAdvisorController.rateLimitKey`: `user:<id del JWT>`; sin principal, la IP del socket (nunca headers) |
| Tests | `AiAdvisorControllerTest` (3), `ApiSecurityTest#aiRateLimitKeyIsBoundToAuthenticatedUserNotToClientData` |
| Estado | **Corregido — verificado localmente 10/11** (unitarias 174/174) · pendiente CI |

### DEF-09 · Borrar un auto en uso devuelve 500 en lugar de 409

| Campo | Valor |
| --- | --- |
| Id Jira | _completar_ |
| Severidad / Prioridad | Mayor / Media (workaround: no borrar autos con órdenes) |
| HU | HU-09 · escenario "Eliminación" |
| Detectado por | Revisión de código asistida por IA (Claude) |
| Pasos | Como ADMIN, `DELETE /api/cars/{id}` de un auto que figura en una orden |
| Esperado | 409 Problem Details "referenciado en carritos u órdenes" |
| Obtenido (análisis) | El `DELETE` SQL se ejecuta al hacer flush en el commit, fuera del `try/catch`; la violación de FK llega al handler genérico → 500 |
| Causa raíz | `CarServiceTest#deleteWhenReferencedThrowsConflict` simula que `delete()` lanza la excepción, algo que JPA no hace: **el test pasaba aunque el código no funcionara** (test "que se anula" según la cátedra) |
| Corrección | `CarService.delete`: `flush()` dentro del `try` |
| Tests | `CarServiceTest#deleteWhenFkViolationSurfacesOnFlushThrowsConflict`; contra base real: Newman PM 44 (409) y PM 47 (204) |
| Estado | **Corregido — verificado 10/11 contra PostgreSQL real**: Newman PM 44 → 409, PM 47 → 204 (54/54). Opcional: reproducir el 500 en `main` como evidencia del antes |

### DEF-10 · Errores del cliente respondidos como 500

| Campo | Valor |
| --- | --- |
| Id Jira | _completar_ |
| Severidad / Prioridad | Mayor / Media |
| HU | HU-03 (parámetros inválidos) · transversal (códigos RFC 9457 del Eje C) |
| Detectado por | Revisión de código asistida por IA (Claude) |
| Pasos | `GET /api/cars?sort=noExiste`; `PATCH /api/cars/1` (método no soportado); `POST /api/auth/login` con `Content-Type: text/plain` |
| Esperado | 400 / 405 / 415 en Problem Details |
| Obtenido (análisis) | `@ExceptionHandler(Exception.class)` capturaba también las excepciones estándar de Spring MVC y las devolvía como 500 "Error interno" |
| Corrección | `GlobalExceptionHandler`: `PropertyReferenceException` → 400; toda `ErrorResponse` 4xx conserva su status |
| Tests | `ApiSecurityTest#invalidSortPropertyReturns400`, `#unsupportedMethodReturns405NotServerError`, `#unsupportedMediaTypeReturns415NotServerError`; Schemathesis (`not_a_server_error`) en CI |
| Estado | **Corregido — verificado localmente 10/11** (unitarias 174/174) · Schemathesis pendiente en CI |

### DEF-11 · La SPA no envía cabeceras de seguridad

| Campo | Valor |
| --- | --- |
| Id Jira | _completar_ |
| Severidad / Prioridad | Mayor / Media |
| HU | Transversal (Eje D: "CSP, HSTS, X-Content-Type-Options presentes") |
| Detectado por | Revisión de `frontend/nginx.conf` asistida por IA |
| Pasos | `docker compose up` y `curl -sI http://localhost:5173/` |
| Esperado | `Content-Security-Policy`, `Strict-Transport-Security`, `X-Content-Type-Options` |
| Obtenido | Solo la API (`/api/**`, servida por Spring) las enviaba; el HTML de la SPA no |
| Corrección | `frontend/security-headers.conf` incluido en las `location` de la SPA; `server_tokens off` |
| Verificación pendiente | `curl -sI http://localhost:5173/` muestra las cabeceras **y** la app funciona sin errores de CSP en la consola (catálogo, login, carrito, admin, IA) |
| Estado | **Corregido — cabeceras verificadas con `curl` el 10/11** · falta confirmar en el navegador que la CSP no rompe la app |

### DEF-12 · Filtros del catálogo sin nombre accesible (WCAG 2.2 AA)

| Campo | Valor |
| --- | --- |
| Id Jira | _completar_ |
| Severidad / Prioridad | Mayor / Media (axe la clasifica como violación *critical*) |
| HU | HU-03 / HU-12 (accesibilidad) |
| Detectado por | `e2e/accessibility.spec.ts` (axe) en CI, run #5 del 10/11 |
| Pasos | Abrir `#/catalog` y analizar con axe (WCAG 2.0–2.2 A/AA) |
| Esperado | 0 violaciones críticas |
| Obtenido | `select-name`: los `<select>` de Combustible y Carrocería no tienen nombre accesible (el `<label>` no estaba asociado al control) |
| Causa raíz | `<label>` sin `htmlFor` ni `id` en el control, en todo `CarFiltersPanel` |
| Corrección | `useId()` + `htmlFor`/`id` en los 6 controles del panel |
| Estado | Corregido en rama — pendiente re-run de CI |

### DEF-13 · Dos entradas inválidas producen 500 (hallazgo de Schemathesis)

| Campo | Valor |
| --- | --- |
| Id Jira | _completar_ |
| Severidad / Prioridad | Mayor / Media |
| HU | HU-06 (checkout) · HU-09 (edición de autos) |
| Detectado por | Schemathesis `not_a_server_error` en CI, run #5 del 10/11 (361/410 casos OK) |
| Pasos | 1) `POST /api/orders/checkout` con `{"items": []}` e `Idempotency-Key`. 2) `PUT /api/cars/0` con `If-Match:` vacío y campos de texto vacíos (token CUSTOMER) |
| Esperado | 400 / 403 / 404 en Problem Details |
| Obtenido | 500 "Error interno" |
| Causa raíz (checkout) | Reproducido en local: `IdempotencyService` rechazaba una `Idempotency-Key` de más de 255 caracteres con `IllegalArgumentException` genérica → handler de `Exception` → 500. Schemathesis oculta (`[Filtered]`) ese header, por eso el curl de reproducción no lo mostraba |
| Corrección (checkout) | `InvalidRequestException` → 400 Problem Details. Tests: `IdempotencyServiceTest#oversizedKeyIsRejected`, `ApiSecurityTest#oversizedIdempotencyKeyReturns400`, Newman PM 55 |
| Causa raíz (PUT /api/cars) | No se reproduce en local con el curl reportado (da 400). El job de CI ahora imprime las excepciones del backend al fallar, para capturar la causa en el próximo run |
| Estado | Checkout: corregido en rama · PUT: **abierto, en análisis** |

### DEF-14 · Dependencias con CVE altas (Trivy)

| Campo | Valor |
| --- | --- |
| Id Jira | _completar_ |
| Severidad / Prioridad | Mayor / Alta (bloqueaba el CI de `main` desde el 8/11) |
| Detectado por | Trivy en el job `security-scan` |
| Obtenido | `org.postgresql:postgresql` 42.7.4 (3 HIGH) · `spring-boot-starter-actuator` 3.3.5 (CVE-2026-22733, HIGH) · `follow-redirects` 1.16.0 vía axios (3 HIGH) |
| Corrección | pgjdbc → 42.7.12 (`postgresql.version` en `pom.xml`); follow-redirects → 1.16.2 (`npm update follow-redirects`); CVE-2026-22733 mitigada: no se usa Cloud Foundry y `management.cloudfoundry.enabled=false`, excepción documentada en `.trivyignore` |
| Pendiente | Migrar a Spring Boot 3.5.12+ para eliminar la excepción (4.ª entrega) |
| Estado | Corregido/mitigado en rama — pendiente re-run de CI |

### Defectos previos (de `01-plan-de-pruebas.md`)

| Id | Severidad (nueva escala) | Descripción | Estado |
| --- | --- | --- | --- |
| D-02 | Menor (por diseño) | Sin `GROQ_API_KEY` el asesor responde 503; el E2E valida el fallback | Aceptado por diseño |
| D-03 | Menor | `OrderEventsListener` solo registra el evento | Aceptado / documentado |
| D-04 | Mayor | Testcontainers no corrió en local por falta de Docker | **Cerrado 10/11**: 18/18 IT en local con Docker Desktop |
| D-05 | Mayor | k6 sí se ejecutó; ZAP y Trivy no corrieron en local. **ZAP ahora corre en CI** (`dast-zap`) | Pendiente de CI |

## Hallazgos de proceso (no son defectos del producto)

| Hallazgo | Riesgo frente a la cátedra | Tratamiento |
| --- | --- | --- |
| El pipeline no se disparaba con tags | G3 pide CI en verde **sobre el tag** | `on.push.tags: ['v*']` |
| Newman solo corría con `workflow_dispatch` y cubría 17 requests | Eje B: "colección corrida en CI; todos los endpoints con éxito y error" | Corre en cada push; 54 requests, todos los endpoints con éxito y error |
| Sin validación de respuestas contra OpenAPI | Eje B: pruebas de contrato | Schemathesis en el job `api-contract` |
| Sin ZAP en CI | Eje D: ZAP baseline + activo autenticado | Job `dast-zap` (falla con alertas High). Primer intento falló al bajar la imagen de `ghcr.io`; se usa `zaproxy/zap-stable` de Docker Hub |
| El CI de `main` estaba en rojo desde el 8/11 (runs #1–#3) | G3 eliminatorio | Causa: Trivy (DEF-14). Se corrige en esta rama |
| Sin SonarCloud | Eje B: Quality Gate | Job `sonarcloud` (requiere configurar el token: `13-configurar-sonarcloud.md`) |
| Sin chequeo de flaky en CI | Eje B: suite 3 veces idéntica | Job `flaky-check` |
| Sin pruebas de accesibilidad | Eje A: axe/Lighthouse ≥ 90, sin violaciones críticas | `frontend/e2e/accessibility.spec.ts` |
| Transiciones inválidas sin test parametrizado | Eje B: "JUnit 5 parametrizado" | `OrderStatusTest#transitionMatrix`, `OrderServiceTest#everyInvalidTransitionIsRejectedWithConflict` |
