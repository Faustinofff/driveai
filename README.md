# 🚗 DriveAI — E-commerce de autos con asesor de compra por IA

Plataforma completa de venta de autos usados a nivel producción:

- **Backend**: Spring Boot 3.3 (Java 17) + Spring Security (JWT) + Spring Data JPA + PostgreSQL 16 + Flyway + Bucket4j + SpringDoc.
- **Frontend**: React 18 + Vite + TypeScript + Tailwind CSS + TanStack Query + Zustand.
- **IA**: recomendador inteligente con Groq Cloud (`groq/compound-mini`) con **rate limiting**, **anti-alucinación** de IDs y auditoría de prompts.

---

## ✨ Características

- Catálogo paginado y **filtrable** (marca, rango de precio, año mín., combustible, carrocería, búsqueda libre). **Público**, sin login.
- **Cuentas y roles con JWT**: `CUSTOMER` (carrito, compra, IA) y `ADMIN` (publica/edita/elimina autos desde el panel).
- Carrito por `sessionId` con `priceSnapshot` congelado al añadir (requiere sesión iniciada).
- Checkout **transaccional** con bloqueo pesimista (`PESSIMISTIC_WRITE`), validación de stock e inventario descontado.
- **Panel de administración** en el frontend: alta/edición/borrado de autos con validación.
- Asesor de compra IA en lenguaje natural que solo recomienda autos **reales en stock** (filtra IDs alucinados).
- Rate limit local con **Bucket4j** (5 req/min por `sessionId|IP`) → `429` con `Retry-After`.
- Errores uniformes **RFC 7807** (`application/problem+json`), incluidos `401`/`403`.
- Migraciones versionadas **Flyway** + validación de esquema (`ddl-auto=validate`).
- Despliegue unificado con **Docker Compose** y **NINGÚN secret hardcodeado**.

---

## 🧱 Arquitectura

```
┌──────────────┐        ┌──────────────────────────────┐        ┌─────────────────┐
│   Browser    │  HTTP  │   Frontend (React + Vite)    │        │   Groq Cloud    │
│  React SPA   │ ─────▶ │  Nginx (proxy /api)          │ ─────▶ │  /chat/completions
└──────────────┘        └──────────────┬──────────────┘        └─────────────────┘
                                       │ JSON (RFC 7807)
                                       ▼
                           ┌──────────────────────────────┐
│  Backend Spring Boot 3.3      │
                            │  JWT · Car / Cart / Order / AI │
                            │  Bucket4j · Flyway · JPA      │
                           └──────────────┬──────────────┘
                                          │ JDBC
                                          ▼
                           ┌──────────────────────────────┐
                           │   PostgreSQL 16  (cardb)      │
                           │   cars, app_users, carts,    │
                            │   orders, logs                 │
                           └──────────────────────────────┘
```

---

## 📁 Estructura del repositorio

```
.
├── backend/                        # Spring Boot 3 + Maven
│   ├── pom.xml
│   ├── Dockerfile
│   └── src/
│       ├── main/java/com/driveai/
│       │   ├── advice/             # @ControllerAdvice RFC 7807 (400/401/403/404/409/429/503)
│       │   ├── ai/                 # GroqApiClient + HttpGroqApiClient
│       │   ├── config/             # RestClient, OpenAPI, GroqApiProperties, AdminInitializer
│       │   ├── controller/         # Car / Cart / Order / AiAdvisor / Auth
│       │   ├── domain/             # enums + entidades JPA (incl. AppUser)
│       │   ├── dto/                # records Request/Response (incl. Auth)*
│       │   ├── exception/          # excepciones de negocio (ConflictException)
│       │   ├── rate/               # RateLimitService (Bucket4j)
│       │   ├── repository/         # repositorios Spring Data
│       │   ├── security/           # JwtService, JwtAuthenticationFilter, SecurityConfig
│       │   ├── service/            # Car / Cart / Order / GroqAdvisor / Auth
│       │   └── specification/      # CarSpecification + CarFilters
│       ├── main/resources/
│       │   ├── application.yml     # perfiles dev / prod (+ app.jwt / app.admin)
│       │   └── db/migration/       # V1__init_schema, V2__seed_cars, V3__auth_users, V4__fix_cars_sequence
│       └── test/java/...           # JUnit 5 + Mockito
├── frontend/                       # React + Vite + TypeScript
│   ├── package.json
│   ├── Dockerfile
│   ├── nginx.conf
│   └── src/
│       ├── components/             # Navbar, CarCard, Filters, CartDrawer, AiChat…
│       ├── hooks/                  # useCars, useDebounce
│       ├── pages/                  # CatalogPage, AiConciergePage, LoginPage, AdminPage
│       ├── services/api.ts         # cliente Axios tipado + interceptor JWT Bearer + 401
│       ├── store/                  # cartStore + authStore (Zustand + persist)
│       └── types/                  # interfaces TS estrictas
├── .env.example                    # plantilla de variables (0 secrets)
├── docker-compose.yml              # postgres + backend + frontend
└── api-tests.http                  # colección REST Client / IntelliJ
```

