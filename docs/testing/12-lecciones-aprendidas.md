# 12 · Lecciones aprendidas

Entregable 8: tres defectos que llegaron tarde, por qué no se detectaron antes y qué
prueba los habría atrapado. Los tres se encontraron en la revisión del 10/11/2026
contra los criterios de la Clase 15, después de que la suite ya estaba "en verde".

> Borrador del QA: el equipo debe revisar y completar el "por qué" con su propia
> experiencia antes de la defensa.

## 1. DEF-09 — Borrar un auto en uso devolvía 500

- **Por qué no se detectó:** el test unitario simulaba que `carRepository.delete()`
  lanzaba la violación de FK. JPA no se comporta así: el `DELETE` sale recién al
  hacer flush en el commit. El mock reflejaba lo que suponíamos, no lo que hace la
  librería, y el test pasaba aunque el código no funcionara.
- **Qué prueba lo habría atrapado:** una prueba de integración con PostgreSQL real
  (Testcontainers) que borre un auto referenciado por una orden y espere 409, o el
  request de Newman PM 44 contra el stack levantado.
- **Lección:** no mockear el comportamiento de una dependencia que no controlamos
  cuando lo que importa es justamente ese comportamiento (transacciones, flush,
  constraints). Ese caso va en integración.

## 2. DEF-07 — El rate limit de login se evadía rotando la IP

- **Por qué no se detectó:** los tests probaban el límite desde una IP fija. Diseñamos
  el control pensando en el uso normal, no en cómo lo atacaría alguien. Además, la IP
  salía de `X-Forwarded-For`, un header que controla el cliente.
- **Qué prueba lo habría atrapado:** un caso negativo que mande N+1 intentos contra la
  misma cuenta cambiando `X-Forwarded-For` en cada uno, en el plan de seguridad.
- **Lección:** cada control de seguridad necesita al menos un test escrito "como
  atacante": ¿qué dato de la request puedo cambiar para que el control no me reconozca?

## 3. DEF-10 — Errores del cliente respondidos como 500

- **Por qué no se detectó:** los casos negativos eran los que se nos ocurrían (body
  inválido, tipo incorrecto). Nadie probó un `sort` inexistente, un método HTTP no
  soportado o un `Content-Type` incorrecto. El handler genérico `Exception.class`
  capturaba todo y lo convertía en 500.
- **Qué prueba lo habría atrapado:** fuzzing de la API contra el OpenAPI
  (Schemathesis con el check `not_a_server_error`), que genera esas entradas solo.
- **Lección:** los casos negativos escritos a mano no alcanzan. Una herramienta que
  genere entradas a partir del contrato encuentra los huecos que no imaginamos.

## Acciones para la 4.ª entrega

1. Schemathesis y ZAP corren en cada push (ya agregados al pipeline).
2. Para cada control de seguridad nuevo, el PR incluye un test "como atacante".
3. Los tests que dependen de transacciones o de constraints se escriben como IT.
