# Memoria Técnica — DriveAI

**Entrega Obligatoria N.º 1 — 1.º Parcial**

- **Fecha límite:** 14/09
- **Producto:** DriveAI — e-commerce de autos certificados con asesor de compra basado en IA
- **Sistema operativo / entorno de despliegue:** Windows, stack nativo portable (`C:\driveai\.tools`): JDK 17, Maven 3.9, PostgreSQL 16.6 en puerto 5433
- **Repositorio:** https://github.com/Faustinofff/driveai

---

## 1. Resumen ejecutivo

DriveAI es un e-commerce de vehículos certificados que integra un catálogo consultable y
administrable con un asesor de compra basado en inteligencia artificial. El objetivo del producto
es que un usuario encuentre, compare y compre un auto mediante una experiencia guiada, y que un
administrador pueda publicar, editar y eliminar unidades en tiempo real.

El sistema está compuesto por **cuatro componentes de negocio** completamente implementados, cada
uno con arquitectura en capas independiente que se comunica a través de una API REST segura:

1. **Catálogo de autos** (consulta, filtros, paginación y administración CRUD).
2. **Autenticación y autorización** con JWT y roles.
3. **Carrito y checkout** (carrito anónimo persistente y órdenes de compra).
4. **Asesor IA** (recomendaciones de vehículos con lenguaje natural).

El presente documento describe la arquitectura, los componentes, los patrones de diseño aplicados,
la seguridad declarativa y la justificación de cada decisión.

---

## 2. Arquitectura general (en capas)

El sistema sigue una arquitectura **cliente-servidor de tres niveles** (presentación — aplicación —
datos), con separación estricta de responsabilidades en cada plano.

```
┌────────────────────────────────────────────────────────────────────────────┐
│  CAPA DE PRESENTACIÓN  — React 18 + Vite + Tailwind (frontend :5173)       │
│                                                                              │
│  pages/   →  componentes de página (Home, Catalog, Login, Admin, Ai)        │
│  components/ → UI reutilizable (CarCard, Navbar, CartDrawer, CheckoutModal) │
│  services/api.ts → cliente HTTP (axios) con interceptor de JWT              │
│  store/ (zustand) → estado global de sesión, carrito, tema                  │
└──────────────┬───────────────────────────────────────────────┬──────────────┘
               │  REST/JSON  (baseURL /api)                     │
               ▼                                               │
┌───────────────────────────────────────────────────────────────┴────────────┐
│  CAPA DE APLICACIÓN — Spring Boot 3.3 (backend :8080)                       │
│                                                                              │
│  controllers/  →  HTTP: validación, DTOs, códigos de estado                  │
│  services/     →  lógica de negocio y orquestación (facades)                 │
│  security/     →  SecurityFilterChain declarativa + JWT                      │
│  rate/         →  rate limiting con Bucket4j                                │
└──────────────┬───────────────────────────────────────────────┬──────────────┘
               │  Spring Data JPA                               │
               ▼                                               │
┌───────────────────────────────────────────────────────────────┴────────────┐
│  CAPA DE DATOS — repositories/ (DAO) + PostgreSQL 16 (:5433, base “cardb”)  │
│                                                                              │
│  cars · app_users · carts · cart_items · orders · order_items               │
│  ai_recommendation_logs · flyway_schema_history                              │
└──────────────────────────────────────────────────────────────────────────────┘
```

**Regla estructural fundamental:** el `Controller` nunca accede a datos; el `Service` nunca parsea
HTTP; el `Repository` es la única capa que habla con la base de datos. Esto se cumple en los cuatro
componentes y se verifica con la demo (sección 11).

---

## 3. Stack tecnológico

| Plano | Tecnología | Justificación |
|---|---|---|
| Presentación | React 18, TypeScript, Vite 5, Tailwind 3, zustand 4, TanStack Query 5, axios | SPA rápida con hot reload; TypeScript tipa los contratos DTO; query cache para el catálogo |
| Aplicación | Spring Boot 3.3.5, Java 17, Spring Security 6, JPA/Hibernate, Bean Validation, SpringDoc, Bucket4j | Framework dominante en el mercado laboral local; starter convención-sobre-configuración |
| Datos | PostgreSQL 16.6 + Flyway | SQL transaccional real; Flyway versiona el esquema (`ddl-auto: validate`) |
| IA | Groq Cloud (HTTP) | API OpenAI-compatible, rápida y con cuota gratuita |
| Infra | Docker Compose (opcional) y stack por bat (local) | la máquina de desarrollo no habilita virtualización; se usa Postgres portátil en :5433 |

---

## 4. Componente 1 — Catálogo de autos

