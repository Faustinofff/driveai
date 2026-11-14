# Entrega parcial 2 — verificacion del nucleo (16/11/2026)

Revision: 14/11/2026. Base revisada: e0551e9f3811cfb299925ad5a3a87627cceb0452.
Las correcciones se preparan en la rama fix/entrega-2-core para revision mediante PR.
La autorizacion de la integracion en la cuenta propietaria ya permite crear ramas.
No se modifico Jira. No se acredita despliegue remoto ni integracion a main.

## Matriz de cumplimiento

| Requisito | Evidencia y correccion | Estado |
|---|---|---|
| Componentes en capas desplegados | React/Nginx → REST Controller → Service → Repository → PostgreSQL. Compose incluye broker y mock SOAP | Implementacion y configuracion presentes; ejecucion de despliegue pendiente |
| Primer flujo punta a punta | Registro/login → catalogo → carrito → checkout → stock/orden/outbox → RabbitMQ → consumidor | Hay Playwright y ApiContractIT existentes; agregado smoke por origen frontend y consulta del consumo. No ejecutado contra stack aqui |
| Patrones con ADR | 11 decisiones en docs/adr, incluidas las cinco familias nombradas en la memoria anterior | Documentados con rutas y pruebas; revision del equipo pendiente |
| State en entidad y HTTP 409 | Order.transitionTo, estados con comportamiento, ConflictException y handler existente. Lock de orden evita doble reintegro simultaneo | Corregido; 22 pruebas de dominio aprobadas con JUnit local. HTTP/concurrencia pendientes de ejecucion |
| JWT y roles | JwtService/Filter, SecurityConfig, BCrypt, @PreAuthorize ADMIN, propiedad por JWT | Existente e inspeccionado; suite de seguridad no ejecutable aqui |
| Mensajeria en un proceso | Checkout escribe outbox; publisher confirma ACK sin NO_ROUTE; consumidor registra evento. DLQ e idempotencia existentes | Corregida confirmacion de envio; broker no ejecutado aqui |
| Primeros tests unitarios | Suite existente y nuevas pruebas de State/ACK/NACK/retorno/timeout | 22 pruebas de dominio aprobadas mediante launcher JUnit; suite completa Maven bloqueada por dependencias/DNS |
| Jira al dia | Tareas y criterios en JIRA.md; no se encontraron IDs/tablero verificables en repo | Pendiente de acceso al tablero real |
| Feedback de diseño incorporado | Corregidas consistencia de State, concurrencia y documentacion. No hay devolucion docente identificable | Revision tecnica realizada; feedback docente pendiente de aportar y contrastar |

No declarar toda la entrega cumplida hasta ejecutar las verificaciones y adjuntar evidencia real.

## Cambios realizados

- La entidad protege sus transiciones y no ofrece setter publico de estado. Se mantienen nombres persistidos, reglas de cancelacion y checkout CONFIRMED: no requiere migracion.
- Lock pesimista por orden serializa cancelaciones; relectura de CANCELLED devuelve conflicto antes de reintegrar stock y escribir otro evento.
- Checkout y devolucion de inventario toman locks de autos por ID ascendente para reducir deadlocks de orden inverso.
- El publisher espera confirmacion correlacionada del broker, verifica retornos y conserva PENDING ante fallos. Configuracion en application.yml.
- ADRs registran decisiones, consecuencias, alternativas y limites. Specification es el nombre preciso del filtrado; @Bean es factory de construccion, no una jerarquia GoF inventada.
- Se agrega smoke reproducible sin dependencias Python externas y prueba real de concurrencia con Testcontainers.

## Ejecucion en una maquina con Docker y acceso a dependencias

Desde la raiz del repositorio (PowerShell):

~~~powershell
Copy-Item .env.example .env
# Editar .env: JWT_SECRET, ADMIN_EMAIL y ADMIN_PASSWORD son obligatorios.
# Generar JWT_SECRET aleatorio (Base64) con PowerShell:
$bytes = New-Object byte[] 48
[System.Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($bytes)
[Convert]::ToBase64String($bytes)
docker compose up -d --build
docker compose ps
~~~

Copiar el secreto generado a .env sin subirlo al repositorio. Usar una base de demo:
el smoke crea usuarios, un auto, una orden cancelada y eventos; conserva esos datos como evidencia.

~~~powershell
$env:ADMIN_EMAIL = 'email-admin-del-entorno'
$env:ADMIN_PASSWORD = Read-Host 'Password admin' -MaskInput
python ci/verify-partial2.py --messaging --report work/partial2-smoke.json
Push-Location backend
mvn -B test
mvn -B verify -Pintegration,coverage
Pop-Location
Push-Location frontend
npm ci
npx playwright install chromium
$env:E2E_API_URL = 'http://localhost:8080'
npm run test:e2e
Pop-Location
~~~

PowerShell 7 permite -MaskInput. Si la base usa otros nombres, pasar --db-user y --db-name al smoke.
Para probar solo API sin Nginx: --base-url http://localhost:8080; esto NO comprueba el proxy del frontend.
Sin --messaging se omite expresamente la verificacion del broker.

La suite existente de integracion incluye ApiContractIT, PersistenceIT y MessagingIT; se suma OrderConcurrencyIT.
Una ejecucion exitosa de unitarias no sustituye integracion ni Playwright.

## Evidencia que conservar

1. SHA del commit publicado y salida docker compose ps, mostrando servicios saludables.
2. work/partial2-smoke.json exitoso con ambos eventos consumidos y respuesta 409.
3. backend/target/surefire-reports y failsafe-reports sin errores.
4. frontend/playwright-report y resultados del flujo visual real.
5. Enlace al tablero Jira con tareas, responsables, estado y evidencia.
6. Devolucion docente original y trazabilidad comentario → cambio → prueba/ADR.

## Limites detectados

- SOAP actual es cotizacion separada; no se integra validacion de certificacion en checkout. La actividad de clase que proponia certificacion asincrona no debe confundirse con codigo implementado; este requisito no forma parte de la lista enviada para esta revision.
- Consumidor actual registra deduplicacion y log. No hay envio de email/postventa implementado.
- Outbox entrega al menos una vez; varias replicas pueden duplicar envios. Un efecto externo futuro debe ser idempotente y coordinado con deduplicacion.
- IdempotencyService existente tiene ventana entre commit de compra y almacenamiento de respuesta: una caida de proceso ahi necesita conciliacion. No prometer exactly once.
- No se reescribe historial ni se rotan credenciales externas. Mantener seguimiento del saneamiento historico indicado en docs/testing.
- No se atribuye feedback docente a mejoras inferidas de una inspeccion tecnica.
