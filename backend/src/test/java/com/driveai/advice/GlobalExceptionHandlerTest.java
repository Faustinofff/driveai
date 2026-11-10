package com.driveai.advice;

import com.driveai.exception.InvalidRequestException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.firewall.RequestRejectedException;

import static org.assertj.core.api.Assertions.assertThat;

/** DEF-13: entradas inválidas detectadas fuera de Bean Validation responden 400, no 500. */
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void headerRejectedByFirewallIsBadRequestWithoutLeakingTheValue() {
        ResponseEntity<ProblemDetail> response = handler.handleRejectedRequest(new RequestRejectedException(
                "The request was rejected because the header: \"Idempotency-Key \" has a value \"\u0007\" that is not allowed."));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PROBLEM_JSON);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getDetail())
                .isEqualTo("La solicitud contiene caracteres no permitidos en un header o en la URL.")
                .doesNotContain("Idempotency-Key");
    }

    @Test
    void invalidRequestIsBadRequestWithItsMessage() {
        ResponseEntity<ProblemDetail> response = handler.handleInvalidRequest(
                new InvalidRequestException("Idempotency-Key no puede superar 255 caracteres."));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getDetail()).isEqualTo("Idempotency-Key no puede superar 255 caracteres.");
    }

    @Test
    void unexpectedRuntimeErrorIsStill500WithGenericMessage() {
        ResponseEntity<ProblemDetail> response = handler.handleUnexpected(new IllegalStateException("secreto interno"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().getDetail()).doesNotContain("secreto");
    }
}
