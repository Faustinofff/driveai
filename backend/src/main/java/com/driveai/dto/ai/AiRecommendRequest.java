package com.driveai.dto.ai;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AiRecommendRequest(
        @NotBlank @Size(max = 1000) String prompt,
        @NotBlank @Size(max = 64) String sessionId) {
}