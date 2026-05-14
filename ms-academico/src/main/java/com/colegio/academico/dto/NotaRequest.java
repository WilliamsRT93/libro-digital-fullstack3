package com.colegio.academico.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

/**
 * DTO de entrada para crear una nota. La escala de evaluacion es de 1.0 a 7.0.
 */
public record NotaRequest(
        @NotNull Long alumnoId,
        @NotNull Long cursoId,
        @NotBlank @Size(max = 80) String asignatura,
        @NotNull @DecimalMin("1.0") @DecimalMax("7.0") BigDecimal valor,
        @Size(max = 200) String descripcion
) {}
