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
