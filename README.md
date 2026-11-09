# 🚗 DriveAI — E-commerce de autos con asesor de compra por IA

Plataforma completa de venta de autos usados preparada para la evaluación de Testing del
TP Obligatorio (UADE).

- **Backend**: Spring Boot 3.3 (Java 17) + Spring Security (JWT) + Spring Data JPA +
  PostgreSQL 16 + Flyway + Bucket4j + Resilience4j + Spring AMQP + SpringDoc.
- **Frontend**: React 18 + Vite + TypeScript + Tailwind CSS + TanStack Query + Zustand.
- **IA**: recomendador con Groq Cloud (`groq/compound-mini`) con rate limiting,
  anti-alucinación de IDs y auditoría de prompts.
- **Mensajería**: RabbitMQ (exchange/queue durable, DLQ, outbox transaccional,
  consumidor idempotente).
- **Integración externa**: cliente SOAP con timeout de 5 s, reintento acotado,
  circuit breaker, fallback y parser endurecido contra XXE. Mock SOAP (WireMock)
  con WSDL publicado incluido en Docker Compose.

---

## ✨ Funcionalidades

- Catálogo paginado y filtrable (marca, precio, año, combustible, carrocería, búsqueda).
  **Público**, sin login.
- Cuentas y roles con JWT: `CUSTOMER` y `ADMIN`.
- Carrito **propiedad del usuario autenticado** (derivada del JWT; no se acepta
  `sessionId` ni `userId` del cliente) con `priceSnapshot` congelado.
- Checkout transaccional con bloqueo pesimista, validación de stock e inventario descontado.
- `Idempotency-Key` en checkout: un POST repetido crea una sola orden y devuelve la misma
  respuesta.
- Control de concurrencia optimista con `ETag`/`If-Match` en la edición de autos
  (`412 Precondition Failed` si el recurso cambió).
- Máquina de estados de órdenes (`PENDING → CONFIRMED → CANCELLED`) con `409` ante
  transiciones inválidas y restauración de stock al cancelar.
- Panel de administración en el frontend: alta/edición/borrado de autos.
- Asesor de compra IA que solo recomienda autos reales en stock (filtra IDs alucinados).
- Rate limit de login (anti fuerza bruta) y de recomendaciones IA (`429` + `Retry-After`).
- Errores uniformes **RFC 9457 / RFC 7807** (`application/problem+json`) para
  400/401/403/404/409/412/422/429/5xx, sin stack traces ni datos internos.
- Outbox + RabbitMQ: los eventos de orden se publican de forma confiable; el consumidor
  es idempotente y los mensajes inválidos van a la DLQ.
- Migraciones versionadas Flyway + validación de esquema (`ddl-auto=validate`).
- Cabeceras de seguridad (CSP, HSTS, `X-Content-Type-Options`, frame deny, referrer policy)
  y CORS con allowlist (sin comodín con credenciales).
- Despliegue unificado con Docker Compose y **ningún secreto hardcodeado**.

---

## 🧱 Arquitectura

```
┌──────────────┐        ┌──────────────────────────────┐        ┌─────────────────┐
│   Browser    │  HTTP  │   Frontend (React + Vite)    │        │   Groq Cloud    │
│  React SPA   │ ─────▶ │  Nginx (proxy /api)          │        │  /chat/completions
└──────────────┘        └──────────────┬──────────────┘        └─────────────────┘
                                       │ JSON (RFC 9457)                  ▲
                                       ▼                                  │ HTTPS
                           ┌──────────────────────────────┐               │
                           │  Backend Spring Boot 3.3     │───────────────┘
                           │  JWT · Car · Cart · Order    │
                           │  Outbox · Resilience4j       │──────▶ Mock SOAP (WireMock + WSDL)
                           └───────┬──────────────┬───────┘
                                   │ JDBC         │ AMQP
                                   ▼              ▼
                    ┌────────────────────┐  ┌────────────────────────┐
                    │  PostgreSQL 16     │  │  RabbitMQ 3.13         │
                    │  Flyway            │  │  order.exchange        │
                    │  outbox_events     │  │  order.queue + DLQ     │
                    └────────────────────┘  └────────────────────────┘
```

---

## 📁 Estructura del repositorio

