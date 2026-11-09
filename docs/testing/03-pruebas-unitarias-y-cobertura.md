# 03 · Pruebas unitarias y cobertura (eje A)

## 1. Herramientas

- **JUnit 5** + **Mockito** para lógica de servicios, seguridad, mensajería y SOAP.
- **Spring Boot Test** + **`@WebMvcTest`** para controladores y contrato HTTP.
- **WireMock** para simular Groq y el proveedor SOAP sin red externa.
- **JaCoCo 0.8.12** para cobertura de líneas y ramas.
- **AssertJ** para aserciones legibles.

## 2. Comando de ejecución

```bash
cd backend
mvn -B test jacoco:report
```

Reporte HTML: `backend/target/site/jacoco/index.html`
Reporte XML (usado por CI): `backend/target/site/jacoco/jacoco.xml`

## 3. Resultado real de la ejecución local

```
Tests run: 148, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

| Métrica | Cubierto | Total | Porcentaje |
| --- | --- | --- | --- |
| Instrucciones | 3.637 | 3.932 | **92,50 %** |
| **Líneas** | **806** | **859** | **93,83 %** |
| **Ramas** | **213** | **269** | **79,18 %** |
| Complejidad | 251 | 321 | 78,19 % |
| Métodos | 169 | 186 | 90,86 % |
| Clases | 44 | 44 | 100,00 % |

> Umbrales exigidos: líneas ≥ 80 % y ramas ≥ 70 %. Ambos se superan.
> El perfil `coverage` (`mvn -B verify -Pcoverage`) hace fallar el build si no se cumplen.

### Cobertura por paquete

| Paquete | Líneas | Ramas |
| --- | --- | --- |
| `com.driveai.service` | 305/329 (92,7 %) | 88/102 (86,3 %) |
| `com.driveai.security` | 120/123 (97,6 %) | 21/30 (70,0 %) |
| `com.driveai.messaging` | 79/84 (94,0 %) | 28/36 (77,8 %) |
| `com.driveai.soap` | 75/76 (98,7 %) | 18/26 (69,2 %) |
| `com.driveai.ai` | 47/47 (100 %) | 12/14 (85,7 %) |
| `com.driveai.advice` | 39/42 (92,9 %) | 2/2 (100 %) |
| `com.driveai.rate` | 36/42 (85,7 %) | 10/12 (83,3 %) |
| `com.driveai.controller` | 31/42 (73,8 %) | 6/18 (33,3 %) |
| `com.driveai.specification` | 25/25 (100 %) | 14/14 (100 %) |
| `com.driveai.domain.enums` | 27/27 (100 %) | 14/15 (93,3 %) |
| `com.driveai.exception` | 22/22 (100 %) | — |

> `com.driveai.config`, `domain.model`, `dto` y `DriveaiApplication` se excluyen de
> JaCoCo por ser configuración/entidades/records sin lógica propia.

## 4. Inventario de pruebas unitarias

| Clase | Tests | Qué cubre |
| --- | --- | --- |
| `ApiSecurityTest` | 30 | Códigos HTTP, Problem Details, 401/403/404/409/412/429, cabeceras y contrato MVC |
| `CartServiceTest` | 12 | Alta/actualización/eliminación, stock, propiedad por `userId`, 404 BOLA |
| `OrderServiceTest` | 11 | Checkout, stock, idempotencia, transiciones, cancelación y rollback |
| `IdempotencyServiceTest` | 10 | Replay, concurrencia, conflicto de payload y liberación ante fallo |
| `GroqAdvisorServiceTest` | 10 | Recomendaciones, anti-alucinación, timeout, JSON inválido y fallback |
| `SoapVehicleClientTest` | 9 | Éxito, fault de negocio, fault técnico, timeout, XXE y fallback |
| `HttpGroqApiClientTest` | 9 | Cliente HTTP, timeouts, errores 4xx/5xx y parseo |
| `CarServiceTest` | 8 | CRUD, filtros, ETag/versión y borrado con ventas |
| `JwtServiceTest` | 7 | Firma, expiración, issuer, `alg=none`, secreto corto |
| `AuthServiceTest` | 7 | Registro, login, duplicados y creación de admin |
| `LoginRateLimitServiceTest` | 6 | Límite por IP, ventana y `Retry-After` |
| `OrderEventsListenerTest` | 6 | Consumo idempotente y errores |
| `OutboxPublisherTest` | 5 | Publicación, reintento y broker caído |
| `CarSpecificationTest` | 4 | Filtros combinados y búsqueda |
| `WsdlContractTest` | 3 | Contrato WSDL del mock SOAP |
| `OrderStatusTest` | 3 | Máquina de estados |
| `ProcessedMessageServiceTest` | 3 | Deduplicación |
| `MessagingConfigTest` | 2 | Topología y DLQ |
| `RateLimitServiceTest` | 2 | Límite de IA |
| `OutboxServiceTest` | 1 | Persistencia del evento en la transacción |
| **Total** | **148** | |

## 5. Pruebas negativas y de borde

La suite incluye, entre otras:

- JWT manipulado, expirado, con issuer ajeno y con `alg=none`.
- BOLA/IDOR en carrito y órdenes (usuario B sobre recursos de A).
- BFLA: CUSTOMER intentando operaciones de ADMIN.
- Mass assignment: `id`, `version`, `role` en el body no tienen efecto.
- Stock insuficiente, cantidades inválidas, transiciones de estado inválidas.
- Timeout, fault técnico, fault de negocio y XXE en SOAP.
- Broker caído, mensaje duplicado y mensaje inválido en mensajería.
- Rate limit de login e IA.

Esto sostiene el requisito de **≥ 40 % de casos negativos/borde** (ver `02-casos-de-prueba.md`).

## 6. Observaciones

- El paquete `controller` tiene la cobertura de ramas más baja (33 %) porque las ramas
  restantes pertenecen a anotaciones/expresiones de seguridad declarativas que se
  validan mejor en integración. No afecta los umbrales globales.
- La cobertura global se mantiene por encima de los umbrales aun excluyendo
  configuración, DTOs y entidades.
- Ejecución repetida 3 veces consecutivas para detectar flakiness: ver
  `06-pruebas-e2e.md` §6.
