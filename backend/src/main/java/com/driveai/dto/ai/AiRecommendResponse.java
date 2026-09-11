package com.driveai.dto.ai;

import com.driveai.dto.car.CarResponse;

import java.util.List;

public record AiRecommendResponse(
        String sessionId,
        String reasoning,
        List<CarResponse> recommendations) {
}