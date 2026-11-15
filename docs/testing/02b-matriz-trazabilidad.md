# 02b · Matriz de trazabilidad HU → criterio → caso → test → resultado → evidencia → defecto

Entregable 2 de los criterios de la Clase 15. Cada fila es un **criterio de aceptación**
(escenario Gherkin de `docs/historias-gherkin.md`). Ninguna HU ni criterio queda sin caso.

**Referencias de la columna "Test automatizado"** (ruta base `backend/src/test/java/com/driveai/`
y `frontend/e2e/`):
`U` = unitaria JUnit · `MVC` = `controller/ApiSecurityTest` · `IT` = Testcontainers
(`integration/*IT`) · `E2E` = Playwright · `PM nn` = request *nn* de
`postman/DriveAI.postman_collection.json` (Newman en CI).

**Resultado:** ✅ ejecutado y pasado. Unitarias (174), integración (18) y Newman (54) se
ejecutaron el 10/11 en la rama `qa/criterios-clase15` sin fallas; E2E del 8/11 (`06`).
La spec de accesibilidad (7 casos) y toda la suite corrieron en verde en CI sobre `main`
(run <https://github.com/Faustinofff/driveai/actions/runs/42867141379>: unitarias
178/178, IT 18/18, Newman 55/55, Schemathesis 352/352, Playwright 23/23). Run sobre el
**tag** `v1.0-testing`, 9/9 jobs en verde: <https://github.com/Faustinofff/driveai/actions/runs/42897419739>.

**Evidencia:** artefactos de CI — `jacoco-report`, `pit-report`, `failsafe-reports`,
`playwright-report`, `api-contract-reports`, `zap-report`.

**MoSCoW:** propuesta del QA; **validar con el equipo/PO** antes de la entrega.

