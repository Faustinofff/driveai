# 09 · Declaración de uso de IA

## 1. Alcance de la declaración

Este documento declara el uso de herramientas de IA generativa durante el desarrollo y
la preparación de la entrega de testing de DriveAI, en línea con el requisito de
transparencia de la consigna.

## 2. Uso en el producto (funcionalidad)

DriveAI integra **Groq Cloud** como proveedor del asesor de compra. Es una funcionalidad
del sistema, no una herramienta de desarrollo:

- El backend arma un prompt con el catálogo real de autos en stock.
- El LLM devuelve `reasoning` + `recommendedCarIds`.
- El backend **filtra los IDs alucinados** contra la base; solo recomienda autos reales.
- Sin `GROQ_API_KEY`, el asesor responde **503** y el resto del e-commerce sigue
  funcionando.
- No se usan respuestas falsas (mock) de Groq en producción. Los mocks existen solo en
  pruebas automatizadas (WireMock).

## 3. Uso en el desarrollo

Se utilizó un agente de IA como asistente para:

- Auditar el repositorio contra la consigna.
- Implementar controles faltantes (JWT, BOLA/BFLA, idempotencia, outbox, SOAP,
  cabeceras, rate limit).
- Redactar y ejecutar pruebas unitarias, de integración, E2E y de contrato.
- Configurar CI, Docker Compose, `.env.example` y documentación.

**Todas las modificaciones fueron revisadas y ejecutadas localmente.** Las métricas
reportadas provienen de corridas reales (Maven, Playwright, SpotBugs, PIT); lo que no se
pudo ejecutar se marca **NO EJECUTADO** y no se presenta como aprobado.

### 3.1 Detalle por herramienta (criterio 8.1)

| Herramienta y versión | Para qué se usó | Prompt representativo | Qué se aceptó | Qué se corrigió o descartó | Cómo se verificó |
| --- | --- | --- | --- | --- | --- |
| _Agente de IA usado durante el desarrollo inicial (commits del 11-12/9 y 8/11, autor Faustinofff) — **el equipo completa nombre y versión**_ | Implementación de controles y tests (§3) | _**completar** con un prompt real de esa etapa_ | _**completar**_ | _**completar**_ | `mvn verify`, Playwright, PIT (§4) |
| **Claude Opus 5.5** (claude.ai, sesión del 10/11/2026) | Auditoría del repo contra los criterios de la Clase 15; revisión defensiva de seguridad; tests y pipeline faltantes | "Revisá el repo contra estos criterios de evaluación (PDF de la Clase 15) y sumá lo que falte" | Correcciones DEF-07…DEF-11, tests nuevos, colección Newman extendida (54 requests), jobs de CI (Newman, Schemathesis, ZAP, SonarCloud, flaky), spec de accesibilidad, docs 02b y 10–14 | _**completar** al revisar: qué cambios se rechazaron o ajustaron_ | El asistente no podía compilar en su entorno; el equipo lo ejecutó localmente el 10/11: unitarias 174/174, cobertura 93,84 %/79,78 %, IT 18/18, PIT 68 %, Newman 54/54 contra `docker compose up`, cabeceras verificadas con `curl`. CI: run verde sobre `main` 42867141379 (ver `11`) |
| **Claude Code (Claude Fable 5.1)**, sesión del 10/11/2026 operada por Juan Ignacio Aguirre | Cierre de pendientes de la entrega: alta de los defectos en Jira (G7-11…G7-19), lectura de los artefactos del run de CI de `main` para completar `10`, `11`, `02b` y `08`, verificación de la CSP en el navegador (`11` §7) | "Esto es lo que nos faltaría, ¿podrías llevarlo a cabo?" + la lista de 4 puntos (D-01, SonarCloud, tag, docs) | Issues de Jira con la misma ficha que `10`; tablas de resultados con los números del run 42867141379; tabla de alertas Low de ZAP; chequeo de CSP sin backend; saneamiento del historial (D-01) con autorización explícita del equipo, Gitleaks bloqueante en CI y tag `v1.0-testing` | Se negó a usar la cuenta de GitHub del dueño del repo (ofrecida por chat) y pidió confirmación antes del `push --force`; no configuró SonarCloud (requiere la cuenta dueña del repo). Usó `git filter-branch` en lugar de `git filter-repo` por falta de Python. El chequeo de CSP quedó parcial por falta de Docker/Java en la máquina | Números tomados de los artefactos y del log del run de CI, no de la salida del asistente; Gitleaks corrido antes y después de la reescritura; árbol de `main` comparado con `git diff` antes del push |

