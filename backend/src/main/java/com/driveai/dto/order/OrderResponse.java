package com.driveai.dto.order;

import com.driveai.domain.enums.OrderStatus;
import com.driveai.domain.model.Order;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record OrderResponse(
        Long id,
        String sessionId,
        OrderStatus status,
        BigDecimal totalAmount,
        List<OrderItemResponse> items,
        Instant createdAt) {

    public static OrderResponse from(Order order) {
        List<OrderItemResponse> items = order.getItems().stream()
                .map(OrderItemResponse::from)
                .toList();
        return new OrderResponse(
                order.getId(),
                order.getSessionId(),
                order.getStatus(),
                order.getTotalAmount(),
                items,
                order.getCreatedAt());
    }
}