| HU | MoSCoW | Criterio (escenario) | Tipo | Casos | Test automatizado | Resultado | Defecto |
| --- | --- | --- | --- | --- | --- | --- | --- |
| HU-01 Registro | Must | Registro exitoso | Feliz | CP-01 | U `service/AuthServiceTest#registerCreatesCustomer…` · E2E `auth.spec.ts` · PM 02 | ✅ | — |
| HU-01 | Must | Email ya registrado | Error | CN-02 | U `AuthServiceTest#registerWithDuplicateEmailThrowsConflict` · IT `ApiContractIT#authEndpoints…` · PM 18 | ✅ | — |
| HU-01 | Must | Contraseña demasiado corta | Alternativo/borde | CN-04 | MVC `#invalidBodyReturns400ProblemJson` · PM 19 | ✅ | — |
| HU-02 Sesión | Must | Login exitoso | Feliz | CP-02 | U `AuthServiceTest#loginWithValidCredentialsReturnsToken` · E2E `auth.spec.ts` · PM 03 | ✅ | — |
| HU-02 | Must | Contraseña incorrecta | Error | CN-01 | U `AuthServiceTest#loginWithWrongPassword…` · MVC `#loginBadCredentialsReturns401` · E2E · PM 04 | ✅ | — |
| HU-02 | Must | Demasiados intentos | Error (seguridad) | CN-11 | U `rate/LoginRateLimitServiceTest` (incl. `#rotatingIpsAgainstSameAccount…`) · MVC `#loginRateLimitReturns429WithRetryAfter` | ✅ | **DEF-07** |
| HU-02 | Must | Cierre de sesión | Alternativo | CP-02 | E2E `auth.spec.ts` (registro, logout y login) | ✅ | — |
| HU-03 Catálogo | Must | Listado público | Feliz | CP-03 | MVC `#catalogIsPublic` · E2E `catalog.spec.ts` · PM 05 | ✅ | — |
| HU-03 | Must | Búsqueda por marca | Alternativo | CP-04 | U `specification/CarSpecificationTest#searchTerm…` · E2E `catalog.spec.ts` | ✅ | — |
| HU-03 | Should | Filtros combinados | Alternativo | CB-01..03 | U `CarSpecificationTest#combinedFilters…` | ✅ | — |
| HU-03 | Must | Sin resultados / parámetros inválidos | Error/borde | CB-02 | E2E `catalog.spec.ts#búsqueda sin resultados` · MVC `#invalidQueryParamTypeReturns400`, `#invalidSortPropertyReturns400` | ✅ | **DEF-10** |
| HU-04 Detalle | Must | Abrir detalle | Feliz | CP-05 | MVC `#carDetailReturnsEtag` · E2E `catalog.spec.ts` · PM 06 | ✅ | — |
| HU-04 | Should | Cerrar detalle (botón / Escape) | Alternativo | CP-21 | E2E `catalog.spec.ts#el detalle se cierra con la tecla Escape` | ✅ | — |
| HU-04 | Must | Vehículo inexistente | Error | CN-14 | MVC `#carDetailMissingReturnsProblemJson` · PM 07 | ✅ | — |
| HU-05 Carrito | Must | Agregar un auto | Feliz | CP-09 | U `service/CartServiceTest#addToCartCreatesCartWithPriceSnapshot` · E2E `cart-checkout.spec.ts` · PM 09, 27 | ✅ | — |
| HU-05 | Should | Ajustar cantidades | Alternativo | CP-10 | U `CartServiceTest#updateItemOnlyWorksForOwnedItem`, `#updateItemRejectsQuantityAboveStock` · PM 28, 29, 31 | ✅ | — |
| HU-05 | Must | Checkout anónimo | Error | CN-21 | E2E `cart-checkout.spec.ts#el checkout exige autenticación` · PM 08, 33 | ✅ | — |
| HU-05 | Must | Carrito ajeno (BOLA) | Error (seguridad) | CN-22, CN-23 | U `CartServiceTest#updateItemOfAnotherUserIsNotFound` · IT `ApiContractIT#cartIsIsolatedPerUser…` · PM 30, 32 | ✅ | — |
| HU-06 Checkout | Must | Compra exitosa | Feliz | CP-11 | U `OrderServiceTest#checkoutDeductsStockAndCreatesConfirmedOrder` · E2E `cart-checkout.spec.ts` · PM 11, 35 | ✅ | — |
| HU-06 | Must | Sin stock | Error | CN-28 | U `OrderServiceTest#checkoutThrowsInsufficientStockWithoutPersisting` · MVC `#insufficientStockReturns409WithDetails` | ✅ | — |
| HU-06 | Must | Reintento idempotente | Alternativo | CB-05, CN-31 | U `IdempotencyServiceTest` (10 casos) · IT `ApiContractIT#checkoutIsIdempotent…` · PM 12 | ✅ | — |
| HU-06 | Must | Broker caído al confirmar (externo) | Error externo | CN-39 | U `messaging/OutboxPublisherTest#brokerOutage…` · IT `MessagingIT#brokerOutageKeepsEventPending…` | ✅ | — |
| HU-07 Órdenes | Must | Historial propio | Feliz | CP-12 | U `OrderServiceTest#listForUserReturnsOnlyOwnOrdersNewestFirst` · MVC `#orderListReturnsOnlyOwnOrders` · PM 36, 37 | ✅ | — |
| HU-07 | Must | Orden ajena (BOLA) | Error (seguridad) | CN-25 | U `OrderServiceTest#getForUserWhenOrderBelongsToAnotherUser…` · MVC `#orderDetailOfAnotherUserReturns404` · PM 39 | ✅ | — |
| HU-07 | Must | Cancelación válida | Alternativo | CP-13, CP-14 | U `OrderServiceTest#cancellingOrderRestoresStock`, `#adminCancelsOrderOfAnotherUser` · PM 14, 42 | ✅ | — |
| HU-07 | Must | Transición inválida → 409 | Error | CN-26, CN-27 | U `domain/enums/OrderStatusTest#transitionMatrix` (9 combinaciones) · U `OrderServiceTest#everyInvalidTransitionIsRejectedWithConflict` (6 parametrizados) · MVC `#invalidTransitionReturns409` · PM 15, 40, 43 | ✅ | — |
| HU-08 Asesor IA | Should | Recomendación | Feliz | CP-15 | U `GroqAdvisorServiceTest#filtersHallucinatedIds…` · U `ai/HttpGroqApiClientTest#returnsModelContent` · E2E `ai-advisor.spec.ts` · PM 51 | ✅ | D-02 |
| HU-08 | Should | Proveedor no disponible (externo) | Error externo | CN-33, CN-34 | U `HttpGroqApiClientTest#timeoutIsReportedAsNoResponse`, `#maps5xx…` · MVC `#groqFailureReturns503` · PM 51 | ✅ | — |
| HU-08 | Should | Prompt injection | Error (seguridad IA) | CN-36 | E2E `ai-advisor.spec.ts#prompt injection no revela…` | ✅ | — |
| HU-08 | Should | Rate limit | Error | CN-37 | U `rate/RateLimitServiceTest` · U `controller/AiAdvisorControllerTest` · MVC `#aiRateLimitKeyIsBoundToAuthenticatedUser…` | ✅ | **DEF-08** |
| HU-09 Admin | Must | Acceso anónimo | Error | CN-15 | MVC `#anonymousCannotCreateCar` · E2E `admin-access.spec.ts` | ✅ | — |
| HU-09 | Must | Acceso de CUSTOMER (BFLA) | Error (seguridad) | CN-13, CN-16 | MVC `#customerCannotCreateCar` · E2E `admin-access.spec.ts` · PM 16, 25, 46 | ✅ | — |
| HU-09 | Must | Alta de auto | Feliz | CP-06 | MVC `#adminCreatesCarWithEtag` · U `CarServiceTest#createPersistsANewCar` · E2E admin · PM 21, 22 | ✅ | — |
| HU-09 | Must | Edición con concurrencia (ETag → 412) | Alternativo/error | CP-07, CN-45 | U `CarServiceTest#updateWithStaleIfMatch…` · MVC `#staleIfMatchReturnsPreconditionFailed` · IT `ApiContractIT#adminCarCrud…` · PM 23, 24 | ✅ | — |
| HU-09 | Must | Eliminación (y auto en uso → 409) | Feliz + error | CP-08, CN-46 | U `CarServiceTest#delete*` (incl. `#deleteWhenFkViolationSurfacesOnFlush…`) · MVC `#adminDeletesCar` · PM 44, 47, 48 | ✅ | **DEF-09** |
| HU-10 Seguridad | Must | Token manipulado | Error | CN-06 | U `security/JwtServiceTest#extractEmailReturnsNullForTamperedToken` · MVC `#invalidTokenIsRejected` | ✅ | — |
| HU-10 | Must | Token expirado | Error | CN-05 | U `JwtServiceTest#expiredTokenIsRejected` | ✅ | — |
| HU-10 | Must | Algoritmo none | Error | CN-08 | U `JwtServiceTest#unsignedAlgNoneTokenIsRejected` | ✅ | — |
| HU-10 | Must | Escalada de privilegios / mass assignment | Error | CN-10, CN-13 | MVC `#tokenWithUnknownRoleIsRejected` · IT `ApiContractIT#adminCarCrudEnforcesEtagAndIgnoresMassAssignment` | ✅ | — |
| HU-11 Resiliencia | Must | Broker caído | Error externo | CN-39 | U `OutboxPublisherTest#brokerOutage*` · IT `MessagingIT#brokerOutageKeepsEventPendingAndRecovers…` | ✅ | — |
| HU-11 | Must | Mensaje duplicado | Alternativo | CN-40 | U `OrderEventsListenerTest#duplicateEventIsIgnored…` · IT `MessagingIT#duplicateEventIsProcessedOnlyOnce` | ✅ | — |
| HU-11 | Must | Mensaje inválido → DLQ | Error | CN-41 | U `OrderEventsListenerTest#*GoesToDlq` · IT `MessagingIT#invalidMessageIsRoutedToDeadLetterQueue` | ✅ | — |
| HU-11 | Must | SOAP con Fault de negocio | Error externo | CN-43 | U `soap/SoapVehicleClientTest#businessFaultIsMappedToConflictException` · MVC `#soapBusinessFaultReturns409` · PM 53 | ✅ | — |
| HU-11 | Must | SOAP con fallo técnico / timeout 5 s | Error externo | CN-44, CB-07 | U `SoapVehicleClientTest#timeoutIsTechnicalFailure`, `#technicalFallbackReturnsPublishedPriceMessage`, `#xxePayloadIsRejected` · PM 52 | ✅ | — |
| HU-12 Responsive | Should | Mobile 360 px | Feliz | CP-23 | E2E `responsive.spec.ts` · E2E `accessibility.spec.ts` (360 px) | ✅ (a11y ✅ CI) | DEF-12 (cerrado) |
| HU-12 | Should | Tablet 768 px | Alternativo | CP-23 | E2E `responsive.spec.ts` | ✅ | — |
| HU-12 | Should | Desktop 1280 px | Alternativo | CP-23 | E2E `responsive.spec.ts` · E2E `accessibility.spec.ts` (1280 px) | ✅ (a11y ✅ CI) | DEF-12 (cerrado) |