Reglas que el equipo aplica a todo lo generado con IA:

- Cada test generado con IA queda **revisado y commiteado por un integrante** (autor en
  Git), que tiene que poder explicarlo en la defensa.
- Un "CUMPLE" propuesto por la IA sin test ejecutado y sin evidencia se informa como
  **NO VERIFICABLE**.

### 3.2 Caso en que la IA propuso un test incorrecto (criterio 8.1)

`CarServiceTest#deleteWhenReferencedThrowsConflict` simulaba con Mockito que
`carRepository.delete()` lanzaba `DataIntegrityViolationException`. JPA no hace eso:
el `DELETE` se ejecuta en el flush/commit. El test pasaba, pero contra una base real
el endpoint devolvía 500 (DEF-09).

- **Cómo se detectó:** revisión defensiva del código con IA, que contrastó el mock con
  el comportamiento real de JPA. El caso queda confirmado con el request Newman PM 44
  contra PostgreSQL real en CI.
- **Autoría:** _el equipo confirma si ese test fue generado con IA_. Si no lo fue, se
  reemplaza este ejemplo por otro real.
- **Corrección:** `flush()` dentro del `try` y un test que simula la falla donde ocurre.

### 3.3 Plan de IA para la 4.ª entrega y la final (criterio 8.2)

| Herramienta / uso | Objetivo medible | Riesgo que controla |
| --- | --- | --- |
| Generación de casos negativos a partir de las HU en Gherkin (LLM + revisión humana) | Mantener ≥ 40 % de casos negativos/borde; 0 criterios sin caso en la matriz `02b` | Huecos de cobertura funcional |
| Revisión defensiva de seguridad con IA en cada PR (BOLA, rate limit, headers) | 0 hallazgos High en ZAP y 0 controles evadibles (como DEF-07 y DEF-08) | Controles de seguridad evadibles |
| Pruebas visuales automatizadas (capturas de Playwright comparadas por píxel) a 360, 768 y 1280 px | 0 regresiones visuales no aprobadas por release | Roturas de layout responsive |
| Triage de defectos asistido (resumen de logs de CI y propuesta de severidad según sección 9) | Defecto registrado en Jira < 24 h después del run rojo | Defectos sin registrar (G9) |
| Evaluación del asesor IA con un set fijo de prompts y salidas esperadas (golden set) | ≥ 90 % de recomendaciones válidas; 100 % de inyecciones de prompt bloqueadas | Regresiones de calidad y seguridad del LLM |

## 4. Verificación de lo generado

| Artefacto | Verificación |
| --- | --- |
| Código backend | `mvn -B test` → 148 tests verdes; `test-compile` OK |
| Cobertura | JaCoCo 93,83 % líneas / 79,18 % ramas |
| Mutación | PIT 73,5 % (161/219) |
| Análisis estático | SpotBugs 0 hallazgos |
| Frontend | `npm run build` OK |
| E2E | Playwright 16/16 contra stack real |
| Contrato WSDL | `WsdlContractTest` 3/3 |
| Repetición | 3 corridas unitarias consecutivas verdes |

## 5. Pruebas específicas de IA

| Caso | Resultado |
| --- | --- |
| Recomendación válida (WireMock) | ✅ |
| Sin clave → 503 controlado | ✅ (E2E y unitaria) |
| Timeout del proveedor → fallback | ✅ |
| JSON inválido del LLM → fallback | ✅ |
| IDs alucinados filtrados | ✅ |
| Prompt injection pide prompt/clave | ✅ no revela `system prompt` ni `gsk_` |
| Rate limit de recomendaciones | ✅ 429 |

## 6. Limitaciones y riesgos asumidos

- El camino feliz con Groq real no se ejecutó en el E2E local (sin clave). Se validó el
  fallback y el cliente HTTP con WireMock.
- La calidad de las recomendaciones del LLM no se evalúa de forma semántica; se valida
  que los IDs existan y tengan stock.
- Los prompts se auditan en `ai_recommendation_logs`, pero no se implementó moderación de
  contenido adicional.
- El asistente de IA de desarrollo puede cometer errores; por eso cada cambio se validó
  con compilación, tests y ejecución real.

## 7. Declaración

El equipo declara que:

1. No se inventaron resultados de pruebas ni métricas.
2. Los casos no ejecutados se identifican explícitamente.
3. No se incluyeron secretos reales en el código, logs ni commits nuevos.
4. El uso de IA se limita a asistencia en desarrollo y a la funcionalidad declarada del
   asesor de compra.
