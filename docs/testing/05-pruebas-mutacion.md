# 05 · Pruebas de mutación (eje C)

## 1. Objetivo

Medir la **calidad de la suite**, no solo su cobertura. PIT introduce mutaciones
sintéticas en el código de producción y verifica que al menos una prueba falle
(mate la mutación). Una mutación que sobrevive indica una aserción faltante o débil.

## 2. Herramienta y configuración

- **PIT 1.15.8** + `pitest-junit5-plugin` 1.2.1.
- Clases objetivo: `com.driveai.service.*`, `com.driveai.domain.enums.*`,
  `com.driveai.rate.*`, `com.driveai.security.*`.
- Se excluyen las pruebas de integración (`com.driveai.integration.*`) para no requerir
  Docker dentro de los minions de PIT.
- Umbrales en `backend/pom.xml`: `mutationThreshold=60`, `coverageThreshold=60`.

## 3. Comando

```bash
cd backend
mvn -B pitest:mutationCoverage
```

Reporte HTML: `backend/target/pit-reports/index.html`
Reporte XML: `backend/target/pit-reports/mutations.xml`

## 4. Resultado real de la ejecución local

```
Generated 219 mutations
Killed 161 (74 %)
Mutations with no coverage 12
Test strength 78 %
Line Coverage (for mutated classes only): 479/512 (94 %)
BUILD SUCCESS
```

| Métrica | Valor | Umbral | Estado |
| --- | --- | --- | --- |
| Mutation score | **73,5 %** (161/219) | ≥ 60 % | ✅ |
| Test strength | **78 %** | — | ✅ |
| Cobertura de clases mutadas | **94 %** | ≥ 60 % | ✅ |
| Mutaciones sin cobertura | 12 | — | Aceptable |

> El *mutation score* cuenta las mutaciones no cubiertas como no matadas; el *test
> strength* las excluye. Ambos se informan para transparencia.

### Detalle por clase

| Clase | No matadas / total |
| --- | --- |
| `CartService` | 5 / 31 |
| `CarService` | 11 / 30 |
| `GroqAdvisorService` | 6 / 27 |
| `OrderService` | 4 / 25 |
| `IdempotencyService` | 6 / 21 |
| `SecurityConfig` | 13 / 17 |
| `JwtService` | 2 / 16 |
| `JwtAuthenticationFilter` | 1 / 13 |
| `LoginRateLimitService` | 3 / 11 |
| `AuthService` | 1 / 10 |
| `OrderStatus` | 0 / 9 |
| `RateLimitService` | 5 / 8 |
| `UserPrincipal` | 1 / 1 |

### Mutadores que más aportaron

| Mutador | Generadas | Matadas |
| --- | --- | --- |
| `NegateConditionalsMutator` | 74 | 70 (95 %) |
| `ConditionalsBoundaryMutator` | 61 | 35 (57 %) |
| `VoidMethodCallMutator` | 44 | 33 (75 %) |
| `EmptyObjectReturnValsMutator` | 12 | 9 (75 %) |
| `PrimitiveReturnsMutator` | 10 | 1 (10 %) |
| `NullReturnValsMutator` | 8 | 6 (75 %) |
| `BooleanTrueReturnValsMutator` | 6 | 4 (67 %) |
| `BooleanFalseReturnValsMutator` | 2 | 2 (100 %) |
| `MathMutator` | 2 | 1 (50 %) |

## 5. Interpretación de sobrevivientes

- **`SecurityConfig` (13/17)**: son mutaciones de expresiones declarativas de
  autorización (`requestMatchers(...).permitAll()` / `.authenticated()`). Muchas no
  cambian el resultado observable para los endpoints ejercitados, y se validan mejor con
  pruebas de integración reales (`ApiContractIT`). Se documenta como deuda, no se oculta.
- **`CarService` (11/30)**: mutaciones en ramas de ordenamiento/paginación y en el
  mensaje de error de borrado con ventas.
- **`GroqAdvisorService` (6/27)**: ramas defensivas de parseo de JSON y filtrado de IDs.
- **`RateLimitService` (5/8)**: mutaciones en el cálculo de la ventana temporal.
- El resto de las clases críticas (checkout, idempotencia, JWT, estados) tiene scores
  altos, lo que respalda la sensibilidad de la suite ante regresiones.

## 6. Conclusión

La suite **supera el umbral de 60 %** con 73,5 % y una cobertura de 94 % sobre las clases
mutadas. Las mutaciones sobrevivientes están identificadas y justificadas; no se
declaran como aprobadas ni se eliminan del reporte.
