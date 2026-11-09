# 02 · Matriz de casos de prueba

> Convención: **CP** = caso positivo, **CN** = caso negativo, **CB** = caso borde.
> La columna **Nivel** indica dónde se automatiza: `U` unitaria, `MVC` controlador,
> `IT` integración Testcontainers, `E2E` Playwright, `API` REST Assured/HTTP.
> **Estado local**: ✅ ejecutado y verde · ⛔ no ejecutado en este entorno (requiere
> Docker/herramienta) · 🔁 cubierto por CI.

## Resumen

| Tipo | Cantidad | % |
| --- | --- | --- |
| Positivos (CP) | 24 | 31 % |
| Negativos (CN) | 47 | 60 % |
| Borde (CB) | 7 | 9 % |
| **Total** | **78** | **100 %** |
| **Negativos + borde** | **54** | **69 %** |

El requisito de **≥ 40 % de casos negativos/borde** se cumple con 69 %.

---

## 1. Autenticación y autorización

| ID | Caso | Nivel | Esperado | Estado |
| --- | --- | --- | --- | --- |
| CP-01 | Registro con email y contraseña válidos | U, E2E, API | 201 + JWT + rol `CUSTOMER` | ✅ |
| CP-02 | Login con credenciales válidas | U, E2E, API | 200 + JWT | ✅ |
| CN-01 | Login con contraseña incorrecta | U, E2E, API | 401 `application/problem+json` | ✅ |
| CN-02 | Registro con email ya existente | U, IT | 409 Problem Details | ✅ U / ⛔ IT |
| CN-03 | Registro con email mal formado | U, MVC, IT | 400 con detalle de validación | ✅ U / ⛔ IT |
| CN-04 | Registro con contraseña < 8 caracteres | U, MVC | 400 | ✅ |
| CN-05 | JWT expirado | U | 401 / excepción de validación | ✅ |
| CN-06 | JWT con firma alterada | U | 401, no autentica | ✅ |
| CN-07 | JWT con issuer distinto | U | 401 | ✅ |
| CN-08 | JWT con `alg=none` | U | 401, no acepta token sin firma | ✅ |
| CN-09 | Secreto JWT demasiado corto | U | Falla al construir el servicio | ✅ |
| CN-10 | Rol desconocido en el token | U | Token rechazado | ✅ |
| CN-11 | > 5 intentos de login por minuto | U | 429 + `Retry-After` | ✅ |
| CN-12 | Endpoint de carrito sin token | MVC, API | 401 Problem Details | ✅ |
| CN-13 | CUSTOMER intenta crear un auto (BFLA) | MVC, API, E2E | 403 | ✅ |

## 2. Catálogo y administración

| ID | Caso | Nivel | Esperado | Estado |
| --- | --- | --- | --- | --- |
| CP-03 | Listado paginado del catálogo | U, API, E2E | 200 + `content[]` + `totalElements` | ✅ |
| CP-04 | Filtros combinados (precio, combustible, carrocería) | U, API | 200 con resultados coherentes | ✅ |
| CP-05 | Detalle de vehículo | API, E2E | 200 + `ETag` + datos completos | ✅ |
| CN-14 | Detalle de auto inexistente | MVC, API | 404 Problem Details | ✅ |
| CB-01 | Búsqueda sin resultados | E2E | Estado vacío "Sin resultados" | ✅ |
| CB-02 | Búsqueda con debounce de 400 ms | E2E | Resultados filtrados sin recargar | ✅ |
| CB-03 | Limpiar filtros y búsqueda | E2E | Vuelve a mostrar el catálogo | ✅ |
| CP-06 | ADMIN crea auto | MVC, API, E2E | 201 + `ETag` | ✅ |
| CP-07 | ADMIN edita con `If-Match` correcto | MVC, API | 200 + nueva versión | ✅ |
| CN-15 | Edición con `If-Match` desactualizado | MVC, API | 412 Problem Details | ✅ |
| CN-16 | Mass assignment (`id`, `version`, `role`) | API | Campos ignorados; no escalada | ✅ |
| CP-08 | ADMIN elimina auto sin ventas | API, E2E | 204 | ✅ |
| CN-17 | Eliminar auto con órdenes asociadas | U | 409 conflicto de negocio | ✅ |
| CN-18 | Anónimo intenta crear auto | MVC | 401 | ✅ |

