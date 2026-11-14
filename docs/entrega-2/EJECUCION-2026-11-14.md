# Registro de verificacion — 14/11/2026

Base: e0551e9f3811cfb299925ad5a3a87627cceb0452. Correcciones en fix/entrega-2-core.

Actualizacion de acceso: despues de autorizar la app en la cuenta propietaria del
repositorio, la creacion de fix/entrega-2-core fue exitosa. Los errores de GitHub
indicados abajo corresponden al intento inicial; ya no bloquean la publicacion.
Las pruebas de CI deben consultarse en el PR y no se dan por aprobadas anticipadamente.

## Evidencia obtenida en GitHub Actions

PR: https://github.com/Faustinofff/driveai/pull/3

Primera corrida: https://github.com/Faustinofff/driveai/actions/runs/43620190567
Commit probado: b7f377901d3a84ba68c93ef63f0b0a3703dd3ae2.

- Backend: 192 pruebas, 0 fallos, 0 errores y 0 omitidas; umbrales JaCoCo cumplidos y SpotBugs exitoso.
- Suite repetida tres veces: 192 pruebas exitosas en cada corrida.
- Frontend: build de produccion exitoso y 23 pruebas Playwright aprobadas.
- API: Newman y contrato OpenAPI exitosos.
- Seguridad: Gitleaks, Trivy y ZAP exitosos.
- SonarCloud: job exitoso pero analisis omitido por falta de configuracion; no equivale a un Quality Gate evaluado.

- Integracion Testcontainers: 19 pruebas aprobadas, incluida cancelacion concurrente y recuperacion del broker.
- Mutacion PIT: 222 mutaciones generadas, 164 eliminadas (74%); build exitoso.

La primera corrida dejo contextos Spring en cache despues de detener los contenedores,
con reconexiones de listeners y scheduler al terminar. Se agrega @DirtiesContext(AFTER_CLASS)
en las cuatro suites de integracion para cerrar sus recursos al terminar cada clase.
El nuevo flujo Compose y esta limpieza deben contrastarse con su propia corrida final.

La primera ejecucion Compose (43620579700) construyo y levanto las cinco capas, pero
detecto JSON no UTF-8 en la respuesta 401 del filtro de seguridad. Se corrige
SecurityConfig para escribir bytes UTF-8 y se agrega una regresion que parsea esos
bytes con Jackson. El smoke conserva su validacion estricta; no se relaja el cliente.
Los bloqueos locales de las secciones siguientes quedan como registro historico y no invalidan la evidencia remota.

## Comprobaciones aprobadas

- Compilacion real con JDK 17 de OrderStatus, Order, OrderItem, Car, enums y ConflictException, procesando Lombok con la biblioteca disponible localmente.
- Ejecucion JUnit de los archivos reales OrderStatusTest y OrderStateTest: **22 tests encontrados, 22 iniciados, 22 exitosos, 0 fallidos**. Incluye las 9 transiciones en el enum y las 9 sobre la entidad, y rechazo de null.
- Harness adicional del enum: 15 comprobaciones exitosas (9 transiciones, 3 null, 3 terminales).
- Sintaxis Python: python -m py_compile ci/verify-partial2.py; ayuda de CLI correcta.
- Recuperacion del snapshot: los 217 blobs originales se contrastaron con los SHA de GitHub, para evitar introducir diferencias en archivos no modificados.

## Alcance de la ejecucion JUnit local

Se uso LauncherFactory con JUnit Jupiter 5.10.3 / Platform 1.10.3, AssertJ 3.25.3,
Lombok 1.18.34 y Jakarta Persistence 3.1.0 presentes en cache. Se compilaron los
archivos del proyecto sin reemplazar clases ni introducir stubs. No se cambio pom.xml.
Esta ejecucion aislada NO es una ejecucion Maven del proyecto ni verifica la resolucion
exacta de todas sus dependencias, el contexto Spring, la API HTTP o el esquema real.

## Verificaciones bloqueadas

| Accion | Resultado observado | Consecuencia |
|---|---|---|
| mvn -B -o test | jacoco-maven-plugin:0.8.12 no disponible en cache | No se ejecuto la suite Maven |
| mvn -B test con repositorio temporal | DNS de repo.maven.apache.org falla | No se pueden descargar las dependencias faltantes |
| npm ci --offline | ENOTCACHED para zustand-4.5.7.tgz | No se construyo ni ejecuto el frontend |
| Docker al inicio | Pipe dockerDesktopLinuxEngine inexistente | No se ejecutaron Testcontainers, despliegue ni smoke contra stack |
| Jira | Error de integracion: Unknown tool, incluso tras informar autorizacion | No se leyo ni actualizo el tablero |
| GitHub | permissions.push=false | No se publicaron commits ni PR en origen |

Se solicito iniciar Docker Desktop para intentar disponer del motor sin modificar su configuracion.
Si sigue inaccesible, completar los comandos de VERIFICACION.md en un entorno operativo.
No se generaron resultados ficticios ni se reutilizaron porcentajes historicos como evidencia de estos cambios.

## Pruebas nuevas pendientes de ejecucion completa

- OrderServiceTest: locks de inventario en orden ascendente y cambios de estado usando consulta con lock.
- OutboxPublisherTest: ACK, NACK, mensaje sin ruta y timeout; conservacion de PENDING ante fallos.
- OrderConcurrencyIT: dos cancelaciones concurrentes, un reintegro de stock y un evento.
- ci/verify-partial2.py --messaging: flujo por Nginx y comprobacion de dos eventos consumidos de la misma orden.

## Cierre

El nucleo tiene correcciones concretas y documentacion trazable. El cumplimiento integral queda pendiente
de publicacion, ejecucion del stack/suite, acceso efectivo a Jira y contraste del feedback docente original.