```
.
├── backend/                          # Spring Boot 3 + Maven
│   ├── pom.xml                       # JaCoCo, PIT, SpotBugs, Testcontainers, REST Assured
│   ├── Dockerfile
│   └── src/
│       ├── main/java/com/driveai/
│       │   ├── advice/               # @ControllerAdvice RFC 9457
│       │   ├── ai/                   # GroqApiClient + HttpGroqApiClient
│       │   ├── config/               # RestClient, Soap, OpenAPI, AdminInitializer
│       │   ├── controller/           # Car / Cart / Order / AiAdvisor / Auth / SoapQuote
│       │   ├── domain/               # enums + entidades JPA
│       │   ├── dto/                  # records Request/Response
│       │   ├── exception/            # excepciones de negocio
│       │   ├── messaging/            # Outbox, publisher, consumer, DLQ topology
│       │   ├── rate/                 # RateLimitService + LoginRateLimitService
│       │   ├── repository/           # repositorios Spring Data
│       │   ├── security/             # JwtService, filter, SecurityConfig, UserPrincipal
│       │   ├── service/              # Car / Cart / Order / GroqAdvisor / Auth / Idempotency
│       │   ├── soap/                 # SoapVehicleClient + VehicleQuote
│       │   └── specification/        # CarSpecification + CarFilters
│       ├── main/resources/
│       │   ├── application.yml       # perfiles dev / prod
│       │   └── db/migration/         # V1 … V8
│       └── test/java/...             # unitarias + integración (Testcontainers) + MVC
├── frontend/                         # React + Vite + TypeScript
│   ├── package.json
│   ├── Dockerfile
│   ├── nginx.conf
│   ├── playwright.config.ts          # E2E
│   └── src/
│       ├── components/               # Navbar, CarCard, Filters, CartDrawer, AiChat…
│       ├── hooks/                    # useCars, useDebounce
│       ├── pages/                    # CatalogPage, AiConciergePage, LoginPage, AdminPage
│       ├── services/api.ts           # cliente Axios tipado + interceptor JWT
│       ├── store/                    # cartStore + authStore (Zustand + persist)
│       └── types/                    # interfaces TS estrictas
├── docker/soap-mock/                 # WireMock: mappings + WSDL
├── load-tests/k6.js                  # prueba de carga (50 VUs, 5 min)
├── docs/testing/                     # entregables de QA
├── .github/workflows/ci.yml          # pipeline de CI
├── .env.example                      # plantilla de variables (0 secretos)
├── docker-compose.yml                # postgres + rabbitmq + soap-mock + backend + frontend
└── api-tests.http                    # colección REST Client / IntelliJ
```

---

## 🚀 Inicio rápido con Docker Compose (G1)

Requisitos: Docker Engine + Docker Compose v2. No hace falta instalar Java, Node ni
PostgreSQL en la máquina.

```bash
# 1) Copiar la plantilla de variables
cp .env.example .env          # Windows PowerShell: copy .env.example .env

# 2) Completar en .env las variables OBLIGATORIAS:
#    JWT_SECRET       → openssl rand -base64 48
#    ADMIN_EMAIL      → admin@driveai.com
#    ADMIN_PASSWORD   → contraseña que elijas para la demo
#    GROQ_API_KEY     → opcional (sin clave el asesor IA responde 503)

# 3) Levantar TODO con un único comando
docker compose up -d --build

# 4) Verificar
docker compose ps
curl http://localhost:8080/actuator/health     # → {"status":"UP"}
```

Servicios y puertos:

| Servicio        | URL                                              | Puerto |
|-----------------|--------------------------------------------------|--------|
| Frontend        | `http://localhost:5173`                          | 5173   |
| Backend API     | `http://localhost:8080`                          | 8080   |
| Swagger UI      | `http://localhost:8080/swagger-ui.html`          | 8080   |
| OpenAPI JSON    | `http://localhost:8080/v3/api-docs`              | 8080   |
| PostgreSQL      | `jdbc:postgresql://localhost:5432/cardb`         | 5432   |
| RabbitMQ        | `amqp://localhost:5672`                          | 5672   |
| RabbitMQ panel  | `http://localhost:15672` (guest/guest por defecto) | 15672 |
| Mock SOAP       | `http://localhost:8089/soap/vehicles`            | 8089   |
| Mock SOAP WSDL  | `http://localhost:8089/soap/vehicles?wsdl`       | 8089   |
| WireMock admin  | `http://localhost:8089/__admin`                  | 8089   |

