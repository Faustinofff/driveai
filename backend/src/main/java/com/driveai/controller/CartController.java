package com.driveai.controller;

import com.driveai.dto.cart.CartAddRequest;
import com.driveai.dto.cart.CartResponse;
import com.driveai.dto.cart.CartUpdateRequest;
import com.driveai.security.UserPrincipal;
import com.driveai.service.CartService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Carrito del usuario autenticado. El dueño se toma del JWT
 * ({@link UserPrincipal}); no se acepta ningún identificador de sesión o de
 * usuario enviado por el cliente.
 */
@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
@Validated
@Tag(name = "Carrito", description = "Carrito del usuario autenticado (propiedad derivada del JWT)")
public class CartController {

    private final CartService cartService;

    @Operation(summary = "Obtiene el carrito del usuario autenticado (vacío si no existe)")
    @GetMapping
    public CartResponse get(@AuthenticationPrincipal UserPrincipal principal) {
        return cartService.getCart(principal.id());
    }

    @Operation(summary = "Agrega un auto al carrito con precio congelado")
    @PostMapping("/add")
    public CartResponse add(@AuthenticationPrincipal UserPrincipal principal,
                            @Valid @RequestBody CartAddRequest request) {
        return cartService.addToCart(request, principal.id());
    }

    @Operation(summary = "Actualiza la cantidad de un ítem propio")
    @PutMapping("/update/{itemId}")
    public CartResponse update(@AuthenticationPrincipal UserPrincipal principal,
                               @PathVariable Long itemId,
                               @Valid @RequestBody CartUpdateRequest request) {
        return cartService.updateItem(itemId, principal.id(), request.quantity());
    }

    @Operation(summary = "Elimina un ítem propio del carrito")
    @DeleteMapping("/remove/{itemId}")
    public CartResponse remove(@AuthenticationPrincipal UserPrincipal principal,
                               @PathVariable Long itemId) {
        return cartService.removeItem(itemId, principal.id());
    }
}