### 4.1 Funcionalidad
Consulta paginada y filtrable del inventario (marca, precio mínimo/máximo, año mínimo,
combustible, carrocería, búsqueda por texto), detalle por id, y administración CRUD
(publicar / editar / eliminar) **restringida a rol ADMIN**.

### 4.2 Capas
- **Presentación:** `CatalogPage` (filtros + paginación), `CarCard`, `AdminPage` con `CarFormModal`.
- **Aplicación:** `CarController` → `CarService` → `CarSpecification` (filtros dinámicos).
- **Datos:** `CarRepository extends JpaRepository<Car, Long>` + tabla `cars`.

### 4.3 Contrato REST
```
GET    /api/cars?page=0&size=12&brand=&minPrice=&bodyType=&q=        → PageResponse<CarResponse>
GET    /api/cars/{id}
POST   /api/cars                  (ADMIN)  → 201
PUT    /api/cars/{id}             (ADMIN)
DELETE /api/cars/{id}             (ADMIN)  → 204 (409 si el auto figura en órdenes)
```

### 4.4 Flujo de una consulta (ejemplo real)
El usuario filtra "SUV económico" en `CatalogPage` → `services/api.getCars()` construye la query
string → `GET /api/cars` → `CarController.list` arma el filtro → `CarService` delega en
`CarRepository.findAll(specification, pageable)` → el motor SQL de Postgres ejecuta la query →
`PageResponse.from(page)` adapta el resultado a DTO → se renderiza la rejilla con React Query.

---

## 5. Componente 2 — Autenticación y autorización por roles

### 5.1 Funcionalidad
Registro de clientes (rol `CUSTOMER`), login con emisión de JWT, y autorización por rol sobre
operaciones administrativas.

### 5.2 Capas
- **Presentación:** `LoginPage` (login/registro), `authStore` (zustand) persiste el token en `localStorage`.
- **Aplicación:** `AuthController` → `AuthService` → `JwtService`; `SecurityConfig` (SecurityFilterChain).
- **Datos:** `AppUserRepository`, tabla `app_users` (hash BCrypt, nunca la contraseña).

### 5.3 Contrato REST
```
POST /api/auth/register   → 201  { token, user { id, email, fullName, role } }
POST /api/auth/login      → 200  { token, user }
```

### 5.4 Decisiones
- **JWT stateless + `SessionCreationPolicy.STATELESS`:** la API no guarda sesión en servidor; escalable horizontalmente.
- **Token firmado con secreto de aplicación** (`JWT_SECRET`, expiración 1440 min) con claims `sub` (email), `uid` y `role`.
- **admin inicial auto-creado** en el arranque por `AdminInitializer` leyendo `ADMIN_EMAIL`/`ADMIN_PASSWORD` del entorno (se crea solo la primera vez).

---

## 6. Componente 3 — Carrito y checkout

### 6.1 Funcionalidad
Carrito anónimo identificado por `sessionId` (generado por el cliente y enviado en cada request),
con alta/modificación/baja de ítems, y checkout que convierte el carrito en una **orden persistente**
con ítems y total.

### 6.2 Capas
- **Presentación:** `CartDrawer`, `CheckoutModal`, `cartStore`.
- **Aplicación:** `CartController` (get/add/update/remove) y `OrderController` (`POST /checkout`).
- **Datos:** tablas `carts`, `cart_items`, `orders`, `order_items` + repositorios dedicados.

### 6.3 Contrato REST
```
GET    /api/cart?sessionId=...            → CartResponse (subtotal)
POST   /api/cart/add                      { sessionId, carId, quantity }
PUT    /api/cart/update/{itemId}          { quantity }
DELETE /api/cart/remove/{itemId}          ?sessionId=...
POST   /api/orders/checkout               { sessionId, items[] } → Order (PENDING)
```

### 6.4 Decisiones
- El carrito **no depende de cookies de sesión** (la API es stateless): el `sessionId` viaja en el
  cuerpo, lo que la hace funcional desde cualquier cliente HTTP (Postman, app móvil futura).
- El **precio se congela al agregar** (`priceSnapshot`): si el admin actualiza el precio después, la
  línea del carrito mantiene el valor ofrecido al cliente.
- El checkout valida stock y reconstruye la orden desde `cars` para evitar manipulación del total.

---

## 7. Componente 4 — Asesor IA

### 7.1 Funcionalidad
Chat asesor que recibe una consulta en lenguaje natural, la contrasta contra el inventario **real**
(`stock > 0`), recomienda hasta 5 vehículos y persiste el log de la conversación.

