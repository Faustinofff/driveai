package com.driveai.dto.cart;

import com.driveai.domain.model.CartItem;
import com.driveai.dto.car.CarResponse;

import java.math.BigDecimal;

public record CartItemResponse(
        Long id,
        CarResponse car,
        Integer quantity,
        BigDecimal priceSnapshot,
        BigDecimal lineTotal) {

    public static CartItemResponse from(CartItem item) {
        return new CartItemResponse(
                item.getId(),
                CarResponse.from(item.getCar()),
                item.getQuantity(),
                item.getPriceSnapshot(),
                item.getLineTotal());
    }
}