Detener / limpiar:

```bash
docker compose down          # detener
docker compose down -v       # detener y borrar volúmenes (reset total de BD y broker)
```

> El backend espera los healthchecks de PostgreSQL y RabbitMQ y aplica las migraciones
> Flyway automáticamente. Si falta una variable obligatoria, el contenedor del backend
> se detiene con un mensaje explícito (`Falta … en .env`).

### 👤 Usuarios para ingresar

| Rol      | Email                    | Contraseña                                            |
|----------|--------------------------|-------------------------------------------------------|
| ADMIN    | `ADMIN_EMAIL` de `.env` (recomendado `admin@driveai.com`) | `ADMIN_PASSWORD` definida en `.env` |
| CUSTOMER | cualquiera que se registre desde `/#/login` | la que elija al registrarse |

El administrador se crea automáticamente en el primer arranque si no existe. La contraseña
**no** está versionada: se define en `.env`.

---

## 💻 Ejecución local sin Docker (alternativa)

Si Docker no está disponible, se puede correr el stack nativo con JDK 17, Maven, Node 20+
y PostgreSQL 16 locales:

```bash
# 1) PostgreSQL en localhost:5432 con base cardb y usuario cardb_user/cardb_pass
#    (o exportar SPRING_DATASOURCE_URL/USERNAME/PASSWORD)

# 2) RabbitMQ en localhost:5672 (guest/guest)
#    Sin broker el backend arranca, pero los eventos outbox quedan PENDING.

# 3) Mock SOAP (opcional, para GET /api/soap/quote/{id})
#    Levantar docker/soap-mock o cualquier mock que responda en SOAP_BASE_URL.

# 4) Backend
cd backend
export JWT_SECRET="$(openssl rand -base64 48)"
export ADMIN_EMAIL=admin@driveai.com
export ADMIN_PASSWORD=tu_password_demo
export GROQ_API_KEY=            # opcional
mvn spring-boot:run

# 5) Frontend
cd frontend
npm install
npm run dev                     # http://localhost:5173
```

---

## 🧪 Estrategia de pruebas y comandos

Toda la evidencia de ejecución se guarda en `docs/testing/`.

### Unitarias + MVC + cobertura (sin Docker)

```bash
cd backend
mvn -B test                     # JUnit 5 + Mockito + @WebMvcTest
mvn -B test jacoco:report       # reporte en target/site/jacoco/
mvn -B verify -Pcoverage        # falla si línea < 80% o rama < 70%
```

### Integración con dependencias reales (Testcontainers, requiere Docker)

```bash
cd backend
mvn -B verify -Pintegration     # PostgreSQL 16 + RabbitMQ reales en contenedores
```

Cubre: migraciones Flyway, unicidad de carrito por usuario, versión optimista,
propiedad de órdenes, reserva atómica de idempotencia, deduplicación de mensajes y
topología/consumo real de RabbitMQ.

### Mutation testing (PIT)

```bash
cd backend
mvn -B pitest:mutationCoverage  # umbral 60%; reporte en target/pit-reports/
```

### Análisis estático (SpotBugs)

```bash
cd backend
mvn -B verify                   # incluye spotbugs (target/spotbugsXml.xml)
```

### Pruebas de API (contrato HTTP completo)

```bash
cd backend
mvn -B verify -Pintegration     # incluye ApiContractIT (requiere Docker)
```

`ApiContractIT` valida OpenAPI, auth (201/409/401/400), catálogo, 401/403, ETag/412,
mass assignment, BOLA en carrito y órdenes, idempotencia, 409 en transiciones y
`/actuator/health`. La colección manual equivalente está en `api-tests.http` y la de
Postman/Newman en `postman/DriveAI.postman_collection.json`.

### Pruebas de contrato (WSDL)

```bash
cd backend
mvn -B test -Dtest=WsdlContractTest
```

Valida que el WSDL del mock SOAP describa la operación `getVehicleQuote`, sus mensajes,
el binding document/literal y el `address` del servicio.

### E2E con Playwright (requiere backend + frontend levantados)

