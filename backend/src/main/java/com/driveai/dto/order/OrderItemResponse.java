package com.driveai.dto.order;

import com.driveai.domain.model.OrderItem;
import com.driveai.dto.car.CarResponse;

import java.math.BigDecimal;

public record OrderItemResponse(
        Long id,
        CarResponse car,
        Integer quantity,
        BigDecimal unitPrice,
        BigDecimal lineTotal) {

    public static OrderItemResponse from(OrderItem item) {
        return new OrderItemResponse(
                item.getId(),
                CarResponse.from(item.getCar()),
                item.getQuantity(),
                item.getUnitPrice(),
                item.getLineTotal());
    }
}