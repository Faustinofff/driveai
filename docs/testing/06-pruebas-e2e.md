# 06 · Pruebas end-to-end (eje D)

## 1. Objetivo

Validar los flujos críticos del usuario a través de la UI real (React + Vite) contra el
backend real y PostgreSQL, sin mocks en el navegador.

## 2. Herramientas

- **Playwright 1.65.0** + Chromium headless.
- `frontend/playwright.config.ts`: `testDir=./e2e`, workers=1, retries=0 local / 1 en CI,
  trace + screenshot + video **solo en fallo**.
- `webServer` levanta `npm run dev` automáticamente si no está corriendo.

## 3. Comando

```bash
cd frontend
npm install
npx playwright install chromium
ADMIN_EMAIL=admin@driveai.com \
ADMIN_PASSWORD='<password-de-demo>' \
E2E_API_URL=http://localhost:8080 \
npm run test:e2e
```

Reporte: `frontend/playwright-report/index.html`
Evidencia de fallos: `frontend/test-results/<test>/trace.zip` + video + screenshot.

## 4. Escenarios implementados (6 specs / 16 tests)

| Spec | Tests | Flujo |
| --- | --- | --- |
| `auth.spec.ts` | 3 | Registro por UI, logout, login; login inválido; redirección de vista protegida |
| `catalog.spec.ts` | 3 | Catálogo, búsqueda por marca, detalle en modal, cierre con Escape, estado vacío y limpieza |
| `cart-checkout.spec.ts` | 2 | Agregar al carrito, checkout, confirmación `CONFIRMED`; checkout anónimo → login |
| `ai-advisor.spec.ts` | 2 | Consulta al asesor con fallback controlado; prompt injection sin fuga de datos |
| `admin-access.spec.ts` | 3 | Anónimo → login; CUSTOMER → "Acceso restringido"; ADMIN crea y elimina un auto |
| `responsive.spec.ts` | 3 | Home y catálogo sin overflow en 360 / 768 / 1280 px |

Se superan los **5 escenarios E2E** exigidos.

### Cobertura funcional E2E

- Alta de usuario y sesión persistida (Zustand + `localStorage`).
- Navegación por hash (`#/login`, `#/catalog`, `#/ai`, `#/admin`).
- Catálogo paginado, búsqueda con debounce, detalle de vehículo en modal accesible.
- Carrito local + checkout real con `Idempotency-Key` y orden `CONFIRMED`.
- Autorización en UI: anónimo, CUSTOMER y ADMIN.
- Responsive sin scroll horizontal.
- Fallback de IA cuando no hay `GROQ_API_KEY` (503 controlado).

## 5. Resultado real de la ejecución local

Entorno usado:

| Componente | Versión / puerto |
| --- | --- |
| PostgreSQL | 16.6 nativo, `localhost:5433/cardb` |
| Backend | Spring Boot 3.3.5, `http://localhost:8080` |
| Frontend | Vite 5, `http://localhost:5173` |
| Navegador | Chromium headless (Playwright 1.65.0) |
| IA | Sin `GROQ_API_KEY` (se valida el fallback 503) |

```
Running 16 tests using 1 worker
...
16 passed (19.0s)
```

| Resultado | Cantidad |
| --- | --- |
| Aprobados | **16** |
| Fallidos | 0 |
| Omitidos | 0 |

### Escenario ADMIN

El test de administración se ejecutó con el usuario demo definido por variables de
entorno (`ADMIN_EMAIL` / `ADMIN_PASSWORD`). El test se **omite automáticamente** si esas
variables no están definidas, para no fallar en un entorno sin credenciales.

## 6. Estabilidad (flakiness)

La suite unitaria se ejecutó **3 veces consecutivas**:

```bash
for i in 1 2 3; do mvn -B test || exit 1; done
```

Resultado: **3/3 corridas verdes** (`Tests run: 148, Failures: 0, Errors: 0`).

La suite E2E se ejecutó dos veces completa en la misma sesión (primera corrida con
fallos de selectores, corregidos; corrida final 16/16). Los fallos iniciales fueron
defectos reales de accesibilidad/configuración detectados por la suite:

1. `VITE_API_URL` con origen (`http://localhost:8080`) generaba llamadas a `/cars` en
   lugar de `/api/cars`. Se normalizó en `src/services/api.ts`.
2. Los `<label>` no estaban asociados a los inputs (`htmlFor`/`id`), lo que rompía
   `getByLabel` y la accesibilidad. Se corrigió en login y formulario admin.
3. "Limpiar filtros" no limpiaba la búsqueda del navbar. Se agregó `onClearSearch`.
4. El filtro del catálogo borraba el hash de la URL al escribir los query params. Se
   preserva `#/catalog`.
5. Un selector ambiguo (`Productos` en navbar y footer) se acotó al `banner`.

Estos hallazgos se documentan porque son parte del valor de la automatización; no se
ocultaron ni se “arreglaron” solo en el test.

## 7. Limitaciones

- El detalle de vehículo se implementó como **modal** dentro del catálogo (no hay ruta
  `/cars/:id`), decisión consistente con la navegación por hash de la SPA.
- Las pruebas de IA validan el camino de degradación (sin clave) y la resistencia a
  prompt injection, no la calidad de una recomendación real de Groq. Para ejecutar el
  camino feliz de Groq se debe definir `GROQ_API_KEY` antes de levantar el backend.
- No se ejecutó E2E en modo `CI` (retries=1) localmente; el pipeline lo hará.
