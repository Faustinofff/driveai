package com.driveai.controller;

import com.driveai.dto.auth.AuthResponse;
import com.driveai.dto.auth.LoginRequest;
import com.driveai.dto.auth.RegisterRequest;
import com.driveai.rate.LoginRateLimitService;
import com.driveai.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Autenticación", description = "Registro y login (JWT)")
public class AuthController {

    private final AuthService authService;
    private final LoginRateLimitService loginRateLimitService;

    @Operation(summary = "Registra un cliente y devuelve token + datos de usuario")
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @Operation(summary = "Inicia sesión y devuelve token + datos de usuario")
    @ApiResponse(responseCode = "429", description = "Demasiados intentos de login (fuerza bruta)")
    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request, HttpServletRequest http) {
        loginRateLimitService.check(clientIp(http), request.email());
        return authService.login(request);
    }

    private String clientIp(HttpServletRequest http) {
        String forwarded = http.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return http.getRemoteAddr() == null ? "unknown" : http.getRemoteAddr();
    }
}
