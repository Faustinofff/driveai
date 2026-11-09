package com.driveai.dto.order;

import com.driveai.domain.enums.OrderStatus;
import jakarta.validation.constraints.NotNull;

public record OrderStatusUpdateRequest(
        @NotNull(message = "El estado es obligatorio") OrderStatus status) {
}
