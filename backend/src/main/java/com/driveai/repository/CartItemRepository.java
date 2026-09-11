package com.driveai.repository;

import com.driveai.domain.model.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    Optional<CartItem> findByIdAndCart_SessionId(Long id, String sessionId);
}