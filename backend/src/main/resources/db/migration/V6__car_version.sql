-- ============================================================================
-- DriveAI — V6: Control de concurrencia optimista para el catálogo
--
-- `version` es gestionado por JPA (@Version) y expuesto como ETag en la API.
-- Una actualización con If-Match desactualizado recibe HTTP 412.
-- ============================================================================

ALTER TABLE cars
    ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;
