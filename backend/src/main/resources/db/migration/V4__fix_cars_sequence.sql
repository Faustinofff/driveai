-- ============================================================================
-- DriveAI — V4: Re-sincroniza la secuencia de cars
-- V2 insertó autos con ids explícitos (1..25) sin avanzar la secuencia del
-- catálogo; sin esta corrección cualquier alta nueva de un auto chocaría con
-- la PK reutilizando el id 1. Es idempotente: recalcula desde MAX(id).
-- ============================================================================

SELECT setval(
    pg_get_serial_sequence('cars', 'id'),
    (SELECT COALESCE(MAX(id), 0) + 1 FROM cars),
    false
);