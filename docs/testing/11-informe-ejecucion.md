# 11 · Informe de ejecución

Entregable 4. Consolida resultados por nivel. **Regla de honestidad (G9):** solo se
informan números de corridas reales; lo que todavía no corrió figura como
⏳ PENDIENTE y no se presenta como aprobado.

- Fuente de los valores ✅: corrida local del **10/11/2026** sobre la rama
  `qa/criterios-clase15` (macOS, JDK 23 con `-Dmaven.compiler.proc=full`, Docker Desktop 29)
  para unitarias, cobertura, integración, mutación y Newman; E2E, carga y WSDL vienen de las
  corridas del 8/11 documentadas en `06` y `07`.
- Los valores "✅ CI" provienen del **run verde sobre `main`** (`b4554eb`, merge del PR #1,
  9 jobs OK): <https://github.com/Faustinofff/driveai/actions/runs/42867141379>. Los
  artefactos de ese run (`jacoco-report`, `pit-report`, `failsafe-reports`,
  `playwright-report`, `api-contract-reports`, `zap-report`, `spotbugs-report`) quedan
  disponibles 7 días; descargarlos y adjuntarlos a la entrega.
- El **run sobre el tag `v1.0-testing`** (link: _pendiente — el tag se crea recién
  después de sanear D-01, ver `14`_) es la evidencia que se muestra en la defensa; al
  crearlo, reemplazar el link de arriba y repetir esta tabla si algún número cambia.

## 1. Totales por nivel

| Nivel | Herramienta | Ejecutados | Pasados | Fallados | Bloqueados | Estado | Evidencia |
| --- | --- | --- | --- | --- | --- | --- | --- |
| Unitario + MVC (incluye los tests nuevos de DEF-07…DEF-13) | JUnit 5 / Mockito | 178 | 178 | 0 | 0 | ✅ CI (174 en local 10/11, antes de los 4 tests de DEF-13) | `target/surefire-reports` |
| Integración | Testcontainers (PostgreSQL + RabbitMQ reales) | 18 | 18 | 0 | 0 | ✅ local 10/11 (cierra D-04) · ✅ CI | `failsafe-reports` |
| Contrato SOAP | `WsdlContractTest` | 3 | 3 | 0 | 0 | ✅ local | `07` §1.3 |
| Contrato OpenAPI | Schemathesis (`not_a_server_error` + `response_schema_conformance`) | 16 operaciones / 352 casos | 352 | 0 | 0 | ✅ CI | `api-contract-reports/schemathesis.xml` |
| API | Postman + Newman, contra PostgreSQL + RabbitMQ reales | 55 requests | 55 | 0 | 0 | ✅ local 10/11 (54) · ✅ CI (55, incluye PM 55 de DEF-13) | `api-contract-reports/newman.xml` |
| E2E | Playwright (chromium) | 16 | 16 | 0 | 0 | ✅ local · ✅ CI | `playwright-report` |
| E2E accesibilidad | Playwright + axe (WCAG 2.0–2.2 A/AA, 360 y 1280 px) | 7 | 7 | 0 | 0 | ✅ CI (cierra DEF-12) | `playwright-report` |
| Carga | k6, 50 VU × 5 min | 14.950 iteraciones | 14.950 | 0 | 0 | ✅ local | `load-tests/summary.json` |
| Mutación | PIT | 223 mutantes | 166 muertos | — | — | ✅ CI (151/223 en local 10/11) | `pit-report` |
| Estabilidad | 3 corridas de la suite unitaria | 3 × 178 | 3 × 178 | 0 | 0 | ✅ CI: "Las 3 corridas dieron idéntico resultado" | job `flaky-check` |

## 2. Cobertura y mutación (capa de negocio)

| Métrica | Valor | Umbral cátedra | Estado |
| --- | --- | --- | --- |
| Líneas (JaCoCo) | 93,84 % local · 92 % CI (3.727/4.021) | ≥ 80 % | ✅ |
| Ramas (JaCoCo) | 79,78 % local · 79 % CI (221/277) | ≥ 70 % | ✅ |
| Mutation score (PIT) | 68 % local (151/223) · **74 % CI (166/223)**, cobertura de líneas de las clases mutadas 94 % | ≥ 60 % | ✅ |

> En local (JDK 23) el mutation score había bajado a 68 % respecto del 8/11 (73,5 %); en
> CI con JDK 17 dio 74 % (166/223). La diferencia es de mutantes sin cobertura según la
> JVM; en ambos casos se supera el umbral. Pendiente opcional: revisar en `pit-report`
> qué clases concentran los sobrevivientes.