---

## 🔑 Obtener tu API Key gratis en Groq

1. Ve a <https://console.groq.com/keys> y crea una cuenta gratuita.
2. Haz clic en **Create API Key**, dale un nombre (ej. `driveai-dev`) y copia la clave con formato `gsk_…`.
3. No la compartas ni la subas al repositorio: se inyecta solo como variable de entorno.

```bash
# Copia la plantilla y edita con tu clave
cp .env.example .env
```

Edita el archivo `.env` y pega tu clave:

```dotenv
GROQ_API_KEY=gsk_xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
```

---

## ⚙️ Ejecución local sin Docker (stack nativo — usado en esta máquina)

> Docker Desktop no arranca en este equipo porque la **virtualización está desactivada en la BIOS**
> (`HyperVisorPresent: False`, `HyperVRequirementVirtualizationFirmwareEnabled: False`).
> Mientras tanto el proyecto corre con un stack nativo portable en `C:\driveai\.tools`
> (JDK 17 + Maven 3.9 + PostgreSQL 16), sin Docker ni WSL.

Arranque (o reinicio) de los tres servicios en el orden correcto:

```bat
:: 1) PostgreSQL portátil (puerto 5433)
C:\driveai\.tools\start-db.bat

:: 2) Backend Spring Boot (puerto 8080) — compila + arranca en ~30 s
C:\driveai\.tools\start-backend.bat

:: 3) Frontend Vite (puerto 5173)
C:\driveai\.tools\start-frontend.bat
```

Servicios y puertos:

| Servicio     | URL                             | Puerto | Logs                         |
|--------------|---------------------------------|--------|------------------------------|
| Frontend     | `http://localhost:5173`         | 5173   | `.tools/frontend.out.log`    |
| Backend API  | `http://localhost:8080`         | 8080   | `.tools/backend.out.log`     |
| Swagger UI   | `http://localhost:8080/swagger-ui.html` | 8080 | —                     |
| PostgreSQL   | `jdbc:postgresql://localhost:5433/cardb` | 5433 | `.tools/pgdata/log/` |

Notas:

- El puerto **5433** se usa a propósito: hay un servicio `postgresql-x64-18` del sistema ocupando el 5432.
- El backend arranca con `mvn spring-boot:run`; `SPRING_DATASOURCE_URL/USERNAME/PASSWORD` se
  inyectan por variables de entorno desde `start-backend.bat` (por eso `application.yml` mantiene
  `localhost:5432` y aún así funciona).
- `start-backend.bat` además inyecta `JWT_SECRET`, `GROQ_API_KEY`, `GROQ_MODEL` y las credenciales
  `ADMIN_EMAIL`/`ADMIN_PASSWORD` del administrador inicial (se crea solo al primer arranque).
- Credenciales demo locales: `admin@driveai.com` / `REDACTED` (definidas en `.env`, no versionado).
- Para **habilitar Docker** en el futuro: activa la virtualización en la BIOS (Intel VT-x / AMD SVM),
  ejecuta `wsl --install` y reinicia; luego `docker compose up -d --build`.
- Verificación rápida de salud: `curl http://localhost:8080/actuator/health` → `{"status":"UP"}`.

---

## 🐳 Inicio rápido con Docker Compose

Requisitos: Docker + Docker Compose v2.