### 7.2 Capas
- **Presentación:** `AiConciergePage` y `AiChat`.
- **Aplicación:** `AiAdvisorController` → `GroqAdvisorService` (orquestador) → abstracción `GroqApiClient`.
- **Datos:** `CarRepository` (inventario) y `AiRecommendationLogRepository` (historial).

### 7.3 Contrato REST
```
POST /api/ai/recommend  { prompt, sessionId }  → { recommendation, reasoning, cars[] }
```
Respuestas: `200` OK, `429` límite por minuto excedido (Bucket4j), `503` servicio de IA no disponible.

### 7.4 Decisiones clave
- **Sistema doble prompt:** inyecta un catálogo JSON compacto real en el system prompt.
- **Anti-alucinación:** los ids devueltos por el modelo se **filtran contra la base**; un id
  inventado nunca se muestra.
- **Rate limiting local** (`RateLimitService` + Bucket4j) por `sessionId|IP`, evita abusos con una
  cuenta gratuita.
- **Log de cada recomendación** para auditoría y trazabilidad.

---

## 8. Componentes stateful y stateless

### 8.1 Concepto
Un componente **stateless** no mantiene estado entre invocaciones; el contenedor puede crear
múltiples instancias con tráfico seguro. Un componente **stateful** conserva datos mutables entre
invocaciones, por lo que el ciclo de vida (creación y destrucción) lo gestiona el contenedor.

### 8.2 Evidencia en el backend
- **Stateless (singletons):** `CarService`, `AuthService`, `CartService`, `OrderService`,
  `GroqAdvisorService`, controladores y repositorios. No contienen campos mutables; cada request
  recibe su propio `HttpServletRequest`/argumento.
- **Stateful:** `RateLimitService` (`@Component`) mantiene un `ConcurrentHashMap<String, Bucket>`
  con un bucket por `sessionId|IP`. Su estado crece con los clientes y por eso se gestiona con
  poda (`evictIfNeeded`). Este es el componente stateful del sistema y es donde se evidencian los
  callbacks del contenedor.

### 8.3 Evidencia del ciclo de vida gestionado por el contenedor
- **Inicialización:** `AdminInitializer implements ApplicationRunner` — el contenedor invoca
  `run(...)` al terminar de cargar el contexto y si no existe la cuenta admin la crea (BCrypt).
  Este runtime es evidencia del callback de inicialización del contenedor.
- **Destrucción:** los beans singletons son destruidos por el contenedor al apagar la aplicación;
  para evidenciarlo de forma explícita se incorpora en `RateLimitService` un `@PreDestroy` que
  limpia el mapa de buckets al cierre.
- **Frontend (React):** los componentes también viven un ciclo de vida (montaje/desmontaje)
  gestionado por React. Ejemplos stateless puros: `CarCard`, `Pagination`, `Footer`.
  Ejemplos stateful (con `useState`/`useEffect`): `LoginPage`, `HeroCarousel`.
  Callbacks de destrucción: `Reveal` desconecta su `IntersectionObserver` en el cleanup del
  `useEffect`; `HeroCarousel` limpia su `setInterval` al desmontar.

---

## 9. Patrones de diseño aplicados y justificados

### 9.1 DAO (Data Access Object) — repositorios Spring Data
`CarRepository`, `AppUserRepository`, `CartRepository`, `OrderRepository`,
`AiRecommendationLogRepository`. **Justificación:** aíslan la persistencia de la lógica de negocio.
La capa de aplicación depende de la abstracción y nunca del SQL concreto; Flyway garantiza que el
esquema esperado exista. Es el patrón que habilita la regla "solo la capa de datos toca la BD".

### 9.2 Facade — Services de negocio
`CarService`, `CartService`, `OrderService` y sobre todo `GroqAdvisorService`. **Justificación:**
el `GroqAdvisorService` expone una única operación (`recommend`) que internamente orquesta
*rate limiting → catálogo → llamada al LLM → anti-alucinación → log*. El controlador queda en un
nivel de abstracción mínima y la lógica es testeable sin HTTP.

### 9.3 Adapter — abstracción del cliente Groq
`GroqApiClient` (interfaz) + `HttpGroqApiClient` (implementación). **Justificación:** el dominio
no depende de la API HTTP de Groq; un change de proveedor (o una implementación fake para tests)
no toca al orquestador. También se usan adaptadores de DTO: `CarResponse.from(Car)` y
`PageResponse.from(Page<T>)` convierten el modelo de dominio al contrato de la API.

### 9.4 Factory — configuración de beans y DTOs
`SecurityConfig` define factories (`securityFilterChain()`, `passwordEncoder()` → BCrypt),
`RestClientConfig` construye el `RestClient`, y `PageResponse.from` es una factory estática.
**Justificación:** centraliza cómo se construyen objetos de dependencia crítica (filtro de
seguridad, encoder) y oculta la construcción de objetos complejos (páginas DTO).

