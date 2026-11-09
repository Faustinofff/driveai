# Manual de usuario — DriveAI

DriveAI es un e-commerce de autos usados certificados con un asesor de compra asistido
por IA. Este manual explica cómo iniciar la aplicación y cómo usar cada función.

---

## 1. Requisitos

### Opción A — Docker (recomendada)

- Docker Engine 24+ y Docker Compose v2.
- No hace falta instalar Java, Node ni PostgreSQL.

### Opción B — Ejecución nativa

- JDK 17, Maven 3.9+, Node 20+, PostgreSQL 16.
- RabbitMQ 3.13 (opcional: sin broker el e-commerce funciona, pero los eventos quedan
  pendientes).

---

## 2. Inicio rápido con Docker

```bash
# 1) Crear el archivo de variables
cp .env.example .env
```

Editar `.env` y completar como mínimo:

```dotenv
POSTGRES_DB=cardb
POSTGRES_USER=cardb_user
POSTGRES_PASSWORD=una_clave_segura

JWT_SECRET=<generado con: openssl rand -base64 48>
ADMIN_EMAIL=admin@driveai.com
ADMIN_PASSWORD=<tu_contraseña_de_demo>

# Opcional: sin esta clave el asesor IA responde 503
GROQ_API_KEY=
```

```bash
# 2) Levantar todo
docker compose up -d --build

# 3) Verificar
docker compose ps
curl http://localhost:8080/actuator/health
```

Abrir **http://localhost:5173**.

### Servicios y puertos

| Servicio | URL |
| --- | --- |
| Frontend | http://localhost:5173 |
| Backend | http://localhost:8080 |
| Swagger UI | http://localhost:8080/swagger-ui.html |
| RabbitMQ | http://localhost:15672 |
| Mock SOAP | http://localhost:8089/soap/vehicles |

Para detener: `docker compose down`. Para borrar datos: `docker compose down -v`.

---

## 3. Inicio sin Docker

```bash
# 1) PostgreSQL en localhost:5432, base cardb, usuario cardb_user
#    (o definir SPRING_DATASOURCE_URL / USERNAME / PASSWORD)

# 2) Backend
cd backend
export JWT_SECRET="$(openssl rand -base64 48)"
export ADMIN_EMAIL=admin@driveai.com
export ADMIN_PASSWORD=tu_password_demo
export GROQ_API_KEY=            # opcional
mvn spring-boot:run

# 3) Frontend (otra terminal)
cd frontend
npm install
npm run dev
```

Si el backend corre en otro puerto/origen, definir `VITE_API_URL` antes de `npm run dev`:

```bash
VITE_API_URL=http://localhost:8080 npm run dev
```

El valor puede incluir o no el sufijo `/api`; la aplicación lo normaliza.

---

## 4. Usuarios de la demo

| Rol | Email | Contraseña |
| --- | --- | --- |
| ADMIN | el valor de `ADMIN_EMAIL` (ej. `admin@driveai.com`) | el valor de `ADMIN_PASSWORD` |
| CUSTOMER | cualquiera que se registre en `/#/login` | la que elija (mín. 8 caracteres) |

El administrador se crea automáticamente en el primer arranque si no existe. **La
contraseña no está versionada**: se define en `.env`.

---

## 5. Recorrido de la aplicación

### 5.1 Inicio

La página principal muestra el hero, categorías, autos destacados y llamadas a la acción.
La navegación principal está en la barra superior: **Inicio**, **Productos**, **IA**,
carrito, tema claro/oscuro e **Ingresar**.

### 5.2 Registro e inicio de sesión

1. Ir a **Ingresar**.
2. Para crear una cuenta: pestaña **Crear cuenta**, completar nombre, email y contraseña.
3. Para ingresar: pestaña **Ingresar**, email y contraseña.
4. Al autenticarse, la barra muestra el nombre del usuario y **Cerrar sesión**.

Errores frecuentes:

- `Email o contraseña incorrectos.` → credenciales inválidas.
- `Demasiados intentos...` → rate limit (5 intentos por minuto); esperar y reintentar.

### 5.3 Catálogo

1. Ir a **Productos** (`#/catalog`).
2. Usar el buscador de la barra o los filtros laterales: marca, precio mínimo/máximo,
   año, combustible y carrocería.
