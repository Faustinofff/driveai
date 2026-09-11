package com.driveai.dto.order;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CheckoutRequest(
        @NotBlank @Size(max = 64) String sessionId,
        @NotEmpty List<@Valid CheckoutItemRequest> items) {
}