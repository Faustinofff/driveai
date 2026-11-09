-- ============================================================================
-- DriveAI — V5: Propiedad de carritos y órdenes por usuario autenticado
--
-- Corrige BOLA/IDOR: el carrito y las órdenes dejan de depender de un
-- `sessionId` controlado por el cliente y pasan a pertenecer al usuario del
-- JWT (user_id). `session_id` se conserva nullable para no romper datos
-- históricos de desarrollo.
-- ============================================================================

ALTER TABLE carts
    ADD COLUMN IF NOT EXISTS user_id BIGINT REFERENCES app_users (id) ON DELETE CASCADE;

ALTER TABLE carts
    ALTER COLUMN session_id DROP NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uk_carts_user ON carts (user_id) WHERE user_id IS NOT NULL;

ALTER TABLE orders
    ADD COLUMN IF NOT EXISTS user_id BIGINT REFERENCES app_users (id) ON DELETE SET NULL;

ALTER TABLE orders
    ALTER COLUMN session_id DROP NOT NULL;

CREATE INDEX IF NOT EXISTS idx_orders_user ON orders (user_id);
