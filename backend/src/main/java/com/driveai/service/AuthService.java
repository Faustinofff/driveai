package com.driveai.service;

import com.driveai.config.AppProperties;
import com.driveai.domain.enums.UserRole;
import com.driveai.domain.model.AppUser;
import com.driveai.dto.auth.AuthResponse;
import com.driveai.dto.auth.LoginRequest;
import com.driveai.dto.auth.RegisterRequest;
import com.driveai.exception.ConflictException;
import com.driveai.repository.AppUserRepository;
import com.driveai.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AppProperties appProperties;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = request.email().toLowerCase().trim();
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ConflictException("Ya existe una cuenta con ese email.");
        }
        AppUser user = AppUser.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode(request.password()))
                .fullName(request.fullName().trim())
                .role(UserRole.CUSTOMER)
                .build();
        userRepository.save(user);
        return AuthResponse.of(user, jwtService.generateToken(user.getId(), user.getEmail(), user.getRole().name()));
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        AppUser user = userRepository.findByEmailIgnoreCase(request.email().trim())
                .orElseThrow(() -> new BadCredentialsException("Email o contraseña incorrectos."));
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BadCredentialsException("Email o contraseña incorrectos.");
        }
        return AuthResponse.of(user, jwtService.generateToken(user.getId(), user.getEmail(), user.getRole().name()));
    }

    /**
     * Asegura la cuenta admin inicial leyendo ADMIN_EMAIL/ADMIN_PASSWORD del entorno.
     * Sin credenciales configuradas no crea nada (y queda logueado un aviso).
     */
    @Transactional
    public void ensureAdminAccount() {
        String email = appProperties.admin().email();
        String password = appProperties.admin().password();
        if (email == null || email.isBlank() || password == null || password.isBlank()) {
            return;
        }
        String normalized = email.toLowerCase().trim();
        if (userRepository.existsByEmailIgnoreCase(normalized)) {
            return;
        }
        userRepository.save(AppUser.builder()
                .email(normalized)
                .passwordHash(passwordEncoder.encode(password))
                .fullName("Administrador")
                .role(UserRole.ADMIN)
                .build());
    }
}