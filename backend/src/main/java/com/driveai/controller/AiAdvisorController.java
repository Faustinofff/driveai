package com.driveai.controller;

import com.driveai.dto.ai.AiRecommendRequest;
import com.driveai.dto.ai.AiRecommendResponse;
import com.driveai.security.UserPrincipal;
import com.driveai.service.GroqAdvisorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
@Tag(name = "Asesor IA", description = "Recomendaciones de autos con Groq Cloud (con rate limit y anti-alucinación)")
public class AiAdvisorController {

    private final GroqAdvisorService advisorService;

    @Operation(summary = "Recomienda autos según un prompt en lenguaje natural")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Recomendaciones generadas"),
            @ApiResponse(responseCode = "429", description = "Límite de solicitudes por minuto excedido"),
            @ApiResponse(responseCode = "503", description = "Servicio de IA saturado o no disponible")
    })
    @PostMapping("/recommend")
    public AiRecommendResponse recommend(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody AiRecommendRequest request,
            HttpServletRequest http) {
        return advisorService.recommend(request.prompt(), request.sessionId(), rateLimitKey(principal, http));
    }

    /**
     * La cuota se ata a la identidad autenticada (JWT), nunca a datos que manda el
     * cliente. Antes se usaba {@code sessionId + X-Forwarded-For}: cambiando
     * cualquiera de los dos en cada request se evadía el límite (defecto DEF-08).
     */
    static String rateLimitKey(UserPrincipal principal, HttpServletRequest http) {
        if (principal != null && principal.id() != null) {
            return "user:" + principal.id();
        }
        String remote = http.getRemoteAddr();
        return "ip:" + (remote == null ? "unknown" : remote);
    }
}