```bash
# 1) Copiar y completar variables (obligatorio: GROQ_API_KEY)
cp .env.example .env

# 2) Construir y levantar todo (postgres → backend → frontend)
docker compose up -d --build

# 3) Verificar
docker compose ps
docker compose logs -f backend

# Servicios
#   Frontend    → http://localhost:5173
#   Backend API → http://localhost:8080
#   Swagger UI  → http://localhost:8080/swagger-ui.html
```

Detener / limpiar:

```bash
docker compose down          # detener
docker compose down -v       # detener y borrar el volumen de la BD (reset total)
```

> El backend espera al healthcheck de PostgreSQL y aplica las migraciones Flyway automáticamente.

---

## 💻 Despliegue local manual

### 1) Base de datos (PostgreSQL 16)

Opcional A — con Docker:

```bash
docker compose up -d postgres

# Verificar conectividad en el puerto 5432
pg_isready -h localhost -p 5432
```

Opcional B — instalación nativa (o dentro del contenedor):

```bash
# Crear usuario y base desde un cliente psql (comando único desde el host)
psql -U postgres -h localhost <<'SQL'
CREATE USER cardb_user WITH PASSWORD 'cardb_pass';
CREATE DATABASE cardb OWNER cardb_user;
SQL
```

Verificación:

```bash
psql -h localhost -U cardb_user -d cardb -c '\dt'
#   public | ai_recommendation_logs
#   public | car_items        -> cart_items
#   public | cars
#   public | carts
#   public | order_items
#   public | orders
```

El esquema y los 25 autos de ejemplo se crean/insertan solos vía Flyway
(`backend/src/main/resources/db/migration`), en el primer arranque del backend.

### 2) Backend (Java 17 + Maven)

```bash
cd backend
# perfil dev usa localhost:5432/cardb con cardb_user/cardb_pass (ajustable en application.yml)
export GROQ_API_KEY=gsk_...     # PowerShell: $env:GROQ_API_KEY="gsk_..."
mvn spring-boot:run
#   → API en http://localhost:8080
#   → Swagger en /swagger-ui.html
```

### 3) Frontend (Node 20+)

```bash
cd frontend
npm install
npm run dev
#   → http://localhost:5173 (Vite proxea /api → http://localhost:8080)
```

---

## 🧪 Tests

```bash
cd backend
mvn test
```

Cobertura de pruebas unitarias (JUnit 5 + Mockito):

| Clase                                  | Qué prueba                                            |
| -------------------------------------- | ----------------------------------------------------- |
| `CarSpecificationTest`                 | Predicados dinámicos (marca/ precio / año / fuel / búsqueda) |
| `OrderServiceTest`                     | Checkout: descuento de stock, merge de líneas, 404 y stock insuficiente |
| `GroqAdvisorServiceTest`               | **Anti-alucinación** (filtra IDs inventados / sin stock), JSON corrupto, saturación, logs |
| `RateLimitServiceTest`                 | Bucket4j: bloqueo tras límite y aislamiento de llaves |
| `AuthServiceTest`                      | Registro/login, pwd incorrecta → 401, `ensureAdminAccount` idempotente |
| `CarServiceTest`                       | CRUD admin: alta/edición, 404, borrado y conflicto cuando el auto tiene ventas |

**26 tests** en total: `mvn test` → `BUILD SUCCESS`.

---

## 🔌 Endpoints principales

### Autenticación — `/api/auth` (público)
| Método | Ruta                  | Descripción |
| ------ | --------------------- | ----------- |
| POST   | `/api/auth/register`  | `{ email, password(8-72), fullName }` → crea `CUSTOMER`, devuelve `{ token, user }` (201) |
| POST   | `/api/auth/login`     | `{ email, password }` → `{ token, user }`. Errores: 401 RFC 7807 |

El `token` es un **JWT HS-384** con claims `uid`/`role` (subject = email) y se envía como
`Authorization: Bearer <token>` (el interceptor de Axios lo agrega solo). Caduca a las
`JWT_EXPIRATION_MINUTES` (default 1440).

### Catálogo — `/api/cars` (**público**)
| Método | Ruta            | Descripción |
| ------ | --------------- | ----------- |
| GET    | `/api/cars`     | Paginado + filtros: `brand`, `minPrice`, `maxPrice`, `minYear`, `fuelType`, `bodyType`, `q` |
| GET    | `/api/cars/{id}` | Detalle (404 RFC 7807 si no existe) |

