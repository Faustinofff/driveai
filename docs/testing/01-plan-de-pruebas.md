# 01 · Plan de pruebas — DriveAI

> **Trabajo Práctico Obligatorio — Testing (UADE)**
> Sistema bajo prueba: DriveAI, e-commerce de autos usados con asesor de compra por IA.
> Repositorio: `https://github.com/Faustinofff/driveai` (branch `main`).

---

## 1. Objetivo

Verificar que DriveAI cumple los requisitos funcionales, de seguridad, de resiliencia y de
calidad exigidos por la consigna, con evidencia reproducible y sin ocultar defectos ni
resultados no ejecutados.

El plan cubre los nueve criterios eliminatorios (G1–G9) y los cinco ejes de evaluación
(A–E) definidos en el enunciado:

| Eje | Foco | Documento |
| --- | --- | --- |
| A | Pruebas unitarias y cobertura | `03-pruebas-unitarias-y-cobertura.md` |
| B | Pruebas de integración con dependencias reales | `04-pruebas-de-integracion.md` |
| C | Pruebas de mutación y calidad de la suite | `05-pruebas-mutacion.md` |
| D | Pruebas end-to-end, API, contrato y carga | `06-pruebas-e2e.md`, `07-pruebas-api-contrato-y-carga.md` |
| E | Seguridad, resiliencia y análisis estático | `08-seguridad-y-resiliencia.md` |

---

## 2. Alcance

### En alcance

- API REST: autenticación, catálogo, carrito, órdenes, asesor IA y cotización SOAP.
- Persistencia PostgreSQL 16 + migraciones Flyway.
- Mensajería RabbitMQ: outbox, consumidor idempotente y Dead Letter Queue.
- Integración externa SOAP (timeout, reintento, circuit breaker, fallback, XXE).
- Integración externa Groq (timeout 5 s, rate limit, degradación a 503).
- SPA React: registro/login, catálogo, detalle, carrito, checkout, panel admin,
  responsive y accesibilidad básica.
- Seguridad: JWT, BOLA/IDOR, BFLA, mass assignment, XSS, XXE, fuerza bruta,
  cabeceras y CORS.
- CI: pipeline reproducible con umbrales de cobertura, mutación y análisis estático.

### Fuera de alcance

- Pasarela de pago real (el checkout confirma la orden sin cobro).
- Envío de correos, facturación electrónica y logística.
- Aplicaciones móviles nativas.
- Infraestructura cloud productiva (el despliegue es Docker Compose local).

---

## 3. Estrategia por niveles

```
        ┌─────────────────────────────┐
        │  E2E (Playwright) 16 casos  │  flujos de usuario reales
        ├─────────────────────────────┤
        │  API/contrato (REST Assured│  OpenAPI + WSDL + códigos HTTP
        │  + WSDL)                    │
        ├─────────────────────────────┤
        │  Integración (Testcontainers│  PostgreSQL + RabbitMQ reales
        │  + WireMock)                │
        ├─────────────────────────────┤
        │  Unitarias (JUnit 5 + Mockito│  lógica de servicios/seguridad
        │  + @WebMvcTest)             │
        └─────────────────────────────┘
```

Cada nivel se ejecuta de forma independiente y automatizable:

| Nivel | Comando | Requiere Docker | Estado local |
| --- | --- | --- | --- |
| Unitarias + MVC | `mvn -B test jacoco:report` | No | ✅ ejecutado |
| Cobertura con umbrales | `mvn -B verify -Pcoverage` | No | ✅ ejecutado |
| Mutación | `mvn -B pitest:mutationCoverage` | No | ✅ ejecutado |
| Análisis estático | `mvn -B spotbugs:check` | No | ✅ ejecutado |
| Integración | `mvn -B verify -Pintegration` | Sí | ⛔ NO EJECUTADO localmente (sin Docker) — se ejecuta en CI |
| E2E | `npm run test:e2e` | No (stack nativo) | ✅ 16/16 ejecutado |
| Carga | `k6 run load-tests/k6.js` | No | ✅ 50 VU / 5 min (p95 7,11 ms, error 0 %) |
| Seguridad dinámica | OWASP ZAP baseline/active | Sí / red | ⛔ NO EJECUTADO (sin Docker) |

---

## 4. Criterios de entrada y salida

### Entrada

