package com.driveai.dto.cart;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CartAddRequest(
        @NotBlank @Size(max = 64) String sessionId,
        @NotNull Long carId,
        @NotNull @Min(1) @Max(100) Integer quantity) {
}