### 9.5 Strategy (criterio) — filtrado dinámico del catálogo
`CarSpecification` implementa la estrategia de búsqueda combinando predicados opcionales
(marca, precios, año, combustible, carrocería, texto). **Justificación:** incorporar un filtro
nuevo agrega un predicado sin tocar el controlador ni el servicio (abierto/cerrado). En el
asesor IA, `GroqApiClient` también permite intercambiar la estrategia de conexión por build.

### 9.6 Beneficio transversal
Todos los patrones refuerzan el mismo objetivo: **bajo acoplamiento entre capas, alta cohesión
dentro de cada capa**, lo que permite pruebas unitarias aisladas y evolución sin efectos de borde.

---

## 10. Seguridad declarativa mínima

La seguridad es **declarativa** (se expresa por configuración/anotaciones, no embebida en el
código de negocio):

1. `SecurityConfig.securityFilterChain(...)` establece el contrato por **matchers**:
   - `permitAll`: `GET /api/cars`, `/api/cars/**`, `/api/auth/**`, `/actuator/health`, Swagger.
   - `authenticated`: todo lo demás (`/api/cart/**`, `/api/orders/**`, `/api/ai/**`).
2. `@EnableMethodSecurity` habilita la autorización por anotaciones.
3. `@PreAuthorize("hasRole('ADMIN')")` protege las **operaciones sensibles**:
   `POST /api/cars`, `PUT /api/cars/{id}`, `DELETE /api/cars/{id}`.
4. JWT firmado (HS384) parseado y validado en cada request; contraseñas con **BCrypt**; CSRF
   desactivado al ser API stateless.

### Evidencia de cumplimiento (pruebas realizadas contra el servicio corriendo)
| Credencial | `POST /api/cars` | Resultado |
|---|---|---|
| Sin token | 401 | Sesión no autorizada |
| Cliente registrado (`CUSTOMER`) | 403 | Forbidden (rol insuficiente) |
| Administrador inicial | 201 | Auto creado (luego edición 200 y borrado 204) |

---

## 11. Despliegue y verificación

El sistema corre completo y en capas en la máquina local mediante scripts (`C:\driveai\.tools`):

| Servicio | URL | Puerto |
|---|---|---|
| Frontend (Vite, dev) | http://localhost:5173 | 5173 |
| Backend (Spring Boot) | http://localhost:8080 | 8080 |
| Swagger UI | http://localhost:8080/swagger-ui.html | 8080 |
| PostgreSQL (portátil) | jdbc:postgresql://localhost:5433/cardb | 5433 |
| pgAdmin 4 | `abrir-pgadmin.bat` desde el repo | — |

**Demo de trazabilidad (une las capas):** el admin publica un auto desde `AdminPage` → se inserta en
`cars` (PostgreSQL) → se comprueba en pgAdmin (`SELECT ...`) → aparece en el catálogo del frontend.
El mismo criterio aplica a órdenes y logs de IA.

---

## 12. Decisiones de diseño y su motivo

| Decisión | Motivo |
|---|---|
| SPA (React) separada del backend | despliegue independiente; cache de queries; UX reactiva |
| API REST stateless con JWT | escalabilidad horizontal; segura sin fijar sesiones al servidor |
| Carrito por `sessionId` en el cuerpo | compatible con clientes no-navegador; testable con curl |
| Precio congelado al agregar al carrito | transparencia de precios ante cambios posteriores |
| Flyway + `ddl-auto: validate` | el esquema es código versionado, nunca se muda a mano |
| Rate limit en la IA | evitar abuso de la cuota gratuita de Groq |
| Anti-alucinación (ids contrastados) | calidad de recomendación: nunca muestra autos inexistentes |
| Admin inicial vía variables de entorno | bootstrap sin migraciones manuales y sin credenciales en el código |

## 13. Trabajo futuro
- Vista de órdenes para el admin (gestionar estados PENDING/CONFIRMED/CANCELLED).
- Deploy público a Render/Railway para una URL accesible fuera de localhost.
- Pruebas E2E (Playwright) y más test unitarios de las services.
- Paginación del historial de recomendaciones IA.

## 14. Conclusión
DriveAI cumple con los requisitos de la entrega: **cuatro componentes reales**, cada uno con
**arquitectura en capas** verificable y desplegada; **componentes stateful y stateless** con
ciclo de vida gestionado por contenedores (Spring y React); **cinco patrones de diseño**
(DAO, Facade, Adapter, Factory, Strategy) aplicados y justificados; y **seguridad declarativa**
por rol sobre operaciones sensibles, demostrada contra el servicio en ejecución.