package com.driveai.controller;

import com.driveai.dto.order.CheckoutRequest;
import com.driveai.dto.order.OrderResponse;
import com.driveai.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Tag(name = "Órdenes", description = "Checkout transaccional con validación y descuento de stock")
public class OrderController {

    private final OrderService orderService;

    @Operation(summary = "Procesa el checkout y crea una orden CONFIRMED")
    @PostMapping("/checkout")
    public OrderResponse checkout(@Valid @RequestBody CheckoutRequest request) {
        return orderService.checkout(request);
    }
}