### Catálogo — gestión (**solo `ADMIN`**)
| Método | Ruta            | Descripción |
| ------ | --------------- | ----------- |
| POST   | `/api/cars`     | Alta de auto (validación en `CarWriteRequest`); 201 con el auto creado |
| PUT    | `/api/cars/{id}` | Edición completa del auto; 404 si no existe |
| DELETE | `/api/cars/{id}` | Borra el auto; **409** si ya tiene ventas (integridad referencial) |

> Cualquier llamada a estos tres sin token de rol `ADMIN` → `401` (sin token) o `403` (rol insuficiente).

### Carrito — `/api/cart` (requiere **Bearer**)
| Método   | Ruta                       | Descripción |
| -------- | -------------------------- | ----------- |
| GET      | `/api/cart?sessionId=…`    | Contenido del carrito |
| POST     | `/api/cart/add`            | `{ sessionId, carId, quantity }` |
| PUT      | `/api/cart/update/{itemId}` | `{ quantity }` |
| DELETE   | `/api/cart/remove/{itemId}` | Elimina la línea |

### Órdenes — `/api/orders` (requiere **Bearer**)
| Método | Ruta              | Descripción |
| ------ | ----------------- | ----------- |
| POST   | `/api/orders/checkout` | `{ sessionId, items:[{carId,quantity}] }` → valida stock (lock), descuenta inventario, orden `CONFIRMED` |

### Asesor IA — `/api/ai` (requiere **Bearer**)
| Método | Ruta                | Descripción |
| ------ | ------------------- | ----------- |
| POST   | `/api/ai/recommend` | `{ prompt, sessionId }` → `{ sessionId, reasoning, recommendations[] }` |

Respuestas de error uniformes **RFC 7807** (`application/problem+json`):

| Código | Caso                                   |
| ------ | -------------------------------------- |
| 400    | Validación de campos / JSON malformado |
| 401    | Token ausente, inválido o expirado     |
| 403    | Autenticado pero sin rol `ADMIN`       |
| 404    | Recurso no encontrado                  |
| 409    | Stock insuficiente / auto con ventas   |
| 429    | Rate limit excedido (+ `Retry-After`)  |
| 503    | Servicio de IA saturado/no disponible  |

La colección completa está en **`api-tests.http`** (extensión REST Client de VS Code o IntelliJ).

---

## 🧠 Cómo funciona el asesor IA (Groq)

1. **Rate limiting**: Bucket4j con `app.rate-limit.requests-per-minute` (default 5).
2. **Prompt de sistema**: se inyecta un **JSON compacto** de los autos con stock (`id, brand, model, year, price, fuelType, fuelConsumption, bodyType`) para minimizar tokens.
3. Se pide a Groq `response_format: {"type":"json_object"}` y salida estricta:
   `{"reasoning": "...", "recommendedCarIds": [1,2,3]}`.
4. **Anti-alucinación**: los IDs devueltos se contrastan contra `cars` (`stock > 0`). Si el modelo inventa IDs (ej. `9999`), se **filtran silenciosamente**.
5. **Fallback**: 429 de Groq, timeout o JSON corrupto → `503` con mensaje amigable (el frontend muestra un aviso con botón de reintentar). Nunca un 500 crudo.
6. **Auditoría**: cada consulta se persiste en `ai_recommendation_logs` (prompt, respuesta, IDs recomendados, estado, error).

Configuración (ver `application.yml` y `.env.example`):

```dotenv
GROQ_API_KEY=…                        # clave de console.groq.com
GROQ_MODEL=groq/compound-mini
GROQ_API_BASE_URL=https://api.groq.com/openai/v1
RATE_LIMIT_PER_MINUTE=5
JWT_SECRET=…                          # ≥32 bytes en Base64; fija entre reinicios
ADMIN_EMAIL=admin@driveai.com         # cuenta admin inicial (auto-creada)
ADMIN_PASSWORD=…                      # password del admin inicial
```

---

## ⚙️ Variables de entorno

