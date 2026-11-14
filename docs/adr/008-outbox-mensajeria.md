# ADR-008: Transactional Outbox, Pub/Sub e Idempotent Consumer

Fecha: 2026-11-14. Estado: documentada para revision del equipo.

## Contexto

No perder la notificacion de una orden si RabbitMQ esta caido ni procesar dos veces un evento reentregado.

## Decision

Escribir orden y OutboxEvent en la misma transaccion PostgreSQL. OutboxPublisher publica mensaje persistente y solo marca SENT despues de ACK del broker sin retorno NO_ROUTE. Se habilitan confirms correlacionados, publisher returns y mandatory. Fallos tecnicos conservan PENDING; JSON corrupto queda FAILED. Consumidor registra eventId mediante insercion atomica; mensajes invalidos pasan a DLQ.

## Evidencia en codigo

backend/src/main/java/com/driveai/messaging/; repository/ProcessedMessageRepository.java; src/main/resources/db/migration/V8__outbox_and_messaging.sql; src/main/resources/application.yml

## Verificacion

OutboxPublisherTest (ACK/NACK/NO_ROUTE/timeout/broker caido); OutboxServiceTest; OrderEventsListenerTest; ProcessedMessageServiceTest; MessagingIT. Las pruebas nuevas requieren ejecutar la suite; su existencia no acredita ejecucion exitosa.

## Consecuencias y alternativas

Entrega al menos una vez, nunca exactly once. El consumidor actual registra procesamiento y log de auditoria; no envia emails. Un efecto futuro debe ser atomico con deduplicacion o idempotente por evento. Espera de confirmacion acotada (5s por evento); varias replicas pueden publicar duplicados admitidos por deduplicacion. Evitar publicar directamente antes del commit de orden. Referencia: https://docs.spring.io/spring-amqp/reference/amqp/template.html#template-confirms