```bash
cd frontend
npm install
npx playwright install chromium
ADMIN_EMAIL=admin@driveai.com ADMIN_PASSWORD='<password-demo>' \
  E2E_API_URL=http://localhost:8080 npm run test:e2e   # 6 specs / 16 tests
```

Escenarios: registro/login, catálogo + búsqueda + detalle, carrito + checkout,
asesor IA (con fallback si no hay clave), autorización de administración (anónimo,
CUSTOMER y ADMIN) y responsive 360/768/1280 px.

### Prueba de carga (k6)

```bash
k6 run load-tests/k6.js
```

Escenario: 50 usuarios virtuales durante 5 minutos sobre tres endpoints críticos
(catálogo, detalle y carrito autenticado). Objetivos: p95 < 500 ms y error < 1%. No se
usa `/api/auth/login` para no mezclar el rate limit anti fuerza bruta con la medición.

Resultado local (2026-11-08, k6 v2.3.0): **14.950 iteraciones**, **p95 7,11 ms**,
**error 0 %**, umbrales cumplidos (exit 0). Evidencia en `load-tests/summary.json` y en
`docs/testing/07-pruebas-api-contrato-y-carga.md`.

### Tres ejecuciones consecutivas (detección de flakiness)

```bash
cd backend
for i in 1 2 3; do mvn -B -q test || exit 1; done
```

---

## 🔌 Endpoints principales

### Autenticación — `/api/auth` (público)
| Método | Ruta                  | Descripción |
| ------ | --------------------- | ----------- |
| POST   | `/api/auth/register`  | `{ email, password, fullName }` → 201 `{ token, user }` |
| POST   | `/api/auth/login`     | `{ email, password }` → `{ token, user }`; 401 / 429 |

El token es un JWT firmado con `uid`/`role` (subject = email) y se envía como
`Authorization: Bearer <token>`. Se valida firma, algoritmo, issuer y expiración.

### Catálogo — `/api/cars` (público)
| Método | Ruta            | Descripción |
| ------ | --------------- | ----------- |
| GET    | `/api/cars`     | Paginado + filtros (`brand`, `minPrice`, `maxPrice`, `minYear`, `fuelType`, `bodyType`, `q`) |
| GET    | `/api/cars/{id}` | Detalle + `ETag` (404 si no existe) |

### Catálogo — gestión (solo `ADMIN`)
| Método | Ruta            | Descripción |
| ------ | --------------- | ----------- |
| POST   | `/api/cars`     | Alta (201 + `ETag`) |
| PUT    | `/api/cars/{id}` | Edición; acepta `If-Match`; 412 si está desactualizado |
| DELETE | `/api/cars/{id}` | Borra; 409 si tiene ventas |

### Carrito — `/api/cart` (Bearer; dueño = JWT)
| Método | Ruta                        | Descripción |
| ------ | --------------------------- | ----------- |
| GET    | `/api/cart`                 | Carrito del usuario autenticado (vacío si no existe) |
| POST   | `/api/cart/add`             | `{ carId, quantity }` |
| PUT    | `/api/cart/update/{itemId}` | `{ quantity }` (404 si el ítem es de otro usuario) |
| DELETE | `/api/cart/remove/{itemId}` | Elimina una línea propia |

### Órdenes — `/api/orders` (Bearer)
| Método | Ruta                  | Descripción |
| ------ | --------------------- | ----------- |
| POST   | `/api/orders/checkout` | `{ items:[{carId,quantity}] }` + `Idempotency-Key` opcional → orden `CONFIRMED` |
| GET    | `/api/orders`         | Historial propio |
| GET    | `/api/orders/{id}`    | Detalle propio (404 si es de otro usuario) |
| PATCH  | `/api/orders/{id}/status` | Cambio de estado (ADMIN o cancelación propia); 409 si es inválido |
| POST   | `/api/orders/{id}/cancel` | Cancela una orden propia; 409 si ya está cancelada |

### Cotización SOAP — `/api/soap`
| Método | Ruta                    | Descripción |
| ------ | ----------------------- | ----------- |
| GET    | `/api/soap/quote/{carId}` | Cotización externa con fallback controlado |

### Asesor IA — `/api/ai` (Bearer)
| Método | Ruta                | Descripción |
| ------ | ------------------- | ----------- |
| POST   | `/api/ai/recommend` | `{ prompt, sessionId }` → `{ sessionId, reasoning, recommendations[] }` |

