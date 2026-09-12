-- ============================================================================
-- DriveAI — V3: Usuarios y roles para autenticación (JWT)
-- Roles: ADMIN (publica/edita autos) y CUSTOMER (compra, consulta IA).
-- La tabla se llama app_users para no colisionar con la palabra reservada
-- "user" de SQL.
-- ============================================================================

CREATE TABLE IF NOT EXISTS app_users (
    id            BIGSERIAL PRIMARY KEY,
    email         VARCHAR(255) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    full_name     VARCHAR(150) NOT NULL,
    role          VARCHAR(20)  NOT NULL CHECK (role IN ('ADMIN', 'CUSTOMER')),
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_app_users_email ON app_users (LOWER(email));