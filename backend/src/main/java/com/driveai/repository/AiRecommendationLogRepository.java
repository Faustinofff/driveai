package com.driveai.repository;

import com.driveai.domain.model.AiRecommendationLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiRecommendationLogRepository extends JpaRepository<AiRecommendationLog, Long> {
}