Códigos de error uniformes (`application/problem+json`):

| Código | Caso |
| ------ | ---- |
| 400 | Validación de campos / JSON malformado |
| 401 | Token ausente, inválido, expirado o de otro emisor |
| 403 | Autenticado sin rol `ADMIN` |
| 404 | Recurso inexistente o de otro usuario |
| 409 | Stock insuficiente / transición inválida / Fault de negocio SOAP |
| 412 | `If-Match` desactualizado |
| 422 | Regla de negocio no procesable |
| 429 | Rate limit excedido (+ `Retry-After`) |
| 503 | IA o servicio externo no disponible (con fallback) |

---

## 🧠 Cómo funciona el asesor IA (Groq)

1. **Rate limiting** por `sessionId|IP` (Bucket4j, default 5/min).
2. **Prompt de sistema** con catálogo JSON compacto de autos con stock.
3. Salida estricta `{"reasoning": "...", "recommendedCarIds": [...]}`.
4. **Anti-alucinación**: los IDs se contrastan contra `cars` con `stock > 0`; los
   inventados se filtran.
5. **Fallback**: 429/timeout/JSON inválido → `503` con mensaje amigable. Sin
   `GROQ_API_KEY` el resto del e-commerce sigue funcionando y la IA responde 503.
6. **Auditoría** en `ai_recommendation_logs`.
7. **Aislamiento**: la IA no modifica stock, no crea órdenes ni accede a datos de otros
   usuarios; solo devuelve IDs del catálogo real.

Casos de testing de IA (prompt injection, datos inventados, proveedor caído, etc.) en
`docs/testing/09-declaracion-ia.md`.

---

## ⚙️ Variables de entorno

| Variable | Descripción | Default |
| -------- | ----------- | ------- |
| `POSTGRES_DB` | Nombre de la base | `cardb` |
| `POSTGRES_USER` | Usuario de la base | `cardb_user` |
| `POSTGRES_PASSWORD` | Contraseña de la base | `cardb_pass` (cámbiala en prod) |
| `POSTGRES_PORT` | Puerto expuesto de PostgreSQL | `5432` |
| `RABBITMQ_USER` / `RABBITMQ_PASSWORD` | Credenciales del broker | `guest` / `guest` |
| `RABBITMQ_PORT` / `RABBITMQ_MANAGEMENT_PORT` | Puertos AMQP / panel | `5672` / `15672` |
| `GROQ_API_KEY` | API Key de Groq (opcional) | vacía |
| `GROQ_MODEL` | Modelo LLM | `groq/compound-mini` |
| `GROQ_API_BASE_URL` | URL base compatible OpenAI | `https://api.groq.com/openai/v1` |
| `SPRING_PROFILES_ACTIVE` | Perfil Spring (`dev`/`prod`) | `dev` |
| `SERVER_PORT` | Puerto del backend | `8080` |
| `RATE_LIMIT_PER_MINUTE` | Recomendaciones IA por minuto | `5` |
| `LOGIN_RATE_LIMIT_PER_MINUTE` | Intentos de login por minuto | `5` |
| `JWT_SECRET` | Secreto JWT (≥32 bytes, Base64) | vacío (aleatorio en dev) |
| `JWT_EXPIRATION_MINUTES` | Expiración del token | `1440` |
| `JWT_ISSUER` | Emisor esperado | `driveai` |
| `ADMIN_EMAIL` / `ADMIN_PASSWORD` | Admin inicial | vacío |
| `CORS_ALLOWED_ORIGINS` | Orígenes permitidos | `http://localhost:5173,http://localhost:4173` |
| `SOAP_MOCK_PORT` | Puerto del mock SOAP | `8089` |
| `VITE_API_URL` | URL absoluta de la API | vacío |
| `FRONTEND_PORT` | Puerto del frontend | `5173` |

En `prod` las variables obligatorias usan `${VAR:?mensaje}` y el arranque falla con un
mensaje claro si faltan.

---

## 🔐 Seguridad

