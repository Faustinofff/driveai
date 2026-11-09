package com.driveai.security;

import com.driveai.domain.enums.UserRole;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (!StringUtils.hasText(header) || !header.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = header.substring(7);
        Claims claims = null;
        try {
            claims = jwtService.parse(token);
        } catch (RuntimeException ignored) {
            // token inválido/expirado: se deja sin autenticar y el entry point responde 401.
        }

        if (claims != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            UserPrincipal principal = toPrincipal(claims);
            if (principal != null) {
                List<SimpleGrantedAuthority> authorities =
                        List.of(new SimpleGrantedAuthority("ROLE_" + principal.role()));
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(principal, null, authorities);
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Convierte los claims en una identidad válida. Un token firmado pero con
     * rol o uid ausente/ inválido no autentica (defensa en profundidad).
     */
    private UserPrincipal toPrincipal(Claims claims) {
        Object uidClaim = claims.get("uid");
        String role = claims.get("role", String.class);
        String email = claims.getSubject();
        if (!(uidClaim instanceof Number number) || email == null || role == null) {
            return null;
        }
        try {
            UserRole.valueOf(role);
        } catch (IllegalArgumentException ex) {
            return null;
        }
        return new UserPrincipal(number.longValue(), email, role);
    }
}