## 3. Carrito

| ID | Caso | Nivel | Esperado | Estado |
| --- | --- | --- | --- | --- |
| CP-09 | Agregar vehículo al carrito | U, API, E2E | 200 con ítem y `priceSnapshot` | ✅ |
| CP-10 | Actualizar cantidad de un ítem propio | U, API | 200 con nuevo subtotal | ✅ |
| CN-19 | Agregar con cantidad 0 o negativa | U, MVC | 400 validación | ✅ |
| CN-20 | Agregar más unidades que el stock | U | 409 stock insuficiente | ✅ |
| CN-21 | Ver carrito sin autenticación | MVC, API | 401 | ✅ |
| CN-22 | Usuario B modifica ítem del usuario A (BOLA) | U, IT, API | 404, no filtra existencia | ✅ U/API / ⛔ IT |
| CN-23 | Usuario B elimina ítem del usuario A (BOLA) | U, IT, API | 404 | ✅ U/API / ⛔ IT |
| CB-04 | Carrito aislado: B ve su carrito vacío | IT, API | `items=[]` y `userId` propio | ✅ API / ⛔ IT |
| CN-24 | Carrito con `carId` inexistente | U | 404 | ✅ |

## 4. Órdenes y checkout

| ID | Caso | Nivel | Esperado | Estado |
| --- | --- | --- | --- | --- |
| CP-11 | Checkout con stock suficiente | U, API, E2E | 200 orden `CONFIRMED`, stock descontado | ✅ |
| CP-12 | Historial de órdenes propias | U, API | 200 solo órdenes del usuario | ✅ |
| CP-13 | Detalle de orden propia | U, API | 200 | ✅ |
| CN-25 | Detalle de orden ajena (BOLA/IDOR) | U, API | 404 | ✅ |
| CP-14 | Cancelar orden propia | U, API | 200 `CANCELLED` + stock restaurado | ✅ |
| CN-26 | Cancelar una orden ya cancelada | U, API | 409 transición inválida | ✅ |
| CN-27 | Transición de estado inválida | U | 409 | ✅ |
| CN-28 | Checkout sin stock | U | 409 | ✅ |
| CN-29 | Checkout sin autenticación | MVC, E2E | 401 / redirección al login | ✅ |
| CB-05 | Reintento con la misma `Idempotency-Key` | U, IT, API | Devuelve la misma orden, no duplica | ✅ U/API / ⛔ IT |
| CN-30 | Misma key con payload distinto | U | 409 conflicto de idempotencia | ✅ |
| CN-31 | Fallo durante el checkout | U | Rollback: no queda orden ni stock descontado | ✅ |
| CN-32 | Cancelar orden inexistente | U | 404 | ✅ |

## 5. Asesor IA (Groq)

| ID | Caso | Nivel | Esperado | Estado |
| --- | --- | --- | --- | --- |
| CP-15 | Recomendación válida con IDs reales | U (WireMock) | 200 + autos en stock | ✅ |
| CN-33 | Sin `GROQ_API_KEY` | U, E2E | 503 controlado; el e-commerce sigue vivo | ✅ |
| CN-34 | Timeout del proveedor | U (WireMock) | Fallback 503, sin colgar la request | ✅ |
| CN-35 | JSON inválido del LLM | U | Fallback 503 | ✅ |
| CB-06 | IDs alucinados por el LLM | U | Se filtran; solo autos reales | ✅ |
| CN-36 | Prompt injection pide prompt/clave | E2E | No revela instrucciones ni `gsk_` | ✅ |
| CN-37 | Rate limit de recomendaciones | U | 429 + `Retry-After` | ✅ |
| CN-38 | Asesor IA sin autenticación | MVC, E2E | 401 / redirección al login | ✅ |

