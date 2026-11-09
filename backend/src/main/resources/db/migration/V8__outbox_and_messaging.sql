-- ============================================================================
-- DriveAI — V8: Outbox transaccional + consumidor idempotente
--
-- `outbox_events` garantiza que un evento de negocio se persista en la misma
-- transacción que la orden. Un publicador lo envía luego al broker; si el
-- broker está caído el evento permanece PENDING y se reintenta.
--
-- `processed_messages` hace idempotente al consumidor: un mensaje duplicado
-- (mismo messageId) se procesa una sola vez.
-- ============================================================================

CREATE TABLE IF NOT EXISTS outbox_events (
    id             BIGSERIAL PRIMARY KEY,
    aggregate_type VARCHAR(50)  NOT NULL,
    aggregate_id   BIGINT       NOT NULL,
    event_type     VARCHAR(80)  NOT NULL,
    payload        TEXT         NOT NULL,
    status         VARCHAR(20)  NOT NULL DEFAULT 'PENDING'
                   CHECK (status IN ('PENDING', 'SENT', 'FAILED')),
    attempts       INTEGER      NOT NULL DEFAULT 0,
    last_error     TEXT,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    sent_at        TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_outbox_status_created ON outbox_events (status, created_at);

CREATE TABLE IF NOT EXISTS processed_messages (
    message_id   VARCHAR(100) PRIMARY KEY,
    consumer     VARCHAR(100) NOT NULL,
    processed_at TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP
);
