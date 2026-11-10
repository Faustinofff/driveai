package com.driveai.controller;

import com.driveai.config.AppProperties;
import com.driveai.domain.enums.BodyType;
import com.driveai.domain.enums.FuelType;
import com.driveai.domain.enums.OrderStatus;
import com.driveai.domain.enums.Transmission;
import com.driveai.domain.model.Car;
import com.driveai.dto.ai.AiRecommendResponse;
import com.driveai.dto.auth.AuthResponse;
import com.driveai.dto.car.CarResponse;
import com.driveai.dto.order.OrderItemResponse;
import com.driveai.dto.order.OrderResponse;
import com.driveai.exception.ConflictException;
import com.driveai.exception.GroqApiException;
import com.driveai.exception.InsufficientStockException;
import com.driveai.exception.InvalidRequestException;
import com.driveai.exception.SoapBusinessException;
import com.driveai.exception.PreconditionFailedException;
import com.driveai.exception.RateLimitExceededException;
import com.driveai.exception.ResourceNotFoundException;
import com.driveai.rate.LoginRateLimitService;
import com.driveai.security.JwtAuthenticationFilter;
import com.driveai.security.JwtService;
import com.driveai.security.SecurityConfig;
import com.driveai.service.AuthService;
import com.driveai.service.CarService;
import com.driveai.service.CartService;
import com.driveai.service.GroqAdvisorService;
import com.driveai.service.IdempotencyService;
import com.driveai.service.OrderService;
import com.driveai.soap.SoapVehicleClient;
import com.driveai.soap.VehicleQuote;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.mapping.PropertyReferenceException;
import org.springframework.data.util.TypeInformation;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.function.Supplier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtService.class})
@EnableConfigurationProperties(AppProperties.class)
class ApiSecurityTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JwtService jwtService;

    @MockBean
    private AuthService authService;
    @MockBean
    private LoginRateLimitService loginRateLimitService;
    @MockBean
    private CarService carService;
    @MockBean
    private CartService cartService;
    @MockBean
    private OrderService orderService;
    @MockBean
    private IdempotencyService idempotencyService;
    @MockBean
    private GroqAdvisorService groqAdvisorService;
    @MockBean
    private SoapVehicleClient soapVehicleClient;

    private String token(long id, String role) {
        return "Bearer " + jwtService.generateToken(id, "user" + id + "@driveai.test", role);
    }

    private static Car car() {
        return Car.builder()
                .id(7L)
                .brand("Toyota")
                .model("Corolla")
                .year(2022)
                .price(new BigDecimal("22500.00"))
                .mileage(10000)
                .fuelType(FuelType.GASOLINA)
                .fuelConsumption(new BigDecimal("5.80"))
                .transmission(Transmission.AUTOMATICA)
                .bodyType(BodyType.SEDAN)
                .imageUrl("https://img.test/car.jpg")
                .description("Auto de prueba")
                .stock(3)
                .version(4L)
                .build();
    }

    private static OrderResponse order() {
        return new OrderResponse(5L, 42L, OrderStatus.CONFIRMED, new BigDecimal("22500.00"),
                List.of(new OrderItemResponse(7L, CarResponse.from(car()), 1,
                        new BigDecimal("22500.00"), new BigDecimal("22500.00"))),
                Instant.parse("2026-01-01T00:00:00Z"));
    }

    @Test
    void catalogIsPublic() throws Exception {
        when(carService.list(any(), any())).thenReturn(new PageImpl<>(
                List.of(car()), PageRequest.of(0, 12), 1));

        mockMvc.perform(get("/api/cars"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].brand").value("Toyota"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void carDetailReturnsEtag() throws Exception {
        when(carService.getById(7L)).thenReturn(car());

        mockMvc.perform(get("/api/cars/7"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ETAG, "\"4\""))
                .andExpect(jsonPath("$.version").value(4));
    }

    @Test
    void carDetailMissingReturnsProblemJson() throws Exception {
        when(carService.getById(99L)).thenThrow(new ResourceNotFoundException("Auto", 99L));

        mockMvc.perform(get("/api/cars/99"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Recurso no encontrado"));
    }

    @Test
    void anonymousCannotCreateCar() throws Exception {
        mockMvc.perform(post("/api/cars")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void customerCannotCreateCar() throws Exception {
        mockMvc.perform(post("/api/cars")
                        .header(HttpHeaders.AUTHORIZATION, token(42L, "CUSTOMER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validCarJson()))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCreatesCarWithEtag() throws Exception {
        when(carService.create(any())).thenReturn(car());

        mockMvc.perform(post("/api/cars")
                        .header(HttpHeaders.AUTHORIZATION, token(1L, "ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validCarJson()))
                .andExpect(status().isCreated())
                .andExpect(header().string(HttpHeaders.ETAG, "\"4\""))
                .andExpect(jsonPath("$.id").value(7));
    }

    @Test
    void staleIfMatchReturnsPreconditionFailed() throws Exception {
        when(carService.update(eq(7L), any(), eq("\"1\"")))
                .thenThrow(new PreconditionFailedException("modificado"));

        mockMvc.perform(put("/api/cars/7")
                        .header(HttpHeaders.AUTHORIZATION, token(1L, "ADMIN"))
                        .header(HttpHeaders.IF_MATCH, "\"1\"")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validCarJson()))
                .andExpect(status().isPreconditionFailed())
                .andExpect(jsonPath("$.title").value("Precondición fallida"));
    }

    @Test
    void adminDeletesCar() throws Exception {
        mockMvc.perform(delete("/api/cars/7")
                        .header(HttpHeaders.AUTHORIZATION, token(1L, "ADMIN")))
                .andExpect(status().isNoContent());
    }

    @Test
    void invalidTokenIsRejected() throws Exception {
        mockMvc.perform(get("/api/cart")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer not-a-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("Inicia")));
    }

    @Test
    void tokenWithUnknownRoleIsRejected() throws Exception {
        mockMvc.perform(get("/api/cart")
                        .header(HttpHeaders.AUTHORIZATION, token(42L, "SUPERADMIN")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void cartRequiresAuthenticationAndUsesJwtIdentity() throws Exception {
        when(cartService.getCart(42L)).thenReturn(
                new com.driveai.dto.cart.CartResponse(42L, List.of(), BigDecimal.ZERO));

        mockMvc.perform(get("/api/cart"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/cart")
                        .header(HttpHeaders.AUTHORIZATION, token(42L, "CUSTOMER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(42));
    }

    @Test
    void loginRateLimitReturns429WithRetryAfter() throws Exception {
        doThrow(new RateLimitExceededException(30))
                .when(loginRateLimitService).check(anyString(), anyString());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"ana@driveai.test\",\"password\":\"secret123\"}"))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string(HttpHeaders.RETRY_AFTER, "30"));
    }

    @Test
    void registerReturnsCreated() throws Exception {
        when(authService.register(any())).thenReturn(new AuthResponse("jwt",
                new AuthResponse.UserResponse(1L, "ana@driveai.test", "Ana", null)));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"ana@driveai.test\",\"password\":\"secret123\",\"fullName\":\"Ana\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").value("jwt"));
    }

    @Test
    void loginBadCredentialsReturns401() throws Exception {
        when(authService.login(any())).thenThrow(
                new org.springframework.security.authentication.BadCredentialsException("bad"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"ana@driveai.test\",\"password\":\"wrongpass\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title").value("Credenciales inválidas"));
    }

    @Test
    void checkoutIsIdempotentFromControllerPerspective() throws Exception {
        when(idempotencyService.execute(anyLong(), any(), any(), eq(OrderResponse.class), any(Supplier.class)))
                .thenReturn(order());

        mockMvc.perform(post("/api/orders/checkout")
                        .header(HttpHeaders.AUTHORIZATION, token(42L, "CUSTOMER"))
                        .header("Idempotency-Key", "key-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"items\":[{\"carId\":7,\"quantity\":1}]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5));
    }

    /** DEF-13 (hallazgo de Schemathesis): Idempotency-Key > 255 caracteres → 400, no 500. */
    @Test
    void oversizedIdempotencyKeyReturns400() throws Exception {
        when(idempotencyService.execute(anyLong(), any(), any(), eq(OrderResponse.class), any(Supplier.class)))
                .thenThrow(new InvalidRequestException("Idempotency-Key no puede superar 255 caracteres."));

        mockMvc.perform(post("/api/orders/checkout")
                        .header(HttpHeaders.AUTHORIZATION, token(42L, "CUSTOMER"))
                        .header("Idempotency-Key", "k".repeat(300))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"items\":[{\"carId\":7,\"quantity\":1}]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Idempotency-Key no puede superar 255 caracteres."));
    }

    @Test
    void invalidTransitionReturns409() throws Exception {
        when(orderService.changeStatus(eq(5L), eq(OrderStatus.CANCELLED), any()))
                .thenThrow(new ConflictException("Transición de estado inválida"));

        mockMvc.perform(post("/api/orders/5/cancel")
                        .header(HttpHeaders.AUTHORIZATION, token(42L, "CUSTOMER")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Conflicto"));
    }

    @Test
    void orderDetailOfAnotherUserReturns404() throws Exception {
        when(orderService.getForUser(eq(5L), any()))
                .thenThrow(new ResourceNotFoundException("Orden", 5L));

        mockMvc.perform(get("/api/orders/5")
                        .header(HttpHeaders.AUTHORIZATION, token(42L, "CUSTOMER")))
                .andExpect(status().isNotFound());
    }

    @Test
    void soapQuoteUsesFallbackPayload() throws Exception {
        when(soapVehicleClient.getQuote(7L))
                .thenReturn(new VehicleQuote(7L, true, null, "USD", "precio publicado", true));

        mockMvc.perform(get("/api/soap/quote/7")
                        .header(HttpHeaders.AUTHORIZATION, token(42L, "CUSTOMER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fallback").value(true));
    }

    @Test
    void aiRecommendationRequiresAuthentication() throws Exception {
        when(groqAdvisorService.recommend(anyString(), anyString(), anyString()))
                .thenReturn(new AiRecommendResponse("s-1", "ok", List.of()));

        mockMvc.perform(post("/api/ai/recommend")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"prompt\":\"auto económico\",\"sessionId\":\"s-1\"}"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/ai/recommend")
                        .header(HttpHeaders.AUTHORIZATION, token(42L, "CUSTOMER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"prompt\":\"auto económico\",\"sessionId\":\"s-1\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reasoning").value("ok"));
    }

    @Test
    void corsAllowsConfiguredOriginAndRejectsUnknownOrigin() throws Exception {
        mockMvc.perform(options("/api/cars")
                        .header(HttpHeaders.ORIGIN, "http://localhost:5173")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:5173"));

        mockMvc.perform(options("/api/cars")
                        .header(HttpHeaders.ORIGIN, "http://evil.test")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
                .andExpect(status().isForbidden());
    }

    @Test
    void invalidBodyReturns400ProblemJson() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"not-an-email\",\"password\":\"x\",\"fullName\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.fields.email").exists());
    }


    @Test
    void insufficientStockReturns409WithDetails() throws Exception {
        when(carService.getById(7L))
                .thenThrow(new InsufficientStockException(7L, 5, 1));

        mockMvc.perform(get("/api/cars/7"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Stock insuficiente"))
                .andExpect(jsonPath("$.carId").value(7))
                .andExpect(jsonPath("$.available").value(1));
    }

    @Test
    void malformedJsonReturns400() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{not-json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Solicitud inválida"));
    }

    @Test
    void invalidQueryParamTypeReturns400() throws Exception {
        mockMvc.perform(get("/api/cars").param("minYear", "abc"))
                .andExpect(status().isBadRequest());
    }

    /** DEF-10: un sort por un campo inexistente es un 400, no un 500. */
    @Test
    void invalidSortPropertyReturns400() throws Exception {
        when(carService.list(any(), any())).thenThrow(new PropertyReferenceException(
                "noExiste", TypeInformation.of(Car.class), List.of()));

        mockMvc.perform(get("/api/cars").param("sort", "noExiste"))
                .andExpect(status().isBadRequest())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE,
                        org.hamcrest.Matchers.containsString("application/problem+json")))
                .andExpect(jsonPath("$.detail").value("Parámetro de ordenamiento inválido."));
    }

    /** DEF-10: un método HTTP no soportado debe responder 405, no 500. */
    @Test
    void unsupportedMethodReturns405NotServerError() throws Exception {
        mockMvc.perform(patch("/api/cars/1")
                        .header(HttpHeaders.AUTHORIZATION, token(1L, "ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.status").value(405));
    }

    /** DEF-10: un Content-Type no soportado debe responder 415, no 500. */
    @Test
    void unsupportedMediaTypeReturns415NotServerError() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("hola"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.status").value(415));
    }

    @Test
    void unexpectedErrorReturns500WithoutLeakingDetails() throws Exception {
        when(carService.list(any(), any())).thenThrow(new RuntimeException("database password leaked"));

        mockMvc.perform(get("/api/cars"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.detail").value("Ocurrió un error inesperado. Inténtalo de nuevo más tarde."));
    }

    @Test
    void groqFailureReturns503() throws Exception {
        when(groqAdvisorService.recommend(anyString(), anyString(), anyString()))
                .thenThrow(new GroqApiException("saturado"));

        mockMvc.perform(post("/api/ai/recommend")
                        .header(HttpHeaders.AUTHORIZATION, token(42L, "CUSTOMER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"prompt\":\"auto\",\"sessionId\":\"s\"}"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.title").value("Servicio de IA no disponible"));
    }

    /**
     * DEF-08: la cuota de IA se ata al usuario del JWT. Cambiar sessionId o
     * X-Forwarded-For en cada request no debe generar una clave nueva.
     */
    @Test
    void aiRateLimitKeyIsBoundToAuthenticatedUserNotToClientData() throws Exception {
        when(groqAdvisorService.recommend(anyString(), anyString(), anyString()))
                .thenReturn(new AiRecommendResponse("s", "ok", List.of()));

        mockMvc.perform(post("/api/ai/recommend")
                        .header(HttpHeaders.AUTHORIZATION, token(42L, "CUSTOMER"))
                        .header("X-Forwarded-For", "203.0.113.7")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"prompt\":\"auto\",\"sessionId\":\"rotada-1\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/ai/recommend")
                        .header(HttpHeaders.AUTHORIZATION, token(42L, "CUSTOMER"))
                        .header("X-Forwarded-For", "198.51.100.9")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"prompt\":\"auto\",\"sessionId\":\"rotada-2\"}"))
                .andExpect(status().isOk());

        verify(groqAdvisorService).recommend("auto", "rotada-1", "user:42");
        verify(groqAdvisorService).recommend("auto", "rotada-2", "user:42");
    }

    @Test
    void soapBusinessFaultReturns409() throws Exception {
        when(soapVehicleClient.getQuote(7L)).thenThrow(new SoapBusinessException("no disponible"));

        mockMvc.perform(get("/api/soap/quote/7")
                        .header(HttpHeaders.AUTHORIZATION, token(42L, "CUSTOMER")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Cotización no disponible"));
    }

    @Test
    void orderListReturnsOnlyOwnOrders() throws Exception {
        when(orderService.listForUser(42L)).thenReturn(List.of(order()));

        mockMvc.perform(get("/api/orders")
                        .header(HttpHeaders.AUTHORIZATION, token(42L, "CUSTOMER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(5));
    }

    @Test
    void cartAddAndRemoveUseJwtOwner() throws Exception {
        when(cartService.addToCart(any(), eq(42L))).thenReturn(
                new com.driveai.dto.cart.CartResponse(42L, List.of(), BigDecimal.ZERO));
        when(cartService.removeItem(anyLong(), eq(42L))).thenReturn(
                new com.driveai.dto.cart.CartResponse(42L, List.of(), BigDecimal.ZERO));

        mockMvc.perform(post("/api/cart/add")
                        .header(HttpHeaders.AUTHORIZATION, token(42L, "CUSTOMER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"carId\":7,\"quantity\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(42));

        mockMvc.perform(delete("/api/cart/remove/3")
                        .header(HttpHeaders.AUTHORIZATION, token(42L, "CUSTOMER")))
                .andExpect(status().isOk());
    }

    @Test
    void adminCanPatchOrderStatus() throws Exception {
        when(orderService.changeStatus(eq(5L), eq(OrderStatus.CONFIRMED), any())).thenReturn(order());

        mockMvc.perform(patch("/api/orders/5/status")
                        .header(HttpHeaders.AUTHORIZATION, token(1L, "ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"CONFIRMED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));
    }

    private static String validCarJson() {
        return """
                {"brand":"Toyota","model":"Corolla","year":2022,"price":22500.00,
                 "mileage":10000,"fuelType":"GASOLINA","fuelConsumption":5.80,
                 "transmission":"AUTOMATICA","bodyType":"SEDAN",
                 "imageUrl":"https://img.test/car.jpg","description":"Auto de prueba","stock":3}
                """;
    }
}