- Contraseñas con **BCrypt**.
- JWT con firma HMAC, validación de issuer, expiración y rechazo de `alg=none`.
- Propiedad de recursos derivada del JWT (BOLA/IDOR corregido en carrito y órdenes).
- Autorización de endpoints administrativos en el backend (`@PreAuthorize`), no solo en UI.
- DTOs de escritura sin campos sensibles (sin rol/estado/precio manipulables).
- Consultas JPA parametrizadas; validación de entrada con Bean Validation.
- Parser XML con DTD y entidades externas deshabilitadas (XXE).
- Rate limit de login → `429` + `Retry-After`.
- Cabeceras CSP, HSTS, `X-Content-Type-Options`, frame deny, referrer policy.
- CORS con allowlist y credenciales sin comodín.
- Errores sin stack traces ni datos internos.
- Escaneo de secretos (Gitleaks) y dependencias (Trivy / OWASP Dependency-Check) en CI.

Informe completo: `docs/testing/08-seguridad-y-resiliencia.md`.

> ⚠️ **Deuda histórica de seguridad (G6)**: el historial de Git contiene una contraseña
> de demo en un commit antiguo. No se reescribió el historial sin autorización. El
> procedimiento seguro está documentado en el informe de seguridad y debe ejecutarse
> antes del tag final.

---

## 🛠️ Troubleshooting

**El backend no levanta (variable faltante)** → completá `.env` según la sección de
inicio rápido. El mensaje indica exactamente qué variable falta.

**Flyway error** → la base no existe o no es accesible:
```bash
pg_isready -h localhost -p 5432
psql -h localhost -U cardb_user -d cardb -c 'select 1'
```

**La IA devuelve 503** → revisá `GROQ_API_KEY`; sin clave el asesor degrada de forma
controlada. Si el modelo fue deprecado, ajustá `GROQ_MODEL`.

**429 en login o IA** → superaste el rate limit; esperá los segundos de `Retry-After`.

**Eventos outbox en PENDING** → RabbitMQ no está disponible. Al levantarlo, el publisher
reintenta automáticamente y los publica (los fallos técnicos no descartan el evento; solo
un payload corrupto queda `FAILED` para reproceso manual).

**401 tras recargar** → el JWT caducó o cambió `JWT_SECRET`; volvé a iniciar sesión.

**Panel "Acceso restringido"** → la cuenta es `CUSTOMER`; ingresá con `ADMIN_EMAIL`.

---

## 🛣️ Notas de producción

- El rate limiter es in-memory por nodo. Para múltiples réplicas usar Redis manteniendo
  la interfaz.
- Los secretos viven en `.env` / secret manager, nunca en el repositorio.
- `ddl-auto=validate` evita desalineaciones entre Flyway y las entidades.
- El checkout usa `PESSIMISTIC_WRITE` por auto para prevenir sobreventa.
- El outbox garantiza publicación confiable; el consumidor deduplica por `messageId`.
- TLS debe terminar en el reverse proxy / balanceador (no se incluye certificado en el
  repositorio).

---

## 📚 Entregables de QA

Ver `docs/testing/`:

1. `01-plan-de-pruebas.md` — plan, alcance, riesgos y criterios de salida
2. `02-casos-de-prueba.md` — matriz de 78 casos (69 % negativos/borde) y trazabilidad
3. `03-pruebas-unitarias-y-cobertura.md` — 148 tests, JaCoCo 93,83 % líneas / 79,18 % ramas
4. `04-pruebas-de-integracion.md` — Testcontainers (PostgreSQL + RabbitMQ)
5. `05-pruebas-mutacion.md` — PIT 73,5 % (161/219)
6. `06-pruebas-e2e.md` — Playwright 16/16 (6 specs)
7. `07-pruebas-api-contrato-y-carga.md` — API, OpenAPI/WSDL y k6
8. `08-seguridad-y-resiliencia.md` — JWT, BOLA/BFLA, cabeceras, ZAP/Trivy
9. `09-declaracion-ia.md` — declaración de uso de IA

Además: `docs/manual-usuario.md`, `docs/historias-gherkin.md` y la memoria técnica
`docs/memoria-tecnica-parcial-1.md`.

> El PDF final (`Clase15_Grupo<N>_<Dominio>_Testing.pdf`) no se generó en este entorno;
> queda pendiente para la entrega formal. Los documentos Markdown de `docs/testing/`
> son la fuente de contenido.
