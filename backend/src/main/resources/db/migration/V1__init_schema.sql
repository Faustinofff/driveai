-- ============================================================================
-- DriveAI — V1: Esquema inicial
-- PostgreSQL 16. Los enums se modelan como VARCHAR + CHECK (portable) y los
-- valores en base coinciden 1:1 con las constantes Java de com.driveai.enums.
-- ============================================================================

-- ----------------------------------------------------------------------------
-- Tabla: cars — catálogo de vehículos
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS cars (
    id                 BIGSERIAL PRIMARY KEY,
    brand              VARCHAR(100) NOT NULL,
    model              VARCHAR(100) NOT NULL,
    year               INTEGER      NOT NULL CHECK (year BETWEEN 1900 AND 2100),
    price              NUMERIC(12,2) NOT NULL CHECK (price >= 0),
    mileage            INTEGER      NOT NULL CHECK (mileage >= 0),
    fuel_type          VARCHAR(20)  NOT NULL CHECK (fuel_type IN ('GASOLINA', 'DIESEL', 'HIBRIDO', 'ELECTRICO')),
    fuel_consumption   NUMERIC(5,2) NOT NULL CHECK (fuel_consumption >= 0),
    transmission       VARCHAR(20)  NOT NULL CHECK (transmission IN ('MANUAL', 'AUTOMATICA')),
    body_type          VARCHAR(20)  NOT NULL CHECK (body_type IN ('HATCHBACK', 'SEDAN', 'SUV', 'PICKUP')),
    image_url          TEXT         NOT NULL,
    description        TEXT         NOT NULL,
    stock              INTEGER      NOT NULL DEFAULT 0 CHECK (stock >= 0),
    created_at         TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at         TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_cars_brand          ON cars (brand);
CREATE INDEX IF NOT EXISTS idx_cars_fuel_type      ON cars (fuel_type);
CREATE INDEX IF NOT EXISTS idx_cars_body_type      ON cars (body_type);
CREATE INDEX IF NOT EXISTS idx_cars_price          ON cars (price);
CREATE INDEX IF NOT EXISTS idx_cars_year           ON cars (year);

-- ----------------------------------------------------------------------------
-- Tabla: carts — carrito anónimo identificado por sessionId
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS carts (
    id          BIGSERIAL PRIMARY KEY,
    session_id  VARCHAR(64) NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_carts_session UNIQUE (session_id)
);

-- ----------------------------------------------------------------------------
-- Tabla: cart_items — líneas del carrito con precio congelado
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS cart_items (
    id             BIGSERIAL PRIMARY KEY,
    cart_id        BIGINT       NOT NULL REFERENCES carts (id) ON DELETE CASCADE,
    car_id         BIGINT       NOT NULL REFERENCES cars (id) ON DELETE RESTRICT,
    quantity       INTEGER      NOT NULL DEFAULT 1 CHECK (quantity > 0),
    price_snapshot NUMERIC(12,2) NOT NULL CHECK (price_snapshot >= 0),
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_cart_item_car UNIQUE (cart_id, car_id)
);

CREATE INDEX IF NOT EXISTS idx_cart_items_car ON cart_items (car_id);

-- ----------------------------------------------------------------------------
-- Tabla: orders — pedido transaccional
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS orders (
    id           BIGSERIAL PRIMARY KEY,
    session_id   VARCHAR(64)   NOT NULL,
    status       VARCHAR(20)   NOT NULL DEFAULT 'PENDING'
                 CHECK (status IN ('PENDING', 'CONFIRMED', 'CANCELLED')),
    total_amount NUMERIC(14,2) NOT NULL CHECK (total_amount >= 0),
    created_at   TIMESTAMPTZ   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMPTZ   NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_orders_session ON orders (session_id);

-- ----------------------------------------------------------------------------
-- Tabla: order_items — líneas del pedido con precio de venta
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS order_items (
    id         BIGSERIAL PRIMARY KEY,
    order_id   BIGINT       NOT NULL REFERENCES orders (id) ON DELETE CASCADE,
    car_id     BIGINT       NOT NULL REFERENCES cars (id) ON DELETE RESTRICT,
    quantity   INTEGER      NOT NULL CHECK (quantity > 0),
    unit_price NUMERIC(12,2) NOT NULL CHECK (unit_price >= 0)
);

CREATE INDEX IF NOT EXISTS idx_order_items_order ON order_items (order_id);
CREATE INDEX IF NOT EXISTS idx_order_items_car   ON order_items (car_id);

-- ----------------------------------------------------------------------------
-- Tabla: ai_recommendation_logs — auditoría de prompts/respuestas del LLM
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ai_recommendation_logs (
    id                   BIGSERIAL PRIMARY KEY,
    session_id           VARCHAR(64),
    prompt               TEXT        NOT NULL,
    response             TEXT,
    recommended_car_ids  TEXT,
    status               VARCHAR(20) NOT NULL,
    error_message        TEXT,
    created_at           TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_ai_logs_session ON ai_recommendation_logs (session_id);
CREATE INDEX IF NOT EXISTS idx_ai_logs_created ON ai_recommendation_logs (created_at);