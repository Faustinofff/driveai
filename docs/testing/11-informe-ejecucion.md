# 11 · Informe de ejecución

Entregable 4. Consolida resultados por nivel. **Regla de honestidad (G9):** solo se
informan números de corridas reales; lo que todavía no corrió figura como
⏳ PENDIENTE y no se presenta como aprobado.

- Fuente de los valores ✅: corrida local del **10/11/2026** sobre la rama
  `qa/criterios-clase15` (macOS, JDK 23 con `-Dmaven.compiler.proc=full`, Docker Desktop 29)
  para unitarias, cobertura, integración, mutación y Newman; E2E, carga y WSDL vienen de las
  corridas del 8/11 documentadas en `06` y `07`.
- Los valores ⏳ se completan con el **run de CI sobre el tag `v1.0-testing`**
  (link: _completar_). Ese run es la evidencia que se muestra en la defensa.

## 1. Totales por nivel

| Nivel | Herramienta | Ejecutados | Pasados | Fallados | Bloqueados | Estado | Evidencia |
| --- | --- | --- | --- | --- | --- | --- | --- |
| Unitario + MVC (incluye los 26 tests nuevos) | JUnit 5 / Mockito | 174 | 174 | 0 | 0 | ✅ local 10/11 | `target/surefire-reports` |
| Integración | Testcontainers (PostgreSQL + RabbitMQ reales) | 18 | 18 | 0 | 0 | ✅ local 10/11 (cierra D-04) | `failsafe-reports` |
| Contrato SOAP | `WsdlContractTest` | 3 | 3 | 0 | 0 | ✅ local | `07` §1.3 |
| Contrato OpenAPI | Schemathesis | — | — | — | — | ⏳ CI | `api-contract-reports/schemathesis.xml` |
| API | Postman + Newman, contra `docker compose up` | 54 requests / 54 aserciones | 54 | 0 | 0 | ✅ local 10/11 · ⏳ CI | `api-contract-reports/newman.xml` |
| E2E | Playwright | 16 | 16 | 0 | 0 | ✅ local | `06` §5 |
| E2E accesibilidad | Playwright + axe | 7 | — | — | — | ⏳ CI | `playwright-report` |
| Carga | k6, 50 VU × 5 min | 14.950 iteraciones | 14.950 | 0 | 0 | ✅ local | `load-tests/summary.json` |
| Mutación | PIT | 223 mutantes | 151 muertos | — | — | ✅ local 10/11 | `target/pit-reports` |
| Estabilidad | 3 corridas de la suite unitaria | 3 | — | — | — | ⏳ CI | job `flaky-check` |

## 2. Cobertura y mutación (capa de negocio)

| Métrica | Valor | Umbral cátedra | Estado |
| --- | --- | --- | --- |
| Líneas (JaCoCo) | 93,84 % | ≥ 80 % | ✅ |
| Ramas (JaCoCo) | 79,78 % | ≥ 70 % | ✅ |
| Mutation score (PIT) | 68 % (151/223) · test strength 79 % | ≥ 60 % | ✅ |

> El mutation score bajó respecto del 8/11 (73,5 %, 161/219) y las mutaciones sin
> cobertura subieron de 12 a 32. Pendiente: comparar con el run de CI (JDK 17) y revisar
> en `target/pit-reports` qué clases concentran los sobrevivientes.

## 3. Análisis estático y seguridad

| Herramienta | Resultado | Umbral | Estado |
| --- | --- | --- | --- |
| SpotBugs | 0 hallazgos | 0 | ✅ local 10/11 |
| SonarCloud Quality Gate | — | Aprobado; 0 bugs Blocker/Critical; 0 vulnerabilidades; dup. ≤ 3 %; ratings A; 100 % hotspots revisados | ⏳ requiere configurar `SONAR_TOKEN` (`13`) — **adjuntar captura del Quality Gate** |
| OWASP ZAP (API scan autenticado) | — | 0 High; Medium justificadas | ⏳ CI (`zap-report`) |
| Trivy (dependencias) | — | 0 CVE críticas/altas sin mitigación | ⏳ CI (`security-scan`) |
| Gitleaks | 1 hallazgo histórico (D-01) | 0 | ❌ hasta sanear el historial |

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
| Pipeline completo (job más largo, en paralelo) | _completar con el run del tag_ | ≤ 15 min |
| Build de imágenes + arranque (`docker compose up -d --build`, con caché parcial) | ~2 min | ≤ 15 min |
| `docker compose up` desde cero (G1) | _medir en máquina limpia_ | ≤ 15 min |

## 6. Defectos

Ver `10-registro-defectos.md`. Al cierre: 0 Bloqueantes y 0 Críticos abiertos es
condición de salida; hoy **D-01 (Crítica) sigue abierto**.

## 7. Verificaciones manuales del 10/11

| Verificación | Resultado |
| --- | --- |
| `curl -sI http://localhost:5173/` (DEF-11) | `Content-Security-Policy`, `Strict-Transport-Security` y `X-Content-Type-Options` presentes ✅ |
| Navegación de la SPA con la consola abierta (CSP no rompe la app) | _completar_ |