| Variable                   | Descripción                                        | Default                         |
| -------------------------- | -------------------------------------------------- | ------------------------------- |
| `POSTGRES_DB`              | Nombre de la base                                  | `cardb`                         |
| `POSTGRES_USER`            | Usuario de la base                                 | `cardb_user`                    |
| `POSTGRES_PASSWORD`        | Contraseña de la base                              | `cardb_pass` (¡cámbiala en prod!) |
| `POSTGRES_PORT`            | Puerto expuesto de PostgreSQL                      | `5432`                          |
| `GROQ_API_KEY`             | API Key de Groq Cloud (obligatoria para la IA)     | *(vacía)*                       |
| `GROQ_MODEL`               | Modelo LLM de Groq                                 | `groq/compound-mini`           |
| `GROQ_API_BASE_URL`        | URL base compatible OpenAI                         | `https://api.groq.com/openai/v1` |
| `SPRING_PROFILES_ACTIVE`   | Perfil Spring (`dev` / `prod`)                     | `dev`                           |
| `SERVER_PORT`              | Puerto del backend                                 | `8080`                          |
| `RATE_LIMIT_PER_MINUTE`    | Máx. recomendaciones IA por minuto por usuario     | `5`                             |
| `JWT_SECRET`               | Secreto JWT (≥32 bytes, Base64)                    | *(vacío → aleatorio)*           |
| `JWT_EXPIRATION_MINUTES`   | Expiración del token JWT (minutos)                 | `1440`                          |
| `ADMIN_EMAIL`              | Email del admin creado en el primer arranque       | *(vacío → no se crea)*          |
| `ADMIN_PASSWORD`           | Password del admin inicial                         | *(vacío → no se crea)*          |
| `VITE_API_URL`             | URL absoluta de la API (vacío = mismo origen/proxy) | vacío                          |
| `FRONTEND_PORT`            | Puerto del frontend en Docker                      | `5173`                          |

Los perfiles de `application.yml` resuelven `SPRING_DATASOURCE_URL/USERNAME/PASSWORD`: en `dev` apuntan a `localhost:5432/cardb`; en `prod` se inyectan vía entorno (Docker Compose).

---

## 🛠️ Troubleshooting

**El backend no levanta (Flyway error)** → la base no existe o no es accesible:
```bash
pg_isready -h localhost -p 5432
psql -h localhost -U cardb_user -d cardb -c 'select 1'
```

**La IA devuelve `503`** → revisa `GROQ_API_KEY` en `.env` y el límite de la cuenta en
console.groq.com. Con clave ausente el mensaje lo indica explícitamente. Si da `404 model_not_found`,
el modelo `GROQ_MODEL` fue deprecado (usa `groq/compound-mini`).

**`429` en `/api/ai/recommend`** → estás superando `RATE_LIMIT_PER_MINUTE`; espera los segundos indicados (`Retry-After`).

**El carrito "desaparece" al recargar** → es normal: se persiste en `localStorage` bajo la clave `driveai-cart`; usa siempre la misma sesión/navegador.

**`401` en carrito/IA/checkout tras recargar** → la sesión JWT caducó (`JWT_EXPIRATION_MINUTES`) o el
backend se reinició con `JWT_SECRET` vacío (tokens aleatorios). Configura `JWT_SECRET` fijo en `.env`.

**El panel muestra "Acceso restringido"** → la cuenta con la que entraste es `CUSTOMER`; entra con
`admin@driveai.com` (o la cuenta definida en `ADMIN_EMAIL`).

**Puerto 5173 ocupado** → cambia `FRONTEND_PORT` en `.env`; en dev usa `npm run dev -- --port 5174`.

---

## 🛣️ Notas de producción

- El rate limiter es **memo-in-memory** (Bucket4j, local al nodo). Para múltiples réplicas usa una implementación distribuida (Redis) manteniendo la misma interfaz `RateLimitService`.
- Las contraseñas de BD, `GROQ_API_KEY` y `JWT_SECRET` **nunca** se versionan; siempre via `.env`/secret manager. La cuenta admin inicial se crea con `ADMIN_EMAIL`/`ADMIN_PASSWORD` del entorno (nunca en migraciones).
- `ddl-auto=validate` evita desalineaciones entre el esquema Flyway y las entidades.
- El checkout usa `PESSIMISTIC_WRITE` por auto: previene sobreventa bajo concurrencia.