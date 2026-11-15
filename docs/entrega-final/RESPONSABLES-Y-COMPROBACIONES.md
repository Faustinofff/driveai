# Cierre de validacion y acciones del equipo

Fecha: 14/11/2026. Base: main despues del PR #3 (8bb5df6).
Las nuevas comprobaciones se revisan en el PR #4 y sus Actions. Una prueba escrita
no se declara aprobada hasta tener una corrida exitosa.

## Comprobaciones preparadas y ejecutables sin accesos nuevos

| Jira | Trabajo | Evidencia reproducible |
|---|---|---|
| KAN-12 | Matriz ampliada con regresiones de State, concurrencia, seguridad y despliegue | docs/testing/02b-matriz-trazabilidad.md |
| KAN-22 | Rollback multiitem y dos compradores por la ultima unidad | OrderConcurrencyIT (PostgreSQL real) |
| KAN-35 | Payloads SQL, mass assignment y HTML almacenado/IA renderizado como texto | ApiContractIT; security-xss.spec.ts |
| KAN-41 | Axe bloqueante para critical/serious; movimiento reducido; Lighthouse >=90 | accessibility.spec.ts; seis reportes Lighthouse |
| KAN-45 | 50 VUs / 5 minutos por Nginx, p95 <500 ms, errores <1% | final-validation.yml; k6-summary.json por commit |
| KAN-46 | Manual contrastado con comandos reales y recorridos UI automatizados | manual-usuario.md; catalog/auth/admin/cart E2E; smoke Compose |

La carga usa una cuenta sintetica compartida por los 50 VUs, con carrito preparado
antes de medir. Se ejecuta en un runner CI: no garantiza igual rendimiento en otro hosting.
Lighthouse mide home, catalogo y login en mobile y desktop; no representa una certificacion
completa de accesibilidad. Los payloads de seguridad son una bateria acotada, no una auditoria exhaustiva.

## Fausti — responsable del repositorio y servicios externos

1. Revisar e integrar el PR #4 una vez que las verificaciones esten verdes.
2. SonarCloud: completar `SONAR_TOKEN` (secret) y `SONAR_ORGANIZATION` /
   `SONAR_PROJECT_KEY` (variables) en GitHub Settings → Secrets and variables → Actions.
   Las instrucciones completas estan en docs/testing/13-configurar-sonarcloud.md.
   Ejecutar el workflow principal y revisar Quality Gate / Security Hotspots en SonarCloud.
3. Groq real: configurar `GROQ_API_KEY` como secret del repositorio, sin pegar claves
   en chats, documentos, commits ni Jira. En Actions → QA - cierre final desplegado →
   Run workflow, elegir la rama integrada y activar `live_ai`.
4. Conservar el artifact `final-qa-evidence`. El resultado de IA real debe contener
   seis casos aprobados; un fallback 503 no cuenta como evaluacion del modelo.
5. Si la catedra exige una URL publica permanente, elegir el hosting y configurar
   HTTPS, datos y secretos de ese entorno. El despliegue efimero de CI no proporciona esa URL.

Sin credenciales y ejecucion efectiva, KAN-31 / KAN-42 / KAN-43 permanecen pendientes.
La bateria IA no guarda respuestas crudas ni secretos en sus reportes; sigue siendo
necesaria la revision funcional de la calidad de recomendaciones por el equipo.

## Valen — responsable del tablero y coordinacion

1. Validar responsables reales y criterios de cierre del equipo en Jira; no se asignan
   personas por suposicion ni se inventan aprobaciones.
2. Tras integrar el PR, cerrar las tareas cuya descripcion enlace evidencia aprobada.
   Mantener abiertas las que dependan de SonarCloud o de pruebas Groq reales.
3. Confirmar con el equipo si hubo feedback docente. Si no hubo, registrar
   "sin devolucion recibida"; si aparece, vincular observacion → cambio → verificacion en KAN-48.
4. Contrastar la consigna final con el alcance implementado. RabbitMQ usa AMQP;
   no se debe presentar como una implementacion JMS si se exigiera JMS expresamente.

## Equipo — defensa

Ensayar registro/login, catalogo, detalle, carrito, checkout, cancelacion 409 y lectura
de eventos con una base de demo. Poder explicar Controller → Service → Repository,
State, JWT/roles, transaccion y outbox. La verificacion tecnica del manual no sustituye
el ensayo ni constituye una prueba de aceptacion realizada por personas.