## Control de huecos

### Regresiones del cierre final (14/11/2026)

Se conserva la evidencia histórica de arriba. Estas filas agregan trazabilidad de
las correcciones y comprobaciones nuevas; los resultados se acreditan con las
corridas del PR de cierre, sin cambiar las fechas de las ejecuciones anteriores.

| HU | Jira | Criterio agregado/completado | Prueba o comprobación |
|---|---|---|---|
| HU-06 | KAN-22 | Si falla una línea posterior, rollback de stock previo, versión, orden y outbox; carrito conservado | IT `OrderConcurrencyIT#failedLaterLineRollsBackEarlierStockAndPreservesCart` |
| HU-06 | KAN-22 | Dos compradores disputan la última unidad; solo uno compra | IT `OrderConcurrencyIT#twoBuyersCannotPurchaseTheSameLastUnit` |
| HU-07 | KAN-21 | State protegido desde la entidad, matriz completa y cancelación concurrente sin doble reintegro | U `OrderStateTest`; IT `OrderConcurrencyIT#simultaneousCancellationsRestoreStockAndRecordEventOnlyOnce` |
| HU-10 | KAN-35 | Payloads SQL no alteran ni amplían catálogo; integridad posterior | IT `ApiContractIT#sqlPayloadsAreTreatedAsDataAndDoNotBroadenQueries` |
| HU-09/10 | KAN-35 | HTML persistido es JSON literal y no permite campos administrativos | IT `ApiContractIT#storedHtmlRemainsJsonDataWithoutMassAssignedFields` |
| HU-08/10 | KAN-35 | Payload XSS del catálogo y del proveedor se muestra como texto, sin nodos ejecutables ni efecto en window | E2E `security-xss.spec.ts` (proveedor simulado solo para renderer IA) |
| HU-10 | KAN-33 | Problem Details de seguridad contiene bytes JSON UTF-8 válidos | MVC `ApiSecurityTest#securityErrorsContainValidUtf8Json` |
| HU-11 | KAN-24/25 | ACK/retorno/timeout y dos eventos realmente consumidos | U `OutboxPublisherTest`; IT `MessagingIT`; `ci/verify-partial2.py --messaging` |
| HU-12 | KAN-41 | Sin hallazgos critical/serious en axe; score >=90 en home/catálogo/login mobile y desktop | E2E `accessibility.spec.ts`; `ci/check-lighthouse.py` y seis JSON Lighthouse |
| HU-01/02/04 | KAN-45 | 50 VUs durante 5 min, p95 <500 ms y errores <1% | `load-tests/k6.js`; artifact `final-qa-evidence/k6-summary.json` |
| HU-04/06 | KAN-46 | Instrucciones del manual: detalle, carrito, cantidades, eliminación y confirmación | E2E `cart-checkout.spec.ts`; smoke Compose |
| HU-08 | KAN-42/43 | Consultas normales, límite, IDs inventados, prompt injection, datos/claves y fuera de dominio contra Groq real | `ci/verify-live-ai.py`; ejecución manual requiere clave; NO se acredita mediante mocks |

Evidencia reproducible: `.github/workflows/ci.yml`, `delivery-smoke.yml` y
`final-validation.yml`. El registro de cierre distingue comprobaciones aprobadas,
pendientes de credenciales y la aceptación humana del equipo.

- 12 HU, 47 criterios: todos con al menos un caso y al menos un test automatizado.
- HU-04 tenía solo 2 criterios (faltaba el de error) y HU-06 no tenía el criterio de falla
  del sistema externo: se agregaron en `historias-gherkin.md` en esta revisión.
- Criterios **Must** con defecto: DEF-07 corregido y verificado en unitarias; DEF-09
  corregido y verificado contra PostgreSQL real (Newman PM 44). Al cierre no puede quedar ningún criterio Must fallando.
