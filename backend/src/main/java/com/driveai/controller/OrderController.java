package com.driveai.controller;

import com.driveai.domain.enums.OrderStatus;
import com.driveai.dto.order.CheckoutRequest;
import com.driveai.dto.order.OrderResponse;
import com.driveai.dto.order.OrderStatusUpdateRequest;
import com.driveai.security.UserPrincipal;
import com.driveai.service.IdempotencyService;
import com.driveai.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Tag(name = "Órdenes", description = "Checkout transaccional, historial propio y transiciones de estado")
public class OrderController {

    private final OrderService orderService;
    private final IdempotencyService idempotencyService;

    @Operation(summary = "Procesa el checkout y crea una orden CONFIRMED (acepta Idempotency-Key)")
    @PostMapping("/checkout")
    public OrderResponse checkout(@AuthenticationPrincipal UserPrincipal principal,
                                  @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
                                  @Valid @RequestBody CheckoutRequest request) {
        return idempotencyService.execute(
                principal.id(),
                idempotencyKey,
                request,
                OrderResponse.class,
                () -> orderService.checkout(request, principal.id()));
    }

    @Operation(summary = "Lista las órdenes del usuario autenticado")
    @GetMapping
    public List<OrderResponse> list(@AuthenticationPrincipal UserPrincipal principal) {
        return orderService.listForUser(principal.id());
    }

    @Operation(summary = "Obtiene una orden propia (404 si es de otro usuario)")
    @GetMapping("/{id}")
    public OrderResponse detail(@AuthenticationPrincipal UserPrincipal principal,
                                @PathVariable Long id) {
        return orderService.getForUser(id, principal);
    }

    @Operation(summary = "Cambia el estado de una orden (ADMIN o cancelación propia); 409 si la transición es inválida")
    @PatchMapping("/{id}/status")
    public OrderResponse changeStatus(@AuthenticationPrincipal UserPrincipal principal,
                                      @PathVariable Long id,
                                      @Valid @RequestBody OrderStatusUpdateRequest request) {
        return orderService.changeStatus(id, request.status(), principal);
    }

    @Operation(summary = "Cancela una orden propia (409 si ya está cancelada)")
    @PostMapping("/{id}/cancel")
    public OrderResponse cancel(@AuthenticationPrincipal UserPrincipal principal,
                                @PathVariable Long id) {
        return orderService.changeStatus(id, OrderStatus.CANCELLED, principal);
    }
}
