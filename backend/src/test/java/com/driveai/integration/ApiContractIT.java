package com.driveai.integration;

import io.restassured.RestAssured;
import io.restassured.config.DecoderConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.http.ContentType;
import io.restassured.path.json.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Pruebas de API y contrato contra la aplicación completa con PostgreSQL y
 * RabbitMQ reales.
 *
 * <p>Valida:
 * <ul>
 *   <li>El documento OpenAPI publicado y sus esquemas.</li>
 *   <li>Casos exitosos y de error de los endpoints principales (REST Assured).</li>
 *   <li>Autorización (401/403), BOLA/IDOR, mass assignment, ETag/412.</li>
 *   <li>Idempotency-Key y transiciones inválidas de estado (409).</li>
 * </ul>
 *
 * Ejecutar con {@code mvn verify -Pintegration} (requiere Docker).
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "app.admin.email=admin-it@driveai.test",
                "app.admin.password=AdminItPassw0rd!",
                "app.jwt.secret=integration-test-only-jwt-secret-not-a-real-secret-00000000000000",
                "app.outbox.publish-interval-ms=3600000"
        })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Testcontainers
class ApiContractIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Container
    @ServiceConnection
    static RabbitMQContainer rabbit = new RabbitMQContainer("rabbitmq:3.13-management-alpine");

    @LocalServerPort
    private int port;

    @BeforeEach
    void setUp() {
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = port;
        // Los Problem Details se serializan en UTF-8 sin charset explícito; sin
        // esto REST Assured decodifica como ISO-8859-1 y rompe tildes/ñ.
        RestAssured.config = RestAssuredConfig.config().decoderConfig(
                DecoderConfig.decoderConfig().defaultContentCharset(java.nio.charset.StandardCharsets.UTF_8));
    }

    private String uniqueEmail(String prefix) {
        return prefix + "-" + UUID.randomUUID() + "@driveai.test";
    }

    private JsonPath register(String email) {
        return given()
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "email", email,
                        "password", "IntegrationPassw0rd!",
                        "fullName", "Integration User"))
                .post("/api/auth/register")
                .then()
                .statusCode(201)
                .body("token", not(emptyString()))
                .extract()
                .jsonPath();
    }

    private String adminToken() {
        return given()
                .contentType(ContentType.JSON)
                .body(Map.of("email", "admin-it@driveai.test", "password", "AdminItPassw0rd!"))
                .post("/api/auth/login")
                .then()
                .statusCode(200)
                .extract()
                .path("token");
    }

    private Map<String, Object> carPayload(String model) {
        Map<String, Object> body = new HashMap<>();
        body.put("brand", "ContractBrand");
        body.put("model", model);
        body.put("year", 2024);
        body.put("price", 21999.99);
        body.put("mileage", 500);
        body.put("fuelType", "GASOLINA");
        body.put("fuelConsumption", 5.8);
        body.put("transmission", "AUTOMATICA");
        body.put("bodyType", "SEDAN");
        body.put("imageUrl", "https://img.test/contract.jpg");
        body.put("description", "Vehículo de prueba de contrato");
        body.put("stock", 2);
        return body;
    }

    @Test
    void openApiDocumentExposesAllCriticalPathsAndSchemas() {
        JsonPath document = given()
                .get("/v3/api-docs")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .extract()
                .jsonPath();

        Map<String, Object> paths = document.getMap("paths");
        assertThat(paths).containsKeys(
                "/api/auth/register",
                "/api/auth/login",
                "/api/cars",
                "/api/cars/{id}",
                "/api/cart",
                "/api/cart/add",
                "/api/cart/update/{itemId}",
                "/api/cart/remove/{itemId}",
                "/api/orders/checkout",
                "/api/orders",
                "/api/orders/{id}",
                "/api/orders/{id}/status",
                "/api/orders/{id}/cancel",
                "/api/ai/recommend",
                "/api/soap/quote/{carId}");

        Map<String, Object> carSchema = document.getMap("components.schemas.CarResponse.properties");
        assertThat(carSchema).containsKeys(
                "id", "brand", "model", "year", "price", "mileage", "fuelType",
                "fuelConsumption", "transmission", "bodyType", "imageUrl",
                "description", "stock", "version");
    }

    @Test
    void authEndpointsCoverSuccessAndErrorCases() {
        String email = uniqueEmail("contract-auth");
        JsonPath registered = register(email);
        assertThat(registered.getLong("user.id")).isPositive();
        assertThat(registered.getString("user.role")).isEqualTo("CUSTOMER");

        // Registro duplicado → 409 Problem Details
        given()
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "email", email,
                        "password", "IntegrationPassw0rd!",
                        "fullName", "Integration User"))
                .post("/api/auth/register")
                .then()
                .statusCode(409)
                .contentType("application/problem+json")
                .body("status", equalTo(409));

        // Credenciales inválidas → 401 Problem Details
        given()
                .contentType(ContentType.JSON)
                .body(Map.of("email", email, "password", "WrongPassw0rd!"))
                .post("/api/auth/login")
                .then()
                .statusCode(401)
                .contentType("application/problem+json")
                .body("title", equalTo("Credenciales inválidas"));

        // Validación de entrada → 400 Problem Details
        given()
                .contentType(ContentType.JSON)
                .body(Map.of("email", "no-es-email", "password", "x"))
                .post("/api/auth/register")
                .then()
                .statusCode(400)
                .contentType("application/problem+json");
    }

    @Test
    void catalogCoversSuccessAndNotFound() {
        given()
                .get("/api/cars?page=0&size=5")
                .then()
                .statusCode(200)
                .body("content.size()", greaterThan(0))
                .body("content[0].id", notNullValue())
                .body("content[0].version", notNullValue())
                .body("content[0].stock", notNullValue());

        given()
                .get("/api/cars/99999999")
                .then()
                .statusCode(404)
                .contentType("application/problem+json");
    }

    @Test
    void protectedEndpointsRejectAnonymousAndCustomerAdminOperations() {
        given()
                .get("/api/cart")
                .then()
                .statusCode(401)
                .contentType("application/problem+json");

        String customerToken = register(uniqueEmail("contract-bfla")).getString("token");
        given()
                .auth().oauth2(customerToken)
                .contentType(ContentType.JSON)
                .body(carPayload("BFLA"))
                .post("/api/cars")
                .then()
                .statusCode(403)
                .contentType("application/problem+json");
    }

    @Test
    void adminCarCrudEnforcesEtagAndIgnoresMassAssignment() {
        String token = adminToken();
        Map<String, Object> payload = carPayload("Etag-" + UUID.randomUUID());
        payload.put("id", 99999999L);
        payload.put("version", 12345L);
        payload.put("role", "ADMIN");

        JsonPath created = given()
                .auth().oauth2(token)
                .contentType(ContentType.JSON)
                .body(payload)
                .post("/api/cars")
                .then()
                .statusCode(201)
                .header("ETag", notNullValue())
                .body("id", not(equalTo(99999999)))
                .body("version", equalTo(0))
                .extract()
                .jsonPath();
        long id = created.getLong("id");

        // If-Match desactualizado → 412
        Map<String, Object> update = carPayload("Etag-updated-" + UUID.randomUUID());
        given()
                .auth().oauth2(token)
                .contentType(ContentType.JSON)
                .header("If-Match", "\"999\"")
                .body(update)
                .put("/api/cars/" + id)
                .then()
                .statusCode(412)
                .contentType("application/problem+json");

        // If-Match correcto → 200
        String etag = given()
                .auth().oauth2(token)
                .get("/api/cars/" + id)
                .then()
                .statusCode(200)
                .extract()
                .header("ETag");

        given()
                .auth().oauth2(token)
                .contentType(ContentType.JSON)
                .header("If-Match", etag)
                .body(update)
                .put("/api/cars/" + id)
                .then()
                .statusCode(200)
                .body("model", equalTo(update.get("model")));

        given()
                .auth().oauth2(token)
                .delete("/api/cars/" + id)
                .then()
                .statusCode(204);
    }

    @Test
    void cartIsIsolatedPerUserAndRejectsForeignItem() {
        JsonPath userA = register(uniqueEmail("contract-cart-a"));
        JsonPath userB = register(uniqueEmail("contract-cart-b"));

        JsonPath cartA = given()
                .auth().oauth2(userA.getString("token"))
                .contentType(ContentType.JSON)
                .body(Map.of("carId", 1, "quantity", 1))
                .post("/api/cart/add")
                .then()
                .statusCode(200)
                .body("items.size()", equalTo(1))
                .extract()
                .jsonPath();
        long itemId = cartA.getLong("items[0].id");
        long userIdA = userA.getLong("user.id");

        // B no ve el carrito de A.
        given()
                .auth().oauth2(userB.getString("token"))
                .get("/api/cart")
                .then()
                .statusCode(200)
                .body("items.size()", equalTo(0))
                .body("userId", not(equalTo((int) userIdA)));

        // B no puede modificar un ítem de A.
        given()
                .auth().oauth2(userB.getString("token"))
                .contentType(ContentType.JSON)
                .body(Map.of("quantity", 2))
                .put("/api/cart/update/" + itemId)
                .then()
                .statusCode(404);

        given()
                .auth().oauth2(userB.getString("token"))
                .delete("/api/cart/remove/" + itemId)
                .then()
                .statusCode(404);
    }

    @Test
    void checkoutIsIdempotentAndOrderIsPrivate() {
        JsonPath owner = register(uniqueEmail("contract-order-owner"));
        JsonPath other = register(uniqueEmail("contract-order-other"));
        String key = UUID.randomUUID().toString();
        Map<String, Object> checkout = Map.of("items", java.util.List.of(Map.of("carId", 2, "quantity", 1)));

        JsonPath first = given()
                .auth().oauth2(owner.getString("token"))
                .contentType(ContentType.JSON)
                .header("Idempotency-Key", key)
                .body(checkout)
                .post("/api/orders/checkout")
                .then()
                .statusCode(200)
                .body("status", equalTo("CONFIRMED"))
                .extract()
                .jsonPath();

        JsonPath replay = given()
                .auth().oauth2(owner.getString("token"))
                .contentType(ContentType.JSON)
                .header("Idempotency-Key", key)
                .body(checkout)
                .post("/api/orders/checkout")
                .then()
                .statusCode(200)
                .extract()
                .jsonPath();

        assertThat(replay.getLong("id")).isEqualTo(first.getLong("id"));

        long orderId = first.getLong("id");

        // Otro usuario no puede ver la orden (BOLA/IDOR) → 404
        given()
                .auth().oauth2(other.getString("token"))
                .get("/api/orders/" + orderId)
                .then()
                .statusCode(404)
                .contentType("application/problem+json");

        // Cancelación válida y luego transición inválida → 409
        given()
                .auth().oauth2(owner.getString("token"))
                .post("/api/orders/" + orderId + "/cancel")
                .then()
                .statusCode(200)
                .body("status", equalTo("CANCELLED"));

        given()
                .auth().oauth2(owner.getString("token"))
                .post("/api/orders/" + orderId + "/cancel")
                .then()
                .statusCode(409)
                .contentType("application/problem+json");
    }

    @Test
    void healthEndpointIsUp() {
        given()
                .get("/actuator/health")
                .then()
                .statusCode(200)
                .body("status", equalTo("UP"));
    }

    @Test
    void sqlPayloadsAreTreatedAsDataAndDoNotBroadenQueries() {
        long baseline = given().get("/api/cars").then().statusCode(200)
                .extract().jsonPath().getLong("totalElements");
        for (String payload : java.util.List.of("' OR 1=1 --", "'; DROP TABLE cars; --",
                "' UNION SELECT password_hash FROM app_users --")) {
            given().queryParam("q", payload).get("/api/cars")
                    .then().statusCode(200).body("totalElements", equalTo(0));
            given().queryParam("brand", payload).get("/api/cars")
                    .then().statusCode(200).body("totalElements", equalTo(0));
        }
        assertThat(given().get("/api/cars").then().statusCode(200)
                .extract().jsonPath().getLong("totalElements")).isEqualTo(baseline);
    }

    @Test
    void storedHtmlRemainsJsonDataWithoutMassAssignedFields() {
        String token = adminToken();
        String payload = "<img src=x onerror=alert('xss')>";
        Map<String, Object> car = carPayload("LiteralHTML-" + UUID.randomUUID());
        car.put("description", payload);
        car.put("role", "ADMIN");
        car.put("id", 999999999);
        long id = given().auth().oauth2(token).contentType(ContentType.JSON).body(car)
                .post("/api/cars").then().statusCode(201).body("description", equalTo(payload))
                .extract().jsonPath().getLong("id");
        try {
            given().get("/api/cars/" + id).then().statusCode(200)
                    .contentType(ContentType.JSON).body("description", equalTo(payload));
            assertThat(id).isNotEqualTo(999999999L);
        } finally {
            given().auth().oauth2(token).delete("/api/cars/" + id).then().statusCode(204);
        }
    }
}
