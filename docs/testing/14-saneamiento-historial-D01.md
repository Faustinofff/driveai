# 14 · Saneamiento del historial (D-01 → G6)

> **Ejecutado el 10/11/2026** con autorización del equipo (push desde la cuenta de
> JIA2004, colaborador con permiso de push; `main` no tiene protección de rama).
> Diferencias respecto del procedimiento de abajo: se usó `git filter-branch
> --tree-filter` porque la máquina no tenía Python para `git filter-repo`; además de la
> contraseña del README se reemplazó la clave JWT de prueba de `ApiContractIT.java`
> (Gitleaks 8.30.1 la marcaba como `generic-api-key`). Resultado: `git grep` 0 archivos y
> Gitleaks `12 commits scanned … no leaks found` sobre mirror y clon local. Hashes nuevos:
> `main` `024f6fd → b1baece`, `3f19436 → 4b38562`. Detalle y evidencia en `10`, D-01.
> **Cada integrante tiene que borrar su clon y clonar de nuevo.** Los commits viejos
> siguen accesibles por URL directa en GitHub hasta pedir a Support que los purgue (§4).

G6 es **eliminatorio**: la cátedra corre gitleaks/trufflehog sobre **todo el historial**.
Que el código actual esté limpio no alcanza. D-01 (contraseña de demo en el commit
`3f19436`) tiene que salir del historial **antes de crear el tag**.

> Reescribir la historia cambia los hashes de todos los commits posteriores. Hay que
> coordinarlo con todo el equipo: después, cada integrante vuelve a clonar.

## 1. Rotar primero

La contraseña ya es pública desde que se pusheó. Antes de cualquier otra cosa:

- Cambiar `ADMIN_PASSWORD` en el `.env` de cada máquina y en cualquier entorno donde
  se haya usado.
- Si esa contraseña se usó en otro servicio, cambiarla también ahí.

## 2. Reescribir con `git filter-repo`

```bash
# En una copia NUEVA y completa (no shallow) del repo
git clone --mirror https://github.com/Faustinofff/driveai.git driveai-mirror
cd driveai-mirror

# replacements.txt: una línea por secreto, con este formato:
#   <contraseña-filtrada>==>REDACTED
# (no commitear este archivo)
pip install git-filter-repo
git filter-repo --replace-text ../replacements.txt

# Verificar ANTES de pushear: tiene que dar 0 hallazgos
gitleaks detect --source . --log-opts="--all"

git push --force --mirror
```

## 3. Después del push

1. Cada integrante borra su clon local y vuelve a clonar (no hacer `pull` sobre el viejo).
2. Cerrar los PR abiertos basados en la historia vieja y recrearlos.
3. En `.github/workflows/ci.yml`, job `security-scan`, **quitar
   `continue-on-error: true`** del paso Gitleaks (y el comentario), para que el pipeline
   falle ante cualquier secreto nuevo. Tener en cuenta que `gitleaks-action` en un
   evento `push` escanea **solo los commits del push** (por eso hoy el job dice
   `no leaks found` aunque D-01 siga en el historial); la evidencia de G6 es la corrida
   manual del paso 2 sobre el mirror, con `--log-opts="--all"`. Guardar esa salida.
4. Marcar D-01 como cerrado en `10-registro-defectos.md` y en Jira
   ([G7-11](https://uade-team-jjfya715.atlassian.net/browse/G7-11)), con causa raíz:
   "contraseña de demo escrita en el README; se previene con Gitleaks bloqueante en CI".
5. Recién ahí crear el tag `v1.0-testing` en `main`:

   ```bash
   git checkout main && git pull
   git tag v1.0-testing && git push origin v1.0-testing
   ```

   El push del tag dispara el CI (`on.push.tags: ['v*']`). Copiar el link de ese run en
   `11-informe-ejecucion.md` (encabezado y §5), `02b-matriz-trazabilidad.md` y
   `10-registro-defectos.md`, y volver a bajar los artefactos.

## 4. Si GitHub mantiene referencias viejas

Los forks y las vistas cacheadas de PR pueden conservar el commit viejo. Si el
commit `3f19436` sigue accesible por URL, pedir a GitHub Support que purgue la caché
("remove sensitive data"). La contraseña ya rotada (paso 1) es lo que realmente
neutraliza el riesgo.
