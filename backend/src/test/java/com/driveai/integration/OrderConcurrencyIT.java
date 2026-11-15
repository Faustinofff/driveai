package com.driveai.integration;

import com.driveai.domain.enums.*;
import com.driveai.domain.model.*;
import com.driveai.exception.ConflictException;
import com.driveai.exception.InsufficientStockException;
import com.driveai.dto.order.CheckoutItemRequest;
import com.driveai.dto.order.CheckoutRequest;
import com.driveai.dto.cart.CartAddRequest;
import com.driveai.service.CartService;
import com.driveai.repository.*;
import com.driveai.security.UserPrincipal;
import com.driveai.service.OrderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = "app.outbox.enabled=false")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Testcontainers
class OrderConcurrencyIT {
    @Container @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");
    @Container @ServiceConnection
    static RabbitMQContainer rabbit = new RabbitMQContainer("rabbitmq:3.13-management-alpine");

    @Autowired OrderService service;
    @Autowired OrderRepository orders;
    @Autowired CarRepository cars;
    @Autowired AppUserRepository users;
    @Autowired JdbcTemplate jdbc;
    @Autowired CartService carts;

    private AppUser buyer() {
        return users.saveAndFlush(AppUser.builder()
                .email("checkout-" + UUID.randomUUID() + "@driveai.test")
                .passwordHash("test-only-not-used-for-login").fullName("Checkout Test")
                .role(UserRole.CUSTOMER).build());
    }

    private Car inventory(int stock) {
        return cars.saveAndFlush(Car.builder().brand("Race").model("Checkout")
                .year(2024).price(new BigDecimal("100.00")).mileage(0)
                .fuelType(FuelType.GASOLINA).fuelConsumption(new BigDecimal("5.00"))
                .transmission(Transmission.AUTOMATICA).bodyType(BodyType.SEDAN)
                .imageUrl("https://img.test/checkout.jpg").description("Transaction test")
                .stock(stock).build());
    }

    @Test
    void failedLaterLineRollsBackEarlierStockAndPreservesCart() {
        AppUser owner = buyer();
        Car first = inventory(5);
        Car unavailable = inventory(0);
        carts.addToCart(new CartAddRequest(first.getId(), 2), owner.getId());
        // Los locks se toman por ID: la primera linea se modifica antes del fallo.
        var request = new CheckoutRequest(java.util.List.of(
                new CheckoutItemRequest(first.getId(), 2),
                new CheckoutItemRequest(unavailable.getId(), 1)));

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.checkout(request, owner.getId()))
                .isInstanceOf(InsufficientStockException.class);

        assertThat(cars.findById(first.getId()).orElseThrow().getStock()).isEqualTo(5);
        assertThat(cars.findById(first.getId()).orElseThrow().getVersion()).isEqualTo(first.getVersion());
        assertThat(cars.findById(unavailable.getId()).orElseThrow().getStock()).isZero();
        assertThat(orders.findByUserIdOrderByCreatedAtDesc(owner.getId())).isEmpty();
        assertThat(carts.getCart(owner.getId()).items()).singleElement()
                .satisfies(item -> assertThat(item.quantity()).isEqualTo(2));
        assertThat(jdbc.queryForObject(
                "select count(*) from outbox_events where payload::jsonb ->> 'userId' = ?",
                Integer.class, owner.getId().toString())).isZero();
    }

    @Test
    void twoBuyersCannotPurchaseTheSameLastUnit() throws Exception {
        AppUser first = buyer();
        AppUser second = buyer();
        Car lastUnit = inventory(1);
        var request = new CheckoutRequest(java.util.List.of(new CheckoutItemRequest(lastUnit.getId(), 1)));
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        java.util.function.Function<Long, Callable<Integer>> buy = userId -> () -> {
            ready.countDown();
            if (!start.await(10, TimeUnit.SECONDS)) throw new IllegalStateException("Workers not ready");
            try {
                service.checkout(request, userId);
                return 200;
            } catch (InsufficientStockException expected) {
                return 409;
            }
        };
        try {
            Future<Integer> a = executor.submit(buy.apply(first.getId()));
            Future<Integer> b = executor.submit(buy.apply(second.getId()));
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            assertThat(java.util.List.of(a.get(20, TimeUnit.SECONDS), b.get(20, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder(200, 409);
            assertThat(cars.findById(lastUnit.getId()).orElseThrow().getStock()).isZero();
            assertThat(orders.findByUserIdOrderByCreatedAtDesc(first.getId()).size()
                    + orders.findByUserIdOrderByCreatedAtDesc(second.getId()).size()).isEqualTo(1);
            assertThat(jdbc.queryForObject(
                    "select count(*) from outbox_events where payload::jsonb ->> 'userId' in (?, ?)",
                    Integer.class, first.getId().toString(), second.getId().toString())).isEqualTo(1);
        } finally {
            start.countDown();
            executor.shutdownNow();
        }
    }

    @Test
    void simultaneousCancellationsRestoreStockAndRecordEventOnlyOnce() throws Exception {
        AppUser user = users.saveAndFlush(AppUser.builder()
                .email("race-" + UUID.randomUUID() + "@driveai.test")
                .passwordHash("test-only-not-used-for-login").fullName("Race Test")
                .role(UserRole.CUSTOMER).build());
        Car car = cars.saveAndFlush(Car.builder().brand("Race").model("Cancellation")
                .year(2024).price(new BigDecimal("100.00")).mileage(0)
                .fuelType(FuelType.GASOLINA).fuelConsumption(new BigDecimal("5.00"))
                .transmission(Transmission.AUTOMATICA).bodyType(BodyType.SEDAN)
                .imageUrl("https://img.test/race.jpg").description("Concurrent cancellation test")
                .stock(3).build());
        Order order = Order.builder().userId(user.getId()).status(OrderStatus.CONFIRMED)
                .totalAmount(new BigDecimal("200.00")).build();
        order.addItem(OrderItem.builder().car(car).quantity(2).unitPrice(car.getPrice()).build());
        Long id = orders.saveAndFlush(order).getId();
        UserPrincipal principal = new UserPrincipal(user.getId(), user.getEmail(), "CUSTOMER");
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        Callable<Integer> cancel = () -> {
            ready.countDown();
            if (!start.await(10, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Concurrent workers did not start");
            }
            try {
                service.changeStatus(id, OrderStatus.CANCELLED, principal);
                return 200;
            } catch (ConflictException expected) {
                return 409;
            }
        };
        try {
            Future<Integer> first = executor.submit(cancel);
            Future<Integer> second = executor.submit(cancel);
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            assertThat(java.util.List.of(first.get(15, TimeUnit.SECONDS),
                    second.get(15, TimeUnit.SECONDS))).containsExactlyInAnyOrder(200, 409);
            assertThat(cars.findById(car.getId()).orElseThrow().getStock()).isEqualTo(5);
            assertThat(orders.findById(id).orElseThrow().getStatus()).isEqualTo(OrderStatus.CANCELLED);
            assertThat(jdbc.queryForObject(
                    "select count(*) from outbox_events where aggregate_id = ? and event_type = ?",
                    Integer.class, id, "ORDER_STATUS_CHANGED")).isEqualTo(1);
        } finally {
            start.countDown();
            executor.shutdownNow();
        }
    }
}
