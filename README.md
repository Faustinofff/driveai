# 🚗 DriveAI — E-commerce de autos con asesor de compra por IA

Plataforma completa de venta de autos usados a nivel producción:

- **Backend**: Spring Boot 3.3 (Java 17) + Spring Data JPA + PostgreSQL 16 + Flyway + Bucket4j + SpringDoc.
- **Frontend**: React 18 + Vite + TypeScript + Tailwind CSS + TanStack Query + Zustand.
- **IA**: recomendador inteligente con Groq Cloud (`llama-3.3-70b-versatile`) con **rate limiting**, **anti-alucinación** de IDs y auditoría de prompts.

---

## ✨ Características

- Catálogo paginado y **filtrable** (marca, rango de precio, año mín., combustible, carrocería, búsqueda libre).
- Carrito anónimo por `sessionId` con `priceSnapshot` congelado al añadir.
- Checkout **transaccional** con bloqueo pesimista (`PESSIMISTIC_WRITE`), validación de stock e inventario descontado.
- Asesor de compra IA en lenguaje natural que solo recomienda autos **reales en stock** (filtra IDs alucinados).
- Rate limit local con **Bucket4j** (5 req/min por `sessionId|IP`) → `429` con `Retry-After`.
- Errores uniformes **RFC 7807** (`application/problem+json`).
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
                           │  Car / Cart / Order / AI      │
                           │  Bucket4j · Flyway · JPA      │
                           └──────────────┬──────────────┘
                                          │ JDBC
                                          ▼
                           ┌──────────────────────────────┐
                           │   PostgreSQL 16  (cardb)      │
                           │   cars, carts, orders, logs   │
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
│       │   ├── advice/             # @ControllerAdvice RFC 7807
│       │   ├── ai/                 # GroqApiClient + HttpGroqApiClient
│       │   ├── config/             # RestClient, OpenAPI, GroqApiProperties
│       │   ├── controller/         # Car / Cart / Order / AiAdvisor
│       │   ├── domain/             # enums + entidades JPA
│       │   ├── dto/                # records Request/Response
│       │   ├── exception/          # excepciones de negocio
│       │   ├── rate/               # RateLimitService (Bucket4j)
│       │   ├── repository/         # repositorios Spring Data
│       │   ├── service/            # Car / Cart / Order / GroqAdvisor
│       │   └── specification/      # CarSpecification + CarFilters
│       ├── main/resources/
│       │   ├── application.yml     # perfiles dev / prod
│       │   └── db/migration/       # V1__init_schema.sql, V2__seed_cars.sql
│       └── test/java/...           # JUnit 5 + Mockito
├── frontend/                       # React + Vite + TypeScript
│   ├── package.json
│   ├── Dockerfile
│   ├── nginx.conf
│   └── src/
│       ├── components/             # Navbar, CarCard, Filters, CartDrawer, AiChat…
│       ├── hooks/                  # useCars, useDebounce
│       ├── pages/                  # CatalogPage, AiConciergePage
│       ├── services/api.ts         # cliente Axios tipado
│       ├── store/cartStore.ts      # Zustand + persist (localStorage)
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

---

## 🔌 Endpoints principales

### Catálogo — `/api/cars`
| Método | Ruta            | Descripción |
| ------ | --------------- | ----------- |
| GET    | `/api/cars`     | Paginado + filtros: `brand`, `minPrice`, `maxPrice`, `minYear`, `fuelType`, `bodyType`, `q` |
| GET    | `/api/cars/{id}` | Detalle (404 RFC 7807 si no existe) |

### Carrito — `/api/cart` (anónimo por `sessionId`)
| Método   | Ruta                       | Descripción |
| -------- | -------------------------- | ----------- |
| GET      | `/api/cart?sessionId=…`    | Contenido del carrito |
| POST     | `/api/cart/add`            | `{ sessionId, carId, quantity }` |
| PUT      | `/api/cart/update/{itemId}` | `{ quantity }` |
| DELETE   | `/api/cart/remove/{itemId}` | Elimina la línea |

### Órdenes — `/api/orders`
| Método | Ruta              | Descripción |
| ------ | ----------------- | ----------- |
| POST   | `/api/orders/checkout` | `{ sessionId, items:[{carId,quantity}] }` → valida stock (lock), descuenta inventario, orden `CONFIRMED` |

### Asesor IA — `/api/ai`
| Método | Ruta                | Descripción |
| ------ | ------------------- | ----------- |
| POST   | `/api/ai/recommend` | `{ prompt, sessionId }` → `{ sessionId, reasoning, recommendations[] }` |

Respuestas de error uniformes **RFC 7807** (`application/problem+json`):

| Código | Caso                                   |
| ------ | -------------------------------------- |
| 400    | Validación de campos / JSON malformado |
| 404    | Recurso no encontrado                  |
| 409    | Stock insuficiente                     |
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
GROQ_MODEL=llama-3.3-70b-versatile
GROQ_API_BASE_URL=https://api.groq.com/openai/v1
RATE_LIMIT_PER_MINUTE=5
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
| `GROQ_MODEL`               | Modelo LLM de Groq                                 | `llama-3.3-70b-versatile`       |
| `GROQ_API_BASE_URL`        | URL base compatible OpenAI                         | `https://api.groq.com/openai/v1` |
| `SPRING_PROFILES_ACTIVE`   | Perfil Spring (`dev` / `prod`)                     | `dev`                           |
| `SERVER_PORT`              | Puerto del backend                                 | `8080`                          |
| `RATE_LIMIT_PER_MINUTE`    | Máx. recomendaciones IA por minuto por usuario     | `5`                             |
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
console.groq.com. Con clave ausente el mensaje lo indica explícitamente.

**`429` en `/api/ai/recommend`** → estás superando `RATE_LIMIT_PER_MINUTE`; espera los segundos indicados (`Retry-After`).

**El carrito "desaparece" al recargar** → es normal: se persiste en `localStorage` bajo la clave `driveai-cart`; usa siempre la misma sesión/navegador.

**Puerto 5173 ocupado** → cambia `FRONTEND_PORT` en `.env`; en dev usa `npm run dev -- --port 5174`.

---

## 🛣️ Notas de producción

- El rate limiter es **memo-in-memory** (Bucket4j, local al nodo). Para múltiples réplicas usa una implementación distribuida (Redis) manteniendo la misma interfaz `RateLimitService`.
- Las contraseñas de BD y `GROQ_API_KEY` **nunca** se versionan; siempre via `.env`/secret manager.
- `ddl-auto=validate` evita desalineaciones entre el esquema Flyway y las entidades.
- El checkout usa `PESSIMISTIC_WRITE` por auto: previene sobreventa bajo concurrencia.