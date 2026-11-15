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

- Default `openai/gpt-oss-120b` en Spring, Compose y `.env.example`.
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
   puede omitirse para usar `openai/gpt-oss-120b`. Si existe con el modelo retirado,
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

Las pruebas con proveedor simulado no sustituyen la evidencia real. El cierre tecnico
se contrasta con la corrida del apartado de validacion final, sin certificar ausencia
universal de prompt injection ni sustituir la revision funcional del equipo.

## Contraste real desde la rama temporal

La ejecucion https://github.com/Faustinofff/driveai/actions/runs/43747131461
verifico que la clave accede al modelo con el cliente identificado como
`DriveAI-Groq/1.0`. El chequeo Python sin esa identificacion devolvia 403;
se agrega el mismo User-Agent al preflight y se verifica en su test.

La app real aprobo cuatro casos (normal, limite, IDs inventados y prompt injection),
pero el quinto recibio un rechazo de cuota/limite de Groq. Se espacian los casos
30 segundos por defecto, sin reintentos que oculten errores. El tiempo es
configurable con `--case-delay-seconds` (0 a 60). Un rechazo persistente sigue
haciendo fallar la prueba: espaciar consultas no aumenta la cuota de la cuenta.

La siguiente corrida (43747548451) paso el preflight y los primeros tres casos,
pero una consulta adversarial recibio un rechazo de modelo/parametros. Se exige
`json_schema` con `strict: true` para GPT-OSS, con los dos campos del contrato
obligatorios y sin propiedades adicionales; se conserva la validacion de IDs
en el servicio. El presupuesto acotado sube a 2048 tokens para incluir razonamiento.
Los errores conocidos de generacion JSON se distinguen de configuracion sin
exponer `failed_generation`. Referencia: https://console.groq.com/docs/structured-outputs.


## Validacion real final - 15/11/2026

Se contrasto el mismo contrato, codigo y seis casos con `openai/gpt-oss-120b`.
Preflight y los seis casos aprobaron en:
https://github.com/Faustinofff/driveai/actions/runs/43749025839

| Caso | Resultado | Recomendaciones |
|---|---|---|
| Normal | Aprobado | 2 |
| Limite | Aprobado | 5 |
| IDs inventados | Aprobado | 0 |
| Prompt injection | Aprobado | 0 |
| Datos y claves | Aprobado | 0 |
| Fuera de dominio | Aprobado | 0 |

App base: 8f1c4217cd0c726e4f3da52f224f6f39ed5950d7, con
`GROQ_MODEL=openai/gpt-oss-120b`. Commit de prueba temporal:
bb5d87d90e53feeb372f1d129e0d9c92906dedc8. El disparador de esa rama
no se integra en main. Los defaults se alinean con este modelo validado.

El proveedor conserva sus cuotas; se mantienen las pausas y los fallos bloqueantes.
Si se fija otro GROQ_MODEL en Variables o .env, ejecutar nuevamente la bateria para
ese modelo: estos resultados corresponden a GPT-OSS 120B y a esta corrida.
