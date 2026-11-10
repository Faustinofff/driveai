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

> **Columna "Id Jira":** proyecto `G7` en <https://uade-team-jjfya715.atlassian.net>
> (G5). Cada defecto está en Jira (tipo *Error*) con el mismo estado, responsable y
> vínculo a la HU; la descripción del issue replica la ficha de este documento.
>
> **Verificación en CI:** los estados "verificado en CI" refieren al run verde sobre
> `main` (`b4554eb`, merge del PR #1):
> <https://github.com/Faustinofff/driveai/actions/runs/42867141379>. El run sobre el tag
> `v1.0-testing` se crea después de sanear D-01 (`14`).

## Resumen

| Severidad | Abiertos | Corregidos, pendientes de verificación | Cerrados |
| --- | --- | --- | --- |
| Bloqueante | 0 | 0 | 0 |
| Crítica | 0 | 0 | 3 (D-01, DEF-07, DEF-08) |
| Mayor | 0 | 1 (DEF-11, falta la pasada con `docker compose up`) | 7 (D-04, D-05, DEF-09, DEF-10, DEF-12, DEF-13, DEF-14) |
| Menor | 1 (D-03) | 0 | 0 |

> ✅ **D-01 cerrado el 10/11/2026**: historial reescrito y verificado con Gitleaks
> (`no leaks found` sobre todo el historial). Ver `14-saneamiento-historial-D01.md`.
> Al cierre no queda ningún Bloqueante ni Crítico abierto.

## Detalle

### D-01 · Contraseña de demo en el historial de Git

| Campo | Valor |
| --- | --- |
| Id Jira | [G7-11](https://uade-team-jjfya715.atlassian.net/browse/G7-11) · Listo · responsable: ffiordalisi (dueño del repo) |
| Severidad / Prioridad | **Crítica** / Alta |
| HU | Transversal (G6) |
| Detectado por | Gitleaks + búsqueda manual |
| Pasos | `gitleaks git . --log-opts="--all"` sobre un clon `--mirror` completo |
| Esperado | 0 secretos en todo el historial |
| Obtenido | Contraseña de demo del admin en el README de 3 commits (`3f19436`, `4d0e6d7`, `43903d0`). Al verificar con Gitleaks 8.30.1 apareció además una clave JWT de prueba de alta entropía en `ApiContractIT.java` (10 commits, regla `generic-api-key`): no era un secreto real, pero la cátedra la contaría como hallazgo |
| Causa raíz | Contraseña de demo escrita en el README; se previene con Gitleaks bloqueante sobre el historial completo en CI |
| Corrección | 10/11/2026, autorizada por el equipo: `git filter-branch --tree-filter` sobre un mirror (no había Python para `git filter-repo`), reemplazando la contraseña por `REDACTED` en el README y la clave JWT de prueba por un valor de baja entropía con el mismo efecto (≥ 32 bytes, UTF-8). `push --force` de `main` y `qa/criterios-clase15` desde la cuenta de JIA2004 (colaborador con push). `ADMIN_PASSWORD` rotada por el dueño del repo antes del push |
| Evidencia | Antes: Gitleaks `12 commits scanned … leaks found: 1` + `git grep` → 3 archivos. Después: Gitleaks `12 commits scanned … no leaks found` (mirror y clon local) + `git grep` → 0. El árbol de `main` quedó idéntico salvo la línea de `ApiContractIT.java`. Mapeo de hashes: `3f19436→4b38562`, `b4554eb→3226acf`, `024f6fd→b1baece` |
| Estado | **Cerrado 10/11/2026** |
| Pendiente residual | GitHub sigue sirviendo los commits viejos por URL directa (`/commit/3f19436…`); pedir a GitHub Support "remove sensitive data" (`14` §4). La contraseña ya rotada neutraliza el riesgo |
| Tratamiento posterior | Gitleaks bloqueante en CI: `gitleaks-action` sobre los commits del push **y** una corrida `docker run … gitleaks git --log-opts="--all" --exit-code 1` sobre el historial completo en cada run |

### DEF-07 · Bypass del rate limit de login rotando la IP o `X-Forwarded-For`

| Campo | Valor |
| --- | --- |
| Id Jira | [G7-12](https://uade-team-jjfya715.atlassian.net/browse/G7-12) · Listo · responsable: vbirstok |
| Severidad / Prioridad | **Crítica** (falla de seguridad explotable: fuerza bruta ilimitada) / Alta |
| HU | HU-02 · escenario "Demasiados intentos" |
| Detectado por | Revisión de código asistida por IA (Claude) |
| Pasos | 1) `POST /api/auth/login` 5 veces con contraseña errónea → 429. 2) Repetir agregando `X-Forwarded-For: 1.2.3.<n>` con un `n` distinto en cada request |
| Esperado | 429 a partir del intento N, sin importar la IP declarada |
| Obtenido | Cada IP nueva crea un bucket nuevo: intentos ilimitados contra la misma cuenta. `nginx.conf` anexaba (`$proxy_add_x_forwarded_for`) y el backend toma el primer valor, que controla el cliente |
| Causa raíz | La clave del bucket era solo `IP + email`; no había cuota por cuenta. El test unitario usaba siempre la misma IP, por eso no lo detectó |
| Corrección | `LoginRateLimitService`: segunda cuota por cuenta (`app.login-rate-limit.per-account-per-minute`, 15/min por defecto). `nginx.conf`: `X-Forwarded-For $remote_addr` (sobrescribe) |
| Tests | `LoginRateLimitServiceTest#rotatingIpsAgainstSameAccountIsBlockedByAccountQuota`, `#accountQuotaIsNeverLowerThanIpQuota`, `#defaultAccountQuotaIsThreeTimesIpQuota` |
| Estado | **Cerrado — verificado localmente 10/11** (unitarias 174/174) **y en CI sobre `main`** (unitarias 178/178, 3 corridas idénticas) |
| Efecto colateral aceptado | Un atacante puede bloquear 1 minuto el login de una cuenta concreta (trade-off estándar; documentar en el informe de seguridad) |

### DEF-08 · Bypass del rate limit del asesor IA cambiando `sessionId`

| Campo | Valor |
| --- | --- |
| Id Jira | [G7-13](https://uade-team-jjfya715.atlassian.net/browse/G7-13) · Listo · responsable: vbirstok |
| Severidad / Prioridad | **Crítica** (abuso de costo del proveedor Groq; control de seguridad evadible) / Alta |
| HU | HU-08 · escenario "Rate limit" |
| Detectado por | Revisión de código asistida por IA (Claude) |
| Pasos | `POST /api/ai/recommend` autenticado, enviando un `sessionId` distinto (o un `X-Forwarded-For` distinto) en cada request |
| Esperado | 429 al superar la cuota del usuario |
| Obtenido | La clave era `sessionId + IP`, ambos controlados por el cliente: nunca se llega al límite |
| Causa raíz | La identidad para la cuota salía del body, no del JWT |
| Corrección | `AiAdvisorController.rateLimitKey`: `user:<id del JWT>`; sin principal, la IP del socket (nunca headers) |
| Tests | `AiAdvisorControllerTest` (3), `ApiSecurityTest#aiRateLimitKeyIsBoundToAuthenticatedUserNotToClientData` |
| Estado | **Cerrado — verificado localmente 10/11** (unitarias 174/174) **y en CI sobre `main`** (unitarias 178/178) |

### DEF-09 · Borrar un auto en uso devuelve 500 en lugar de 409

| Campo | Valor |
| --- | --- |
| Id Jira | [G7-14](https://uade-team-jjfya715.atlassian.net/browse/G7-14) · Listo · responsable: vbirstok |
| Severidad / Prioridad | Mayor / Media (workaround: no borrar autos con órdenes) |
| HU | HU-09 · escenario "Eliminación" |
| Detectado por | Revisión de código asistida por IA (Claude) |
| Pasos | Como ADMIN, `DELETE /api/cars/{id}` de un auto que figura en una orden |
| Esperado | 409 Problem Details "referenciado en carritos u órdenes" |
| Obtenido (análisis) | El `DELETE` SQL se ejecuta al hacer flush en el commit, fuera del `try/catch`; la violación de FK llega al handler genérico → 500 |
| Causa raíz | `CarServiceTest#deleteWhenReferencedThrowsConflict` simula que `delete()` lanza la excepción, algo que JPA no hace: **el test pasaba aunque el código no funcionara** (test "que se anula" según la cátedra) |
| Corrección | `CarService.delete`: `flush()` dentro del `try` |
| Tests | `CarServiceTest#deleteWhenFkViolationSurfacesOnFlushThrowsConflict`; contra base real: Newman PM 44 (409) y PM 47 (204) |
| Estado | **Cerrado — verificado 10/11 contra PostgreSQL real**: Newman PM 44 → 409, PM 47 → 204 (54/54 local; 55/55 en CI sobre `main`). Opcional: reproducir el 500 en el commit previo como evidencia del antes |

### DEF-10 · Errores del cliente respondidos como 500

| Campo | Valor |
| --- | --- |
| Id Jira | [G7-15](https://uade-team-jjfya715.atlassian.net/browse/G7-15) · Listo · responsable: vbirstok |
| Severidad / Prioridad | Mayor / Media |
| HU | HU-03 (parámetros inválidos) · transversal (códigos RFC 9457 del Eje C) |
| Detectado por | Revisión de código asistida por IA (Claude) |
| Pasos | `GET /api/cars?sort=noExiste`; `PATCH /api/cars/1` (método no soportado); `POST /api/auth/login` con `Content-Type: text/plain` |
| Esperado | 400 / 405 / 415 en Problem Details |
| Obtenido (análisis) | `@ExceptionHandler(Exception.class)` capturaba también las excepciones estándar de Spring MVC y las devolvía como 500 "Error interno" |
| Corrección | `GlobalExceptionHandler`: `PropertyReferenceException` → 400; toda `ErrorResponse` 4xx conserva su status |
| Tests | `ApiSecurityTest#invalidSortPropertyReturns400`, `#unsupportedMethodReturns405NotServerError`, `#unsupportedMediaTypeReturns415NotServerError`; Schemathesis (`not_a_server_error`) en CI |
| Estado | **Cerrado — verificado localmente 10/11** (unitarias 174/174) **y en CI sobre `main`**: Schemathesis `not_a_server_error` 352/352 |

### DEF-11 · La SPA no envía cabeceras de seguridad

| Campo | Valor |
| --- | --- |
| Id Jira | [G7-16](https://uade-team-jjfya715.atlassian.net/browse/G7-16) · En curso · responsable: vbirstok |
| Severidad / Prioridad | Mayor / Media |
| HU | Transversal (Eje D: "CSP, HSTS, X-Content-Type-Options presentes") |
| Detectado por | Revisión de `frontend/nginx.conf` asistida por IA |
| Pasos | `docker compose up` y `curl -sI http://localhost:5173/` |
| Esperado | `Content-Security-Policy`, `Strict-Transport-Security`, `X-Content-Type-Options` |
| Obtenido | Solo la API (`/api/**`, servida por Spring) las enviaba; el HTML de la SPA no |
| Corrección | `frontend/security-headers.conf` incluido en las `location` de la SPA; `server_tokens off` |
| Verificación | 1) `curl -sI http://localhost:5173/` muestra las cabeceras (10/11). 2) Navegación con la consola abierta y un listener de `securitypolicyviolation`: build de producción (`npm run build`) servido con las 7 cabeceras exactas de `security-headers.conf` → **0 violaciones de CSP** en home, catálogo (filtros), login (envío del formulario), registro, carrito, cambio de tema claro/oscuro y las redirecciones de `#/admin` y `#/ai` (10/11, Chromium). Ver `11` §7 |
| Estado | **Corregido — cabeceras y CSP verificadas el 10/11**. Pendiente: repetir la navegación con `docker compose up` (backend real) para cubrir detalle de auto, checkout, panel admin y respuesta del asesor IA, que sin backend no renderizan |

### DEF-12 · Filtros del catálogo sin nombre accesible (WCAG 2.2 AA)

| Campo | Valor |
| --- | --- |
| Id Jira | [G7-17](https://uade-team-jjfya715.atlassian.net/browse/G7-17) · Listo · responsable: vbirstok |
| Severidad / Prioridad | Mayor / Media (axe la clasifica como violación *critical*) |
| HU | HU-03 / HU-12 (accesibilidad) |
| Detectado por | `e2e/accessibility.spec.ts` (axe) en CI, run #5 del 10/11 |
| Pasos | Abrir `#/catalog` y analizar con axe (WCAG 2.0–2.2 A/AA) |
| Esperado | 0 violaciones críticas |
| Obtenido | `select-name`: los `<select>` de Combustible y Carrocería no tienen nombre accesible (el `<label>` no estaba asociado al control) |
| Causa raíz | `<label>` sin `htmlFor` ni `id` en el control, en todo `CarFiltersPanel` |
| Corrección | `useId()` + `htmlFor`/`id` en los 6 controles del panel |
| Estado | **Cerrado — verificado en CI sobre `main`**: `accessibility.spec.ts` 7/7 (0 violaciones críticas a 360 y 1280 px) |

### DEF-13 · Dos entradas inválidas producen 500 (hallazgo de Schemathesis)

| Campo | Valor |
| --- | --- |
| Id Jira | [G7-18](https://uade-team-jjfya715.atlassian.net/browse/G7-18) · Listo · responsable: vbirstok |
| Severidad / Prioridad | Mayor / Media |
| HU | HU-06 (checkout) · HU-09 (edición de autos) |
| Detectado por | Schemathesis `not_a_server_error` en CI, run #5 del 10/11 (361/410 casos OK) |
| Pasos | 1) `POST /api/orders/checkout` con `{"items": []}` e `Idempotency-Key`. 2) `PUT /api/cars/0` con `If-Match:` vacío y campos de texto vacíos (token CUSTOMER) |
| Esperado | 400 / 403 / 404 en Problem Details |
| Obtenido | 500 "Error interno" |
| Causa raíz (checkout) | Reproducido en local: `IdempotencyService` rechazaba una `Idempotency-Key` de más de 255 caracteres con `IllegalArgumentException` genérica → handler de `Exception` → 500. Schemathesis oculta (`[Filtered]`) ese header, por eso el curl de reproducción no lo mostraba |
| Corrección (checkout) | `InvalidRequestException` → 400 Problem Details. Tests: `IdempotencyServiceTest#oversizedKeyIsRejected`, `ApiSecurityTest#oversizedIdempotencyKeyReturns400`, Newman PM 55 |
| Causa raíz (ambos endpoints) | Log del backend en CI (run #12): `RequestRejectedException` del `StrictHttpFirewall` de Spring Security al leer `If-Match` / `Idempotency-Key` con caracteres de control o bytes no imprimibles (p. ej. `û¸ÿ`). El firewall hace bien en rechazarlos, pero la excepción se dispara dentro del controller y llegaba al handler genérico → 500. No se reproducía en local porque `curl` no envía esos bytes con facilidad |
| Corrección | `GlobalExceptionHandler`: `RequestRejectedException` → 400 Problem Details sin repetir el valor rechazado. Test: `GlobalExceptionHandlerTest` (3); verificación de punta a punta: Schemathesis en CI |
| Estado | **Cerrado — verificado en CI sobre `main`**: Schemathesis 352/352 en `not_a_server_error` y `response_schema_conformance` (16 operaciones) |

### DEF-14 · Dependencias con CVE altas (Trivy)

| Campo | Valor |
| --- | --- |
| Id Jira | [G7-19](https://uade-team-jjfya715.atlassian.net/browse/G7-19) · Listo · responsable: vbirstok |
| Severidad / Prioridad | Mayor / Alta (bloqueaba el CI de `main` desde el 8/11) |
| Detectado por | Trivy en el job `security-scan` |
| Obtenido | `org.postgresql:postgresql` 42.7.4 (3 HIGH) · `spring-boot-starter-actuator` 3.3.5 (CVE-2026-22733, HIGH) · `follow-redirects` 1.16.0 vía axios (3 HIGH) |
| Corrección | pgjdbc → 42.7.12 (`postgresql.version` en `pom.xml`); follow-redirects → 1.16.2 (`npm update follow-redirects`); CVE-2026-22733 mitigada: no se usa Cloud Foundry y `management.cloudfoundry.enabled=false`, excepción documentada en `.trivyignore` |
| Pendiente | Migrar a Spring Boot 3.5.12+ para eliminar la excepción (4.ª entrega) |
| Estado | **Cerrado — verificado en CI sobre `main`**: Trivy (`CRITICAL,HIGH`, `--exit-code 1`) sin hallazgos |

### Defectos previos (de `01-plan-de-pruebas.md`)

| Id | Severidad (nueva escala) | Descripción | Estado |
| --- | --- | --- | --- |
| D-02 | Menor (por diseño) | Sin `GROQ_API_KEY` el asesor responde 503; el E2E valida el fallback | Aceptado por diseño |
| D-03 | Menor | `OrderEventsListener` solo registra el evento | Aceptado / documentado |
| D-04 | Mayor | Testcontainers no corrió en local por falta de Docker | **Cerrado 10/11**: 18/18 IT en local con Docker Desktop |
| D-05 | Mayor | k6 sí se ejecutó; ZAP y Trivy no corrieron en local. **ZAP y Trivy corren en CI** (`dast-zap`, `security-scan`) | **Cerrado 10/11**: en el run de `main` ZAP terminó con 0 High / 0 Medium (118 PASS) y Trivy sin hallazgos |

## Hallazgos de proceso (no son defectos del producto)

| Hallazgo | Riesgo frente a la cátedra | Tratamiento |
| --- | --- | --- |
| El pipeline no se disparaba con tags | G3 pide CI en verde **sobre el tag** | `on.push.tags: ['v*']` |
| Newman solo corría con `workflow_dispatch` y cubría 17 requests | Eje B: "colección corrida en CI; todos los endpoints con éxito y error" | Corre en cada push; 54 requests, todos los endpoints con éxito y error |
| Sin validación de respuestas contra OpenAPI | Eje B: pruebas de contrato | Schemathesis en el job `api-contract` |
| Sin ZAP en CI | Eje D: ZAP baseline + activo autenticado | Job `dast-zap` (falla con alertas High). Primer intento falló al bajar la imagen de `ghcr.io`; se usa `zaproxy/zap-stable` de Docker Hub |
| El CI de `main` estaba en rojo desde el 8/11 (runs #1–#3) | G3 eliminatorio | Causa: Trivy (DEF-14). Corregido: `main` en verde desde el merge del PR #1 (run 42867141379, 9 jobs OK en 3 min) |
| Los defectos no estaban en Jira | G5: "cada defecto en Jira con estado, responsable y vínculo a la HU" | Issues G7-11 … G7-19 creados el 10/11 (tipo *Error*, etiqueta `defecto`), con el mismo estado que este registro |
| Sin SonarCloud | Eje B: Quality Gate | Job `sonarcloud` (requiere configurar el token: `13-configurar-sonarcloud.md`) |
| Sin chequeo de flaky en CI | Eje B: suite 3 veces idéntica | Job `flaky-check` |
| Sin pruebas de accesibilidad | Eje A: axe/Lighthouse ≥ 90, sin violaciones críticas | `frontend/e2e/accessibility.spec.ts` |
| Transiciones inválidas sin test parametrizado | Eje B: "JUnit 5 parametrizado" | `OrderStatusTest#transitionMatrix`, `OrderServiceTest#everyInvalidTransitionIsRejectedWithConflict` |
