package com.driveai.config;

import com.driveai.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * En el arranque crea (si hace falta) la cuenta ADMIN definida por entorno,
 * de modo que el panel de administración esté disponible sin migraciones de
 * credenciales versionadas.
 */
@Component
@RequiredArgsConstructor
public class AdminInitializer implements ApplicationRunner {

    private final AuthService authService;

    @Override
    public void run(ApplicationArguments args) {
        authService.ensureAdminAccount();
    }
}