package com.driveai.dto.cart;

import java.math.BigDecimal;
import java.util.List;

public record CartResponse(
        String sessionId,
        List<CartItemResponse> items,
        BigDecimal subtotal) {
}