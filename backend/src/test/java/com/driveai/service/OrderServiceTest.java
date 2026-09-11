package com.driveai.service;

import com.driveai.domain.enums.BodyType;
import com.driveai.domain.enums.FuelType;
import com.driveai.domain.enums.OrderStatus;
import com.driveai.domain.enums.Transmission;
import com.driveai.domain.model.Car;
import com.driveai.domain.model.Order;
import com.driveai.dto.order.CheckoutItemRequest;
import com.driveai.dto.order.CheckoutRequest;
import com.driveai.dto.order.OrderResponse;
import com.driveai.exception.InsufficientStockException;
import com.driveai.exception.ResourceNotFoundException;
import com.driveai.repository.CarRepository;
import com.driveai.repository.OrderRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private CarRepository carRepository;
    @Mock
    private OrderRepository orderRepository;
    @Mock
    private CartService cartService;

    @InjectMocks
    private OrderService orderService;

    @Test
    void checkoutDeductsStockAndCreatesConfirmedOrder() {
        Car car = car(7L, 5, new BigDecimal("22500.00"));
        when(carRepository.findWithLockingById(7L)).thenReturn(Optional.of(car));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CheckoutRequest request = new CheckoutRequest("session-1",
                List.of(new CheckoutItemRequest(7L, 2)));

        OrderResponse response = orderService.checkout(request);

        assertThat(car.getStock()).isEqualTo(3);
        assertThat(response.status()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(response.totalAmount()).isEqualByComparingTo("45000.00");
        assertThat(response.items()).hasSize(1);
        assertThat(response.items().get(0).quantity()).isEqualTo(2);
        assertThat(response.items().get(0).lineTotal()).isEqualByComparingTo("45000.00");
        verify(cartService).clearCart("session-1");
    }

    @Test
    void checkoutThrowsInsufficientStockWithoutPersisting() {
        Car car = car(7L, 1, new BigDecimal("22500.00"));
        when(carRepository.findWithLockingById(7L)).thenReturn(Optional.of(car));

        CheckoutRequest request = new CheckoutRequest("session-1",
                List.of(new CheckoutItemRequest(7L, 2)));

        assertThatThrownBy(() -> orderService.checkout(request))
                .isInstanceOf(InsufficientStockException.class)
                .hasMessageContaining("Stock insuficiente");

        assertThat(car.getStock()).isEqualTo(1);
        verify(orderRepository, never()).save(any());
        verify(cartService, never()).clearCart(any());
    }

    @Test
    void checkoutMergesDuplicatedLinesOfSameCar() {
        Car car = car(10L, 10, new BigDecimal("10000.00"));
        when(carRepository.findWithLockingById(10L)).thenReturn(Optional.of(car));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CheckoutRequest request = new CheckoutRequest("session-2",
                List.of(new CheckoutItemRequest(10L, 2), new CheckoutItemRequest(10L, 3)));

        OrderResponse response = orderService.checkout(request);

        verify(carRepository).findWithLockingById(10L);
        assertThat(response.items()).hasSize(1);
        assertThat(response.items().get(0).quantity()).isEqualTo(5);
        assertThat(car.getStock()).isEqualTo(5);
        assertThat(response.totalAmount()).isEqualByComparingTo("50000.00");
    }

    @Test
    void checkoutFailsFastWhenCarDoesNotExist() {
        when(carRepository.findWithLockingById(99L)).thenReturn(Optional.empty());

        CheckoutRequest request = new CheckoutRequest("session-3",
                List.of(new CheckoutItemRequest(99L, 1)));

        assertThatThrownBy(() -> orderService.checkout(request))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(orderRepository, never()).save(any());
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