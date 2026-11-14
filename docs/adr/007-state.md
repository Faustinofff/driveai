# ADR-007: State en Order y exclusión mutua

Fecha: 2026-11-14. Estado: documentada para revision del equipo.

## Contexto

Una orden cancelada no puede volver a confirmarse ni devolver stock dos veces si llegan solicitudes simultaneas.

## Decision

Cada constante OrderStatus implementa canTransitionTo. Order.transitionTo delega en ese State y arroja ConflictException si no se permite. Se elimina el setter publico del estado. OrderService autoriza al usuario, bloquea la fila de orden con PESSIMISTIC_WRITE y ejecuta transicion, stock y outbox en la misma transaccion.

## Evidencia en codigo

backend/src/main/java/com/driveai/domain/enums/OrderStatus.java; domain/model/Order.java; service/OrderService.java; repository/OrderRepository.java; advice/GlobalExceptionHandler.java

## Verificacion

OrderStateTest (9 combinaciones y null); OrderStatusTest; OrderServiceTest; ApiContractIT (HTTP 409); OrderConcurrencyIT (dos cancelaciones, un reintegro). Las pruebas nuevas requieren ejecutar la suite; su existencia no acredita ejecucion exitosa.

## Consecuencias y alternativas

Se mantienen PENDING → CONFIRMED/CANCELLED y CONFIRMED → CANCELLED; CANCELLED terminal. Checkout sigue creando CONFIRMED. Permisos se validan antes del estado: 401/403/404 conservan prioridad. El lock dura hasta commit; autos se bloquean por ID ascendente. Builder/JPA se reservan para creacion e hidratacion. Alternativas: switch en servicio deja entidad anemica; clases por estado son innecesarias para tres estados sin datos propios; version optimista obligaria rollback/reintento de stock.