## 6. Integración SOAP

| ID | Caso | Nivel | Esperado | Estado |
| --- | --- | --- | --- | --- |
| CP-16 | Cotización SOAP exitosa | U (WireMock) | 200 con importe | ✅ |
| CN-39 | Fault de negocio (auto no cotizable) | U | 409, **sin** reintento ni fallback que lo oculte | ✅ |
| CN-40 | Fault técnico | U | Retry acotado + fallback | ✅ |
| CN-41 | Timeout de 5 s | U | Timeout respetado y fallback | ✅ |
| CN-42 | XML con DOCTYPE / entidad externa (XXE) | U | Parser rechaza el documento | ✅ |
| CP-17 | Contrato WSDL | U | Operación, mensajes, binding y address correctos | ✅ |

## 7. Mensajería (RabbitMQ)

| ID | Caso | Nivel | Esperado | Estado |
| --- | --- | --- | --- | --- |
| CP-18 | Outbox publica y el consumidor procesa | U, IT | Evento `SENT` + mensaje procesado | ✅ U / ⛔ IT |
| CB-07 | Mensaje duplicado | U, IT | Se procesa una sola vez (idempotencia) | ✅ U / ⛔ IT |
| CN-43 | Mensaje inválido | IT | Se deriva a la DLQ | ⛔ IT |
| CN-44 | Broker caído | U, IT | Evento queda `PENDING` y se recupera al volver | ✅ U / ⛔ IT |
| CP-19 | Topología exchange/queue/DLQ | U | Nombres, durabilidad y routing correctos | ✅ |

## 8. End-to-end y UX

| ID | Caso | Nivel | Esperado | Estado |
| --- | --- | --- | --- | --- |
| CP-20 | Registro por UI, logout y login | E2E | Sesión visible y persistida | ✅ |
| CP-21 | Catálogo + búsqueda + detalle + Escape | E2E | Modal accesible y cierre correcto | ✅ |
| CP-22 | Carrito + checkout + confirmación | E2E | Orden `CONFIRMED` visible | ✅ |
| CP-23 | Responsive 360/768/1280 sin overflow | E2E | Sin scroll horizontal | ✅ |
| CN-45 | Anónimo en `#/admin` | E2E | Redirige al login con aviso | ✅ |
| CN-46 | CUSTOMER en `#/admin` | E2E | "Acceso restringido", sin tabla admin | ✅ |
| CP-24 | ADMIN publica y elimina un auto | E2E | Fila creada y luego eliminada | ✅ |
| CN-47 | Contraseña incorrecta en UI | E2E | Mensaje de error, sin sesión | ✅ |

---

## Trazabilidad requisito → caso

| Requisito | Casos |
| --- | --- |
| G1 Arranque reproducible | README + `docker-compose.yml` + `.env.example` (verificación manual) |
| G2 Funcionalidad | CP-01…CP-24 |
| G3 API/contrato | CP-03, CP-05, CP-11, CP-17, CN-14, CN-15, CN-25 |
| G4 Seguridad | CN-01…CN-13, CN-16, CN-22, CN-23, CN-25, CN-36, CN-42 |
| G5 Resiliencia | CN-28, CN-33…CN-35, CN-39…CN-41, CN-43, CN-44 |
| G6 Secretos | Escaneo Gitleaks + D-01 |
| G7 CI | `.github/workflows/ci.yml` |
| G8 Carga | `load-tests/k6.js` (✅ 50 VU / 5 min, p95 7,11 ms, error 0 %) |
| G9 Documentación | `docs/testing/01…09` + manual + Gherkin |

> Los casos marcados ⛔ no se ejecutaron en este entorno por falta de Docker o de la
> herramienta (ZAP/Trivy). No se reportan como aprobados: quedan a cargo del pipeline de CI
> o de una ejecución manual con Docker disponible. La prueba de carga k6 **sí** se ejecutó
> localmente (ver `07-pruebas-api-contrato-y-carga.md`).
