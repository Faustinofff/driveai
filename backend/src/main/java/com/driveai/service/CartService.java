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

/**
 * Carrito propiedad del usuario autenticado.
 *
 * <p>Todas las operaciones reciben el {@code userId} resuelto desde el JWT.
 * Un usuario no puede leer ni modificar el carrito de otro aunque conozca el
 * id de un ítem: el repositorio filtra siempre por {@code cart.userId}.
 */
@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final CarRepository carRepository;

    @Transactional(readOnly = true)
    public CartResponse getCart(Long userId) {
        return cartRepository.findByUserId(userId)
                .map(this::toResponse)
                .orElseGet(() -> new CartResponse(userId, List.of(), BigDecimal.ZERO));
    }

    @Transactional
    public CartResponse addToCart(CartAddRequest request, Long userId) {
        Car car = carRepository.findById(request.carId())
                .orElseThrow(() -> new ResourceNotFoundException("Auto", request.carId()));

        Cart cart = cartRepository.findByUserId(userId)
                .orElseGet(() -> {
                    Cart created = new Cart();
                    created.setUserId(userId);
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
    public CartResponse updateItem(Long itemId, Long userId, Integer quantity) {
        CartItem item = cartItemRepository.findByIdAndCart_UserId(itemId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Ítem de carrito", itemId));

        validateStock(item.getCar(), quantity);
        item.setQuantity(quantity);
        cartItemRepository.save(item);

        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Carrito", userId));
        return toResponse(cart);
    }

    @Transactional
    public CartResponse removeItem(Long itemId, Long userId) {
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Carrito", userId));

        boolean removed = cart.getItems().removeIf(item -> item.getId().equals(itemId));
        if (!removed) {
            throw new ResourceNotFoundException("Ítem de carrito", itemId);
        }
        cartRepository.save(cart);
        return toResponse(cart);
    }

    @Transactional
    public void clearCart(Long userId) {
        cartRepository.findByUserId(userId).ifPresent(cartRepository::delete);
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
        return new CartResponse(cart.getUserId(), items, subtotal);
    }
}
