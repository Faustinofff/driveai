-- ============================================================================
-- DriveAI — V7: Idempotency-Key para POST de checkout
--
-- La clave es única por usuario. Se guarda un hash del cuerpo para rechazar
-- el reuso de la misma clave con un payload distinto y la respuesta serializada
-- para devolver exactamente el mismo resultado en reintentos.
-- ============================================================================

CREATE TABLE IF NOT EXISTS idempotency_keys (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT       NOT NULL REFERENCES app_users (id) ON DELETE CASCADE,
    idempotency_key VARCHAR(255) NOT NULL,
    request_hash    VARCHAR(64)  NOT NULL,
    status_code     INTEGER,
    response_body   TEXT,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at    TIMESTAMPTZ,
    CONSTRAINT uk_idempotency_user_key UNIQUE (user_id, idempotency_key)
);
