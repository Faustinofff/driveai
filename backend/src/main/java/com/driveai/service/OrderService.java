package com.driveai.service;

import com.driveai.domain.enums.OrderStatus;
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
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Checkout transaccional y ciclo de vida de la orden.
 *
 * <p>El checkout usa un lock pesimista PESSIMISTIC_WRITE por auto para evitar
 * sobreventa en concurrencia y descuenta inventario dentro de la misma
 * transacción. Además escribe un evento en la tabla outbox en esa misma
 * transacción (no se pierde si el broker está caído).
 *
 * <p>Las órdenes pertenecen al usuario del JWT. Un usuario no puede leer ni
 * modificar órdenes ajenas (404 para no filtrar existencia).
 */
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final CarRepository carRepository;
    private final CartService cartService;
    private final OutboxService outboxService;

    @Transactional
    public OrderResponse checkout(CheckoutRequest request, Long userId) {
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
                .userId(userId)
                .status(OrderStatus.CONFIRMED)
                .totalAmount(total)
                .build();
        orderItems.forEach(order::addItem);

        Order saved = orderRepository.save(order);
        outboxService.recordOrderEvent(saved, "ORDER_CREATED");
        cartService.clearCart(userId);

        return OrderResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> listForUser(Long userId) {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(OrderResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public OrderResponse getForUser(Long orderId, UserPrincipal principal) {
        Order order = orderRepository.findByIdAndUserId(orderId, principal.id())
                .orElseThrow(() -> new ResourceNotFoundException("Orden", orderId));
        return OrderResponse.from(order);
    }

    /**
     * Aplica una transición de estado validada por {@link OrderStatus}.
     *
     * @throws ConflictException     si la transición es inválida (HTTP 409)
     * @throws AccessDeniedException si un CUSTOMER intenta algo distinto de cancelar su orden
     */
    @Transactional
    public OrderResponse changeStatus(Long orderId, OrderStatus target, UserPrincipal principal) {
        boolean admin = "ADMIN".equals(principal.role());
        Order order = (admin
                ? orderRepository.findById(orderId)
                : orderRepository.findByIdAndUserId(orderId, principal.id()))
                .orElseThrow(() -> new ResourceNotFoundException("Orden", orderId));

        if (!admin && target != OrderStatus.CANCELLED) {
            throw new AccessDeniedException("Solo un administrador puede confirmar órdenes.");
        }

        OrderStatus current = order.getStatus();
        if (!current.canTransitionTo(target)) {
            throw new ConflictException(
                    "Transición de estado inválida: " + current + " → " + target + ".");
        }

        if (target == OrderStatus.CANCELLED) {
            restoreStock(order);
        }

        order.setStatus(target);
        Order saved = orderRepository.save(order);
        outboxService.recordOrderEvent(saved, "ORDER_STATUS_CHANGED");
        return OrderResponse.from(saved);
    }

    private void restoreStock(Order order) {
        for (OrderItem item : order.getItems()) {
            Car car = carRepository.findWithLockingById(item.getCar().getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Auto", item.getCar().getId()));
            car.setStock(car.getStock() + item.getQuantity());
        }
    }
}