## 3. Análisis estático y seguridad

| Herramienta | Resultado | Umbral | Estado |
| --- | --- | --- | --- |
| SpotBugs | 0 hallazgos | 0 | ✅ local 10/11 · ✅ CI |
| SonarCloud Quality Gate | — | Aprobado; 0 bugs Blocker/Critical; 0 vulnerabilidades; dup. ≤ 3 %; ratings A; 100 % hotspots revisados | ⏳ **todavía no configurado**: el job `sonarcloud` del run de `main` terminó en verde en 5 s solo porque no hay `SONAR_TOKEN` ni variables (emite un `::warning::` y no analiza). Configurar según `13` y **adjuntar captura del Quality Gate** |
| OWASP ZAP (API scan pasivo + activo, autenticado) | 0 High · 0 Medium · 3 Low · 3 Informational (118 PASS) | 0 High; Medium justificadas | ✅ CI (`zap-report`); detalle de las Low en `08` §11 |
| Trivy (dependencias, `CRITICAL,HIGH`) | 0 hallazgos | 0 CVE críticas/altas sin mitigación | ✅ CI (`security-scan`), con la excepción documentada en `.trivyignore` (DEF-14) |
| Gitleaks 8.30.1 (historial completo, `--log-opts="--all"`) | `no leaks found` tras sanear D-01 el 10/11 | 0 | ✅ local (mirror y clon) · en CI, paso "Gitleaks (historial completo, bloqueante)" del job `security-scan` a partir del tag |

## 4. Performance

| Métrica | Resultado | Umbral | Estado |
| --- | --- | --- | --- |
| p95 | 7,11 ms | < 500 ms | ✅ |
| Tasa de error | 0 % | < 1 % | ✅ |

Limitación declarada en `07` §4: la corrida fue sin RabbitMQ y sobre endpoints de
lectura (catálogo, detalle, carrito). **Pendiente:** repetir con `docker compose up`
completo incluyendo checkout, que es uno de los 3 endpoints críticos.

## 5. Tiempos

| Medición | Valor | Umbral |
| --- | --- | --- |
| Pipeline completo (run de `main` 42867141379; 9 jobs en paralelo, el más largo es PIT) | **3 min 01 s** (17:39:15 → 17:42:16 UTC) | ≤ 15 min |
| Pipeline completo sobre el tag `v1.0-testing` | _completar con el run del tag_ | ≤ 15 min |
| Build de imágenes + arranque (`docker compose up -d --build`, con caché parcial) | ~2 min | ≤ 15 min |
| `docker compose up` desde cero (G1) | _medir en máquina limpia_ | ≤ 15 min |

## 6. Defectos

Ver `10-registro-defectos.md` (issues `G7-11` … `G7-19` en Jira). Al cierre: 0
Bloqueantes y 0 Críticos abiertos es condición de salida: **se cumple** (D-01 cerrado el
10/11 con el historial saneado). DEF-07…DEF-10 y DEF-12…DEF-14 quedaron verificados en
el run de `main`; DEF-11 verificado salvo la pasada con el stack completo (§7).

## 7. Verificaciones manuales del 10/11

| Verificación | Resultado |
| --- | --- |
| `curl -sI http://localhost:5173/` (DEF-11) | `Content-Security-Policy`, `Strict-Transport-Security` y `X-Content-Type-Options` presentes ✅ |
| Navegación de la SPA con la consola abierta (CSP no rompe la app) | ✅ **0 violaciones de CSP** (10/11, Chromium). Método: `npm run build` y servir `frontend/dist` con un servidor local que envía las 7 cabeceras exactas de `frontend/security-headers.conf` (`curl -sI` las muestra); `/api` y `/actuator` responden 503 porque en esta máquina no hay Docker ni Java. Con la consola abierta y un listener de `securitypolicyviolation` se recorrió: home (carrusel e imágenes `https:`), `#/catalog` con filtros y estado de error, `#/login` con envío del formulario y pestaña "Crear cuenta", drawer del carrito, cambio de tema claro/oscuro, y `#/admin` / `#/ai` (redirigen al login sin sesión). Consola: solo los 503 esperados del backend simulado; ningún `Refused to …`. **Pendiente:** repetir con `docker compose up` para cubrir detalle de auto, checkout, panel admin y respuesta del asesor IA (requieren backend) |
