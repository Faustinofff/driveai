-- ============================================================================
-- DriveAI — V9: reemplaza tres imágenes del seed que Unsplash dejó de servir
-- (HTTP 404). En la interfaz esas tarjetas quedaban con el esqueleto de carga
-- girando para siempre. Solo se tocan las filas que todavía apuntan a la URL
-- rota original: si un administrador ya cambió la imagen, se respeta.
-- ============================================================================

UPDATE cars
SET image_url = 'https://images.unsplash.com/photo-1471444928139-48c5bf5173f8?auto=format&fit=crop&w=1200&q=80'
WHERE id = 3
  AND image_url = 'https://images.unsplash.com/photo-1533416784636-2b0ccfea6c97?auto=format&fit=crop&w=1200&q=80';

UPDATE cars
SET image_url = 'https://images.unsplash.com/photo-1519641471654-76ce0107ad1b?auto=format&fit=crop&w=1200&q=80'
WHERE id = 20
  AND image_url = 'https://images.unsplash.com/photo-1606015960883-4a6dd942843d?auto=format&fit=crop&w=1200&q=80';

UPDATE cars
SET image_url = 'https://images.unsplash.com/photo-1617531653332-bd46c24f2068?auto=format&fit=crop&w=1200&q=80'
WHERE id = 23
  AND image_url = 'https://images.unsplash.com/photo-1601362840469-51e4d8a58785?auto=format&fit=crop&w=1200&q=80';
