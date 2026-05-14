package com.colegio.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO de entrada para validacion de credenciales.
 * Bean Validation aplica las restricciones en la frontera del controller.
 */
public record LoginRequest(
        @NotBlank @Size(min = 3, max = 80) String username,
        @NotBlank @Size(min = 6, max = 100) String password
) {}
