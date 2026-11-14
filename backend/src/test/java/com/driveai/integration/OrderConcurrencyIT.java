package com.driveai.integration;

import com.driveai.domain.enums.*;
import com.driveai.domain.model.*;
import com.driveai.exception.ConflictException;
import com.driveai.repository.*;
import com.driveai.security.UserPrincipal;
import com.driveai.service.OrderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
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
