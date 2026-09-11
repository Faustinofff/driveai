package com.driveai.controller;

import com.driveai.dto.cart.CartAddRequest;
import com.driveai.dto.cart.CartResponse;
import com.driveai.dto.cart.CartUpdateRequest;
import com.driveai.service.CartService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
@Validated
@Tag(name = "Carrito", description = "Carrito anónimo identificado por sessionId")
public class CartController {

    private final CartService cartService;

    @Operation(summary = "Obtiene el carrito de una sesión (vacío si no existe)")
    @GetMapping
    public CartResponse get(@RequestParam @NotBlank @Size(max = 64) String sessionId) {
        return cartService.getCart(sessionId);
    }

    @Operation(summary = "Agrega un auto al carrito con precio congelado")
    @PostMapping("/add")
    public CartResponse add(@Valid @RequestBody CartAddRequest request) {
        return cartService.addToCart(request);
    }

    @Operation(summary = "Actualiza la cantidad de un ítem")
    @PutMapping("/update/{itemId}")
    public CartResponse update(
            @PathVariable Long itemId,
            @RequestParam @NotBlank @Size(max = 64) String sessionId,
            @Valid @RequestBody CartUpdateRequest request) {
        return cartService.updateItem(itemId, sessionId, request.quantity());
    }

    @Operation(summary = "Elimina un ítem del carrito")
    @DeleteMapping("/remove/{itemId}")
    public CartResponse remove(
            @PathVariable Long itemId,
            @RequestParam @NotBlank @Size(max = 64) String sessionId) {
        return cartService.removeItem(itemId, sessionId);
    }
}