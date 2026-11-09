package com.driveai.security;

import java.security.Principal;

/**
 * Identidad autenticada extraída del JWT. El {@code id} es el único dato que
 * usan los servicios para resolver la propiedad de recursos (carrito, órdenes).
 * Nunca se confía en ids enviados por el cliente.
 */
public record UserPrincipal(Long id, String email, String role) implements Principal {

    @Override
    public String getName() {
        return email;
    }
}
