package com.driveai.service;

import com.driveai.domain.enums.BodyType;
import com.driveai.domain.enums.FuelType;
import com.driveai.domain.enums.OrderStatus;
import com.driveai.domain.enums.Transmission;
import com.driveai.domain.model.Car;
import com.driveai.domain.model.Order;
import com.driveai.domain.model.OrderItem;
import com.driveai.dto.order.CheckoutItemRequest;
import com.driveai.dto.order.CheckoutRequest;
import com.driveai.dto.order.OrderResponse;
import com.driveai.exception.ConflictException;
import com.driveai.exception.InsufficientStockException;
import com.driveai.exception.ResourceNotFoundException;
import com.driveai.messaging.OutboxService;
import com.driveai.repository.CarRepository;
import com.driveai.repository.OrderRepository;
import com.driveai.security.UserPrincipal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    private static final Long USER_ID = 42L;

    @Mock
    private CarRepository carRepository;
    @Mock
    private OrderRepository orderRepository;
    @Mock
    private CartService cartService;
    @Mock
    private OutboxService outboxService;

    @InjectMocks
    private OrderService orderService;

    @Test
    void checkoutDeductsStockAndCreatesConfirmedOrder() {
        Car car = car(7L, 5, new BigDecimal("22500.00"));
        when(carRepository.findWithLockingById(7L)).thenReturn(Optional.of(car));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CheckoutRequest request = new CheckoutRequest(List.of(new CheckoutItemRequest(7L, 2)));

        OrderResponse response = orderService.checkout(request, USER_ID);

        assertThat(car.getStock()).isEqualTo(3);
        assertThat(response.status()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(response.userId()).isEqualTo(USER_ID);
        assertThat(response.totalAmount()).isEqualByComparingTo("45000.00");
        assertThat(response.items()).hasSize(1);
        assertThat(response.items().get(0).quantity()).isEqualTo(2);
        assertThat(response.items().get(0).lineTotal()).isEqualByComparingTo("45000.00");
        verify(cartService).clearCart(USER_ID);
        verify(outboxService).recordOrderEvent(any(Order.class), eq("ORDER_CREATED"));
    }

    @Test
    void checkoutThrowsInsufficientStockWithoutPersisting() {
        Car car = car(7L, 1, new BigDecimal("22500.00"));
        when(carRepository.findWithLockingById(7L)).thenReturn(Optional.of(car));

        CheckoutRequest request = new CheckoutRequest(List.of(new CheckoutItemRequest(7L, 2)));

        assertThatThrownBy(() -> orderService.checkout(request, USER_ID))
                .isInstanceOf(InsufficientStockException.class)
                .hasMessageContaining("Stock insuficiente");

        assertThat(car.getStock()).isEqualTo(1);
        verify(orderRepository, never()).save(any());
        verify(cartService, never()).clearCart(any());
        verify(outboxService, never()).recordOrderEvent(any(), any());
    }

    @Test
    void checkoutMergesDuplicatedLinesOfSameCar() {
        Car car = car(10L, 10, new BigDecimal("10000.00"));
        when(carRepository.findWithLockingById(10L)).thenReturn(Optional.of(car));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CheckoutRequest request = new CheckoutRequest(
                List.of(new CheckoutItemRequest(10L, 2), new CheckoutItemRequest(10L, 3)));

        OrderResponse response = orderService.checkout(request, USER_ID);

        verify(carRepository).findWithLockingById(10L);
        assertThat(response.items()).hasSize(1);
        assertThat(response.items().get(0).quantity()).isEqualTo(5);
        assertThat(car.getStock()).isEqualTo(5);
        assertThat(response.totalAmount()).isEqualByComparingTo("50000.00");
    }

    @Test
    void checkoutFailsFastWhenCarDoesNotExist() {
        when(carRepository.findWithLockingById(99L)).thenReturn(Optional.empty());

        CheckoutRequest request = new CheckoutRequest(List.of(new CheckoutItemRequest(99L, 1)));

        assertThatThrownBy(() -> orderService.checkout(request, USER_ID))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(orderRepository, never()).save(any());
    }

    @Test
    void listForUserReturnsOnlyOwnOrdersNewestFirst() {
        Order order = order(5L, USER_ID, OrderStatus.CONFIRMED);
        when(orderRepository.findByUserIdOrderByCreatedAtDesc(USER_ID)).thenReturn(List.of(order));

        List<OrderResponse> result = orderService.listForUser(USER_ID);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo(5L);
    }

    @Test
    void getForUserWhenOrderBelongsToAnotherUserThrowsNotFound() {
        when(orderRepository.findByIdAndUserId(9L, USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.getForUser(9L, principal(USER_ID, "CUSTOMER")))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void adminCanConfirmPendingOrder() {
        Order order = order(11L, 99L, OrderStatus.PENDING);
        when(orderRepository.findById(11L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OrderResponse response = orderService.changeStatus(
                11L, OrderStatus.CONFIRMED, principal(1L, "ADMIN"));

        assertThat(response.status()).isEqualTo(OrderStatus.CONFIRMED);
        verify(outboxService).recordOrderEvent(any(Order.class), eq("ORDER_STATUS_CHANGED"));
    }

    @Test
    void customerCannotConfirmOwnOrder() {
        Order order = order(11L, USER_ID, OrderStatus.PENDING);
        when(orderRepository.findByIdAndUserId(11L, USER_ID)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.changeStatus(
                11L, OrderStatus.CONFIRMED, principal(USER_ID, "CUSTOMER")))
                .isInstanceOf(AccessDeniedException.class);

        verify(orderRepository, never()).save(any());
    }

    @Test
    void invalidTransitionReturnsConflict() {
        Order order = order(12L, USER_ID, OrderStatus.CANCELLED);
        when(orderRepository.findByIdAndUserId(12L, USER_ID)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.changeStatus(
                12L, OrderStatus.CANCELLED, principal(USER_ID, "CUSTOMER")))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Transición de estado inválida");
    }

    /**
     * Criterio Eje B (patrón State): TODA transición inválida se rechaza con
     * ConflictException (→ 409 en GlobalExceptionHandler), sin persistir ni
     * publicar eventos. Se prueba con ADMIN para que no la corte la regla de rol.
     */
    @ParameterizedTest(name = "{0} → {1} es inválida")
    @CsvSource({
            "PENDING,   PENDING",
            "CONFIRMED, CONFIRMED",
            "CONFIRMED, PENDING",
            "CANCELLED, PENDING",
            "CANCELLED, CONFIRMED",
            "CANCELLED, CANCELLED"
    })
    void everyInvalidTransitionIsRejectedWithConflict(OrderStatus from, OrderStatus to) {
        Order order = order(40L, USER_ID, from);
        when(orderRepository.findById(40L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.changeStatus(40L, to, principal(1L, "ADMIN")))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Transición de estado inválida: " + from + " → " + to);

        assertThat(order.getStatus()).isEqualTo(from);
        verify(orderRepository, never()).save(any(Order.class));
        verify(outboxService, never()).recordOrderEvent(any(), any());
    }

    @Test
    void cancellingOrderRestoresStock() {
        Car car = car(7L, 2, new BigDecimal("22500.00"));
        Order order = order(13L, USER_ID, OrderStatus.CONFIRMED);
        order.addItem(OrderItem.builder().car(car).quantity(3).unitPrice(car.getPrice()).build());
        when(orderRepository.findByIdAndUserId(13L, USER_ID)).thenReturn(Optional.of(order));
        when(carRepository.findWithLockingById(7L)).thenReturn(Optional.of(car));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OrderResponse response = orderService.changeStatus(
                13L, OrderStatus.CANCELLED, principal(USER_ID, "CUSTOMER"));

        assertThat(response.status()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(car.getStock()).isEqualTo(5);
    }

    @Test
    void adminCancelsOrderOfAnotherUser() {
        Order order = order(14L, 99L, OrderStatus.PENDING);
        when(orderRepository.findById(14L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OrderResponse response = orderService.changeStatus(
                14L, OrderStatus.CANCELLED, principal(1L, "ADMIN"));

        assertThat(response.status()).isEqualTo(OrderStatus.CANCELLED);
    }

    private static UserPrincipal principal(Long id, String role) {
        return new UserPrincipal(id, "user" + id + "@driveai.test", role);
    }

    private static Order order(Long id, Long userId, OrderStatus status) {
        return Order.builder()
                .id(id)
                .userId(userId)
                .status(status)
                .totalAmount(new BigDecimal("22500.00"))
                .build();
    }

    private static Car car(Long id, int stock, BigDecimal price) {
        return Car.builder()
                .id(id)
                .brand("Toyota")
                .model("Corolla")
                .year(2022)
                .price(price)
                .mileage(10000)
                .fuelType(FuelType.GASOLINA)
                .fuelConsumption(new BigDecimal("5.80"))
                .transmission(Transmission.AUTOMATICA)
                .bodyType(BodyType.SEDAN)
                .imageUrl("https://images.unsplash.com/photo-test.jpg")
                .description("Auto de prueba")
                .stock(stock)
                .build();
    }
}