- Código compilable (`mvn -B -DskipTests test-compile` y `npm run build`).
- Migraciones Flyway aplicables sobre una base limpia.
- Variables de entorno documentadas en `.env.example`.
- Stack levantable con `docker compose up -d --build` o en forma nativa.

### Salida (gates)

| Métrica | Umbral | Resultado local |
| --- | --- | --- |
| Cobertura de líneas | ≥ 80 % | **93,83 %** |
| Cobertura de ramas | ≥ 70 % | **79,18 %** |
| Mutación (PIT) | ≥ 60 % | **73,5 %** (161/219) |
| Casos negativos / borde | ≥ 40 % del total | **69 %** (54/78, ver matriz) |
| E2E críticos | 5 escenarios | **16 tests / 6 specs** |
| CI | verde en ≤ 15 min | pipeline definido; ejecución pendiente en GitHub |
| Carga | 50 VU · 5 min · p95 < 500 ms · error < 1 % | **p95 7,11 ms · error 0 %** (14.950 iteraciones) |
| SpotBugs | 0 hallazgos | **0 BugInstance** |

---

## 5. Roles y responsabilidades

| Rol | Responsabilidad |
| --- | --- |
| Desarrollo | Implementar features y pruebas unitarias/integración. |
| QA | Diseñar matriz de casos, ejecutar E2E/API/carga, registrar evidencias. |
| Seguridad | Revisar JWT/BOLA/BFLA, ejecutar ZAP y escaneo de dependencias. |
| Docente | Validar entregables, reproducibilidad y honestidad de resultados. |

---

## 6. Gestión de defectos

Cada defecto se registra con: identificador, severidad, caso que lo detectó, evidencia y
estado. Los defectos conocidos al cierre se documentan en la sección **Riesgos y deuda
técnica** de este plan y en `08-seguridad-y-resiliencia.md`.

Severidades:

- **S1 Bloqueante**: impide compilar, arrancar o autenticarse.
- **S2 Alta**: vulnerabilidad explotable o pérdida de datos.
- **S3 Media**: funcionalidad incorrecta con workaround.
- **S4 Baja**: cosmético o deuda técnica.

### Defectos y limitaciones conocidas

| ID | Severidad | Descripción | Estado |
| --- | --- | --- | --- |
| D-01 | S2 | El historial Git contiene una contraseña de demo (ver ADMIN_PASSWORD en .env) en el commit 3f19436. El código actual no la contiene; Gitleaks queda no bloqueante hasta que el equipo autorice reescribir la historia. | Abierto / requiere autorización |
| D-02 | S3 | Sin `GROQ_API_KEY` el asesor IA responde 503; el E2E valida el fallback, no una recomendación real de Groq. | Aceptado por diseño |
| D-03 | S4 | `OrderEventsListener` solo registra el evento; no hay procesamiento downstream adicional. | Aceptado / documentado |
| D-04 | S3 | Las pruebas de integración Testcontainers no pudieron ejecutarse en el entorno local por falta de Docker; quedan cubiertas por el job de CI. | Pendiente de CI |
| D-05 | S3 | k6, ZAP y Trivy no se ejecutaron localmente (herramientas/daemon no disponibles). | Pendiente de CI / manual |

---

## 7. Riesgos

| Riesgo | Impacto | Mitigación |
| --- | --- | --- |
| Docker no disponible en el entorno de corrección | IT, ZAP y compose no corren | CI con Testcontainers + guía de ejecución nativa en README |
| Groq caído o sin clave | IA no responde | Fallback 503, timeout 5 s, circuit breaker y E2E del fallback |
| Broker caído | Eventos no publicados | Outbox PENDING + reintento + DLQ + IT de recuperación |
| SOAP lento o con faults | Checkout/cotización degradados | Timeout 5 s, retry acotado, circuit breaker, fallback; faults de negocio no se reintentan |
| Reloj/zonas horarias | Fechas inconsistentes | `Instant` + serialización ISO-8601 |
| Fuerza bruta de login | Toma de cuentas | Rate limit 5/min + `429` + `Retry-After` |

---

## 8. Evidencia y reproducibilidad

- Los comandos de cada nivel están en el `README.md` y en cada documento de `docs/testing/`.
- Los reportes generados (`target/site/jacoco`, `target/pit-reports`, `playwright-report`)
  se publican como artefactos en CI.
- Las métricas de este plan provienen de ejecuciones reales registradas en la sección
  **Resultados** de cada documento; lo no ejecutado se marca explícitamente
  **NO EJECUTADO**.
