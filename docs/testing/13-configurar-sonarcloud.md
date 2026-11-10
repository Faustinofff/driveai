# 13 · Configurar SonarCloud (Quality Gate del Eje B)

El job `sonarcloud` del pipeline ya está definido. Mientras no esté configurado, emite
una **advertencia** y no analiza (no rompe G3), pero sin Quality Gate el criterio del
Eje B no está cumplido.

## Pasos (una sola vez, ~10 min)

1. Entrar a <https://sonarcloud.io> con la cuenta de GitHub dueña del repo.
2. **+ → Analyze new project**, elegir `driveai`. Anotar la **Organization key** y la
   **Project key**.
3. En el proyecto: **Administration → Analysis Method → desactivar "Automatic
   Analysis"** (el análisis lo hace el pipeline; si quedan los dos activos, falla).
4. **My Account → Security → Generate token**. Copiarlo.
5. En GitHub, repo → **Settings → Secrets and variables → Actions**:
   - pestaña **Secrets** → `SONAR_TOKEN` = el token.
   - pestaña **Variables** → `SONAR_ORGANIZATION` y `SONAR_PROJECT_KEY`.
6. Hacer un push. El job `Calidad · SonarCloud Quality Gate` espera el resultado
   (`sonar.qualitygate.wait=true`) y falla si el gate no aprueba.

## Para la entrega

- Captura del Quality Gate en el informe de ejecución (`11`, §3).
- Revisar en SonarCloud el **100 % de los Security Hotspots** (marcar cada uno como
  Safe o Fixed, con comentario). La cátedra lo exige.
- Umbrales: 0 bugs Blocker/Critical, 0 vulnerabilidades, duplicación ≤ 3 %, ratings A
  en confiabilidad y seguridad.