3. **Limpiar filtros** restablece filtros y búsqueda.
4. Hacer clic en la imagen o en el nombre de un auto para abrir el **detalle**.
5. En el detalle se ven precio, stock, año, kilometraje, consumo, transmisión y
   descripción. Cerrar con la **X**, con **Volver al catálogo** o con **Escape**.

### 5.4 Carrito

1. En una tarjeta o en el detalle, pulsar **Agregar al carrito**.
2. Abrir el carrito con el ícono del carrito.
3. Ajustar cantidades con **+ / −** o eliminar una línea.
4. El subtotal se actualiza automáticamente.

> Para comprar es necesario iniciar sesión. Si no hay sesión, la app redirige al login
> con el aviso "Inicia sesión para comprar en DriveAI.".

### 5.5 Checkout

1. En el carrito, pulsar **Finalizar compra**.
2. La orden se crea de forma transaccional, se descuenta el stock y se envía un evento al
   broker (outbox).
3. Se muestra **¡Pedido confirmado!** con el número de orden y el estado `CONFIRMED`.
4. **Seguir comprando** cierra el mensaje.

Si el stock cambió entre la carga del catálogo y el checkout, la operación se rechaza con
un mensaje claro y no se crea la orden.

### 5.6 Asesor de compra con IA

1. Ir a **IA** (`#/ai`) con sesión iniciada.
2. Describir lo que se busca, por ejemplo:
   > "Busco una SUV familiar híbrida por menos de 30000 dólares"
3. El asesor responde con una recomendación y tarjetas de autos reales en stock.
4. Se puede agregar un auto recomendado al carrito desde la tarjeta.

Si el proveedor de IA no está disponible o no hay `GROQ_API_KEY`, se muestra un mensaje
de fallback y el resto de la tienda sigue funcionando.

### 5.7 Panel de administración

1. Iniciar sesión con una cuenta **ADMIN**.
2. Ir a `#/admin` o usar el acceso de administración.
3. **Nuevo auto** abre el formulario: marca, modelo, año, precio, kilometraje, stock,
   combustible, consumo, transmisión, carrocería, URL de imagen y descripción.
4. **Editar** permite modificar un auto existente (control de concurrencia con ETag).
5. **Eliminar** pide confirmación y borra el auto si no tiene ventas asociadas.

Un usuario CUSTOMER que intente entrar a `#/admin` verá **"Acceso restringido"** y podrá
cambiar de cuenta.

### 5.8 Tema claro/oscuro

El botón de sol/luna alterna el tema. La preferencia se conserva en el navegador.

---

## 6. Preguntas frecuentes

**¿Necesito una API key de Groq?**
No. Sin clave, el asesor IA responde 503 con un mensaje amigable; el catálogo, carrito y
checkout funcionan normalmente.

**¿Por qué el health devuelve `DOWN`?**
Si RabbitMQ no está disponible, el indicador de salud del broker baja el estado global.
El e-commerce sigue operativo y los eventos quedan pendientes hasta que el broker vuelva.

**¿Dónde veo la documentación de la API?**
En `http://localhost:8080/swagger-ui.html` o en `http://localhost:8080/v3/api-docs`.

**¿Cómo reinicio la base?**
`docker compose down -v` y volver a levantar. Las migraciones Flyway se aplican solas.

**¿Cómo cambio el administrador?**
Modificar `ADMIN_EMAIL`/`ADMIN_PASSWORD` en `.env` y reiniciar. Si el email ya existe, la
contraseña no se sobrescribe automáticamente.

---

## 7. Problemas comunes

| Síntoma | Causa probable | Solución |
| --- | --- | --- |
| El frontend no carga datos | Backend caído o `VITE_API_URL` incorrecto | Verificar `http://localhost:8080/actuator/health` |
| `401` en el carrito | Token ausente/expirado | Volver a iniciar sesión |
| `429` al iniciar sesión | Rate limit | Esperar un minuto |
| `503` en el asesor IA | Sin `GROQ_API_KEY` o proveedor caído | Configurar la clave o usar el resto de la tienda |
| `DOWN` en health | RabbitMQ no disponible | Levantar RabbitMQ o aceptar el modo degradado |
| El backend no arranca en prod | Falta una variable obligatoria | Revisar el mensaje `Falta … en .env` |
| `412` al editar un auto | El auto cambió desde que se cargó | Recargar y volver a editar |
