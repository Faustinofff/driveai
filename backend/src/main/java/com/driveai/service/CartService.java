package com.driveai.service;

import com.driveai.domain.model.Car;
import com.driveai.domain.model.Cart;
import com.driveai.domain.model.CartItem;
import com.driveai.dto.cart.CartAddRequest;
import com.driveai.dto.cart.CartItemResponse;
import com.driveai.dto.cart.CartResponse;
import com.driveai.exception.InsufficientStockException;
import com.driveai.exception.ResourceNotFoundException;
import com.driveai.repository.CarRepository;
import com.driveai.repository.CartItemRepository;
import com.driveai.repository.CartRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final CarRepository carRepository;

    @Transactional(readOnly = true)
    public CartResponse getCart(String sessionId) {
        return cartRepository.findBySessionId(sessionId)
                .map(this::toResponse)
                .orElseGet(() -> new CartResponse(sessionId, List.of(), BigDecimal.ZERO));
    }

    @Transactional
    public CartResponse addToCart(CartAddRequest request) {
        Car car = carRepository.findById(request.carId())
                .orElseThrow(() -> new ResourceNotFoundException("Auto", request.carId()));

        Cart cart = cartRepository.findBySessionId(request.sessionId())
                .orElseGet(() -> {
                    Cart created = new Cart();
                    created.setSessionId(request.sessionId());
                    return created;
                });

        CartItem existing = cart.getItems().stream()
                .filter(item -> item.getCar().getId().equals(car.getId()))
                .findFirst()
                .orElse(null);

        int newQuantity = (existing == null ? 0 : existing.getQuantity()) + request.quantity();
        validateStock(car, newQuantity);

        if (existing != null) {
            existing.setQuantity(newQuantity);
        } else {
            CartItem item = new CartItem();
            item.setCar(car);
            item.setQuantity(request.quantity());
            item.setPriceSnapshot(car.getPrice());
            cart.addItem(item);
        }

        Cart saved = cartRepository.save(cart);
        return toResponse(saved);
    }

    @Transactional
    public CartResponse updateItem(Long itemId, String sessionId, Integer quantity) {
        CartItem item = cartItemRepository.findByIdAndCartSessionId(itemId, sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Ítem de carrito", itemId));

        validateStock(item.getCar(), quantity);
        item.setQuantity(quantity);
        cartItemRepository.save(item);

        Cart cart = cartRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Carrito", sessionId));
        return toResponse(cart);
    }

    @Transactional
    public CartResponse removeItem(Long itemId, String sessionId) {
        Cart cart = cartRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Carrito", sessionId));

        boolean removed = cart.getItems().removeIf(item -> item.getId().equals(itemId));
        if (!removed) {
            throw new ResourceNotFoundException("Ítem de carrito", itemId);
        }
        cartRepository.save(cart);
        return toResponse(cart);
    }

    @Transactional
    public void clearCart(String sessionId) {
        cartRepository.findBySessionId(sessionId).ifPresent(cartRepository::delete);
    }

    private void validateStock(Car car, int quantity) {
        if (car.getStock() < quantity) {
            throw new InsufficientStockException(car.getId(), quantity, car.getStock());
        }
    }

    private CartResponse toResponse(Cart cart) {
        List<CartItemResponse> items = cart.getItems().stream()
                .map(CartItemResponse::from)
                .toList();
        BigDecimal subtotal = items.stream()
                .map(CartItemResponse::lineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new CartResponse(cart.getSessionId(), items, subtotal);
    }
}