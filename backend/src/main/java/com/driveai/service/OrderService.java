package com.driveai.service;

import com.driveai.domain.model.Car;
import com.driveai.domain.model.Order;
import com.driveai.domain.model.OrderItem;
import com.driveai.domain.enums.OrderStatus;
import com.driveai.dto.order.CheckoutItemRequest;
import com.driveai.dto.order.CheckoutRequest;
import com.driveai.dto.order.OrderResponse;
import com.driveai.exception.InsufficientStockException;
import com.driveai.exception.ResourceNotFoundException;
import com.driveai.repository.CarRepository;
import com.driveai.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Checkout transaccional. Emplea un lock pessimista PESSIMISTIC_WRITE por auto
 * para evitar sobreventa en concurrencia y descuenta inventario dentro de la
 * misma transacción.
 */
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final CarRepository carRepository;
    private final CartService cartService;

    @Transactional
    public OrderResponse checkout(CheckoutRequest request) {
        Map<Long, Integer> grouped = new LinkedHashMap<>();
        for (CheckoutItemRequest item : request.items()) {
            grouped.merge(item.carId(), item.quantity(), Integer::sum);
        }

        List<OrderItem> orderItems = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;

        for (Map.Entry<Long, Integer> entry : grouped.entrySet()) {
            Car car = carRepository.findWithLockingById(entry.getKey())
                    .orElseThrow(() -> new ResourceNotFoundException("Auto", entry.getKey()));

            int quantity = entry.getValue();
            if (car.getStock() < quantity) {
                throw new InsufficientStockException(car.getId(), quantity, car.getStock());
            }

            car.setStock(car.getStock() - quantity);
            total = total.add(car.getPrice().multiply(BigDecimal.valueOf(quantity)));

            OrderItem orderItem = OrderItem.builder()
                    .car(car)
                    .quantity(quantity)
                    .unitPrice(car.getPrice())
                    .build();
            orderItems.add(orderItem);
        }

        Order order = Order.builder()
                .sessionId(request.sessionId())
                .status(OrderStatus.CONFIRMED)
                .totalAmount(total)
                .build();
        orderItems.forEach(order::addItem);

        Order saved = orderRepository.save(order);
        cartService.clearCart(request.sessionId());

        return OrderResponse.from(saved);
    }
}