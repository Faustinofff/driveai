# Groq real: modelo vigente y diagnostico

## Que fallo el 15/11/2026

La ejecucion https://github.com/Faustinofff/driveai/actions/runs/43742209441
aprobo despliegue, compra/State/mensajeria, accesibilidad y carga. El primer
pedido del caso normal de IA devolvio HTTP 503: ningun caso de IA quedo aprobado.
El secret llego al entorno; eso no demuestra por si solo que el proveedor lo acepte.

El codigo usaba por defecto `groq/compound-mini`. Groq documento su retiro el
21/09/2026: https://console.groq.com/docs/compound/systems/compound-mini.
Es una configuracion invalida confirmada; el log anterior no permite descartar
otros problemas de autenticacion, cuota o conectividad.

## Correccion

- Default `openai/gpt-oss-20b` en Spring, Compose y `.env.example`.
  Modelo vigente y JSON: https://console.groq.com/docs/models.
- Parametro `max_completion_tokens`; para GPT-OSS, razonamiento bajo y sin
  devolver razonamiento interno. Se conserva la validacion de IDs contra la BD.
- Las consultas fuera de dominio no deben generar recomendaciones y el prompt
  distingue datos del cliente/catalogo de las instrucciones del asesor.
- Respuestas HTTP 400/404/422 del proveedor se informan como configuracion de
  modelo/parametros; no se incluye el cuerpo crudo del proveedor en la respuesta.
- El workflow manual comprueba primero autenticacion y disponibilidad del modelo
  contra `/models`. Luego verifica la aplicacion real con los seis casos existentes.
  Pasar `/models` no sustituye aprobar los seis casos.
- Los reportes muestran categorias de errores, no tokens ni respuestas crudas.

## Que hacer despues de integrar este cambio

1. Mantener `GROQ_API_KEY` como secret: no ponerlo en codigo ni en Variables.
2. En GitHub Settings > Secrets and variables > Actions > Variables, `GROQ_MODEL`
   puede omitirse para usar `openai/gpt-oss-20b`. Si existe con el modelo retirado,
   cambiarla por el nuevo identificador. Para un entorno local, revisar tambien
   el `.env` que ya se hubiera creado: cambiar el default no edita ese archivo.
3. Actions > QA - cierre final desplegado > Run workflow > main > `live_ai=true`.
4. Revisar primero "Verificar clave y modelo de Groq antes de desplegar".
   - 401/403: clave o permisos en la cuenta de Groq.
   - Modelo no disponible: identificador/permisos del modelo para esa cuenta.
   - 429: cuota o limite de la cuenta; no intentar resolverlo publicando la clave.
5. Exigir que "Bateria IA real solicitada por el responsable" apruebe los seis
   casos y revisar funcionalmente la calidad de recomendaciones. Conservar
   `final-qa-evidence/groq-provider.json` y `live-ai.json`.

La correccion preparada y las pruebas con proveedor simulado no se presentan
como una nueva corrida Groq real. KAN-42/KAN-43 continuan pendientes de esa evidencia.
