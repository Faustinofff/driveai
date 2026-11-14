package com.driveai.integration;

import com.driveai.domain.enums.BodyType;
import com.driveai.domain.enums.FuelType;
import com.driveai.domain.enums.OrderStatus;
import com.driveai.domain.enums.Transmission;
import com.driveai.domain.enums.UserRole;
import com.driveai.domain.model.AppUser;
import com.driveai.domain.model.Car;
import com.driveai.domain.model.Cart;
import com.driveai.domain.model.Order;
import com.driveai.domain.model.OrderItem;
import com.driveai.repository.AppUserRepository;
import com.driveai.repository.CarRepository;
import com.driveai.repository.CartRepository;
import com.driveai.repository.IdempotencyRepository;
import com.driveai.repository.OrderRepository;
import com.driveai.repository.ProcessedMessageRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Pruebas de integración con PostgreSQL y RabbitMQ reales en contenedores.
 * Se ejecutan con {@code mvn verify -Pintegration} (requiere Docker).
 */
@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Testcontainers
class PersistenceIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Container
    @ServiceConnection
    static RabbitMQContainer rabbit = new RabbitMQContainer("rabbitmq:3.13-management-alpine");

    @Autowired
    private AppUserRepository userRepository;
    @Autowired
    private CarRepository carRepository;
    @Autowired
    private CartRepository cartRepository;
    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private IdempotencyRepository idempotencyRepository;
    @Autowired
    private ProcessedMessageRepository processedMessageRepository;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private AppUser user() {
        return userRepository.save(AppUser.builder()
                .email("it-" + UUID.randomUUID() + "@driveai.test")
                .passwordHash("$2a$10$abcdefghijklmnopqrstuv")
                .fullName("Integration User")
                .role(UserRole.CUSTOMER)
                .build());
    }

    private Car car() {
        return carRepository.save(Car.builder()
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
                .description("Auto de integración")
                .stock(5)
                .build());
    }

    @Test
    void flywayCreatesOwnershipAndConcurrencyColumns() {
        List<String> cartColumns = jdbcTemplate.queryForList(
                "SELECT column_name FROM information_schema.columns WHERE table_name = 'carts'",
                String.class);
        List<String> carColumns = jdbcTemplate.queryForList(
                "SELECT column_name FROM information_schema.columns WHERE table_name = 'cars'",
                String.class);

        assertThat(cartColumns).contains("user_id", "session_id");
        assertThat(carColumns).contains("version");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM flyway_schema_history WHERE success = true", Integer.class))
                .isGreaterThanOrEqualTo(8);
    }

    @Test
    void oneCartPerUserIsEnforcedByUniqueIndex() {
        AppUser owner = user();
        Cart first = new Cart();
        first.setUserId(owner.getId());
        cartRepository.saveAndFlush(first);

        Cart duplicate = new Cart();
        duplicate.setUserId(owner.getId());

        assertThatThrownBy(() -> cartRepository.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void optimisticVersionIncrementsOnUpdate() {
        Car car = car();
        assertThat(car.getVersion()).isZero();

        car.setPrice(new BigDecimal("21000.00"));
        Car updated = carRepository.saveAndFlush(car);

        assertThat(updated.getVersion()).isEqualTo(1L);
    }

    @Test
    void ordersAreFoundOnlyForTheirOwner() {
        AppUser owner = user();
        AppUser other = user();
        Car car = car();
        Order order = Order.builder()
                .userId(owner.getId())
                .status(OrderStatus.CONFIRMED)
                .totalAmount(new BigDecimal("22500.00"))
                .build();
        order.addItem(OrderItem.builder()
                .car(car)
                .quantity(1)
                .unitPrice(car.getPrice())
                .build());
        Order saved = orderRepository.saveAndFlush(order);

        assertThat(orderRepository.findByIdAndUserId(saved.getId(), owner.getId())).isPresent();
        assertThat(orderRepository.findByIdAndUserId(saved.getId(), other.getId())).isEmpty();
        assertThat(orderRepository.findByUserIdOrderByCreatedAtDesc(owner.getId())).hasSize(1);
    }

    @Test
    void idempotencyReservationIsAtomicAndCompletable() {
        AppUser owner = user();

        assertThat(idempotencyRepository.insertReservation(owner.getId(), "key-1", "hash-1")).isEqualTo(1);
        assertThat(idempotencyRepository.insertReservation(owner.getId(), "key-1", "hash-1")).isZero();

        assertThat(idempotencyRepository.complete(owner.getId(), "key-1", 200, "{\"id\":1}")).isEqualTo(1);
        assertThat(idempotencyRepository.findByUserIdAndIdempotencyKey(owner.getId(), "key-1"))
                .get()
                .extracting(record -> record.getResponseBody())
                .isEqualTo("{\"id\":1}");
    }

    @Test
    void processedMessagesAreIdempotentAtDatabaseLevel() {
        String messageId = UUID.randomUUID().toString();

        assertThat(processedMessageRepository.insertIfAbsent(messageId, "consumer")).isEqualTo(1);
        assertThat(processedMessageRepository.insertIfAbsent(messageId, "consumer")).isZero();
    }
}
