package com.driveai.advice;

import com.driveai.exception.ConflictException;
import com.driveai.exception.GroqApiException;
import com.driveai.exception.InsufficientStockException;
import com.driveai.exception.PreconditionFailedException;
import com.driveai.exception.RateLimitExceededException;
import com.driveai.exception.ResourceNotFoundException;
import com.driveai.exception.SoapBusinessException;
import com.driveai.exception.SoapTechnicalException;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Traduce las excepciones de negocio/infraestructura a RFC 7807
 * (application/problem+json) con una estructura uniforme.
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleNotFound(ResourceNotFoundException ex) {
        return problem(HttpStatus.NOT_FOUND, "Recurso no encontrado", ex.getMessage(), null);
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ProblemDetail> handleConflict(ConflictException ex) {
        return problem(HttpStatus.CONFLICT, "Conflicto", ex.getMessage(), null);
    }

    @ExceptionHandler(PreconditionFailedException.class)
    public ResponseEntity<ProblemDetail> handlePreconditionFailed(PreconditionFailedException ex) {
        return problem(HttpStatus.PRECONDITION_FAILED, "Precondición fallida", ex.getMessage(), null);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ProblemDetail> handleAuthentication(AuthenticationException ex) {
        return problem(HttpStatus.UNAUTHORIZED, "Credenciales inválidas", ex.getMessage(), null);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ProblemDetail> handleAccessDenied(AccessDeniedException ex) {
        return problem(HttpStatus.FORBIDDEN, "Acceso denegado",
                "No tienes permisos para realizar esta operación.", null);
    }

    @ExceptionHandler(InsufficientStockException.class)
    public ResponseEntity<ProblemDetail> handleStock(InsufficientStockException ex) {
        Map<String, Object> properties = Map.of(
                "carId", ex.getCarId(),
                "requested", ex.getRequested(),
                "available", ex.getAvailable());
        return problem(HttpStatus.CONFLICT, "Stock insuficiente", ex.getMessage(), properties);
    }

    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<ProblemDetail> handleRateLimit(RateLimitExceededException ex) {
        ProblemDetail pd = build(HttpStatus.TOO_MANY_REQUESTS,
                "Demasiadas solicitudes", ex.getMessage(), null);
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.RETRY_AFTER, String.valueOf(ex.getRetryAfterSeconds()));
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .headers(headers)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(pd);
    }

    @ExceptionHandler(GroqApiException.class)
    public ResponseEntity<ProblemDetail> handleGroq(GroqApiException ex) {
        return problem(HttpStatus.SERVICE_UNAVAILABLE, "Servicio de IA no disponible", ex.getMessage(), null);
    }

    @ExceptionHandler(SoapBusinessException.class)
    public ResponseEntity<ProblemDetail> handleSoapBusiness(SoapBusinessException ex) {
        return problem(HttpStatus.CONFLICT, "Cotización no disponible", ex.getMessage(), null);
    }

    @ExceptionHandler(SoapTechnicalException.class)
    public ResponseEntity<ProblemDetail> handleSoapTechnical(SoapTechnicalException ex) {
        log.warn("Fallo técnico del servicio SOAP: {}", ex.getMessage());
        return problem(HttpStatus.SERVICE_UNAVAILABLE, "Servicio externo no disponible",
                "El servicio de cotización no está disponible en este momento.", null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> fields = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(fieldError -> fields.putIfAbsent(fieldError.getField(), fieldError.getDefaultMessage()));
        return problem(HttpStatus.BAD_REQUEST, "Solicitud inválida",
                "Revise los campos indicados.", Map.of("fields", fields));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ProblemDetail> handleConstraintViolation(ConstraintViolationException ex) {
        return problem(HttpStatus.BAD_REQUEST, "Solicitud inválida", ex.getMessage(), null);
    }

    @ExceptionHandler({MethodArgumentTypeMismatchException.class, HttpMessageNotReadableException.class})
    public ResponseEntity<ProblemDetail> handleBadRequest(Exception ex) {
        return problem(HttpStatus.BAD_REQUEST, "Solicitud inválida",
                "Formato o tipo de dato incorrecto en la solicitud.", null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleUnexpected(Exception ex) {
        log.error("Error inesperado al procesar la solicitud", ex);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno",
                "Ocurrió un error inesperado. Inténtalo de nuevo más tarde.", null);
    }

    private ResponseEntity<ProblemDetail> problem(HttpStatus status, String title,
                                                  String detail, Map<String, Object> properties) {
        ProblemDetail pd = build(status, title, detail, properties);
        return ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(pd);
    }

    private ProblemDetail build(HttpStatus status, String title,
                                String detail, Map<String, Object> properties) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, detail);
        pd.setTitle(title);
        if (properties != null) {
            properties.forEach(pd::setProperty);
        }
        return pd;
    }
}