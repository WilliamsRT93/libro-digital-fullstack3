package com.colegio.auth.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.Instant;
import java.util.Map;

/**
 * Traduccion centralizada de excepciones. Evita filtrar stack traces al cliente
 * y reduce el ruido en los logs para errores benignos como 404.
 */
@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Map<String, Object>> handleBadCredentials(BadCredentialsException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error("AUTH_001", "Invalid credentials"));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        return ResponseEntity.badRequest().body(error("VAL_001", ex.getBindingResult().getAllErrors().toString()));
    }

    /**
     * 404 transitorios (por ejemplo cuando Prometheus golpea /actuator/prometheus
     * y la dependencia micrometer-registry-prometheus aun no esta cargada en
     * un contenedor sin reconstruir). Los logueamos en DEBUG sin stack trace.
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNoResource(NoResourceFoundException ex) {
        log.debug("Recurso no encontrado: {}", ex.getResourcePath());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error("RES_404", "Recurso no encontrado: " + ex.getResourcePath()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneric(Exception ex) {
        log.error("Unhandled error", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error("SYS_001", "Internal error"));
    }

    private Map<String, Object> error(String code, String message) {
        return Map.of(
                "timestamp", Instant.now().toString(),
                "code", code,
                "message", message
        );
    }
}
