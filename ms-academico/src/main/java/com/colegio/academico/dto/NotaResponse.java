package com.colegio.academico.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO de salida que representa una nota persistida.
 */
public record NotaResponse(
        Long id,
        Long alumnoId,
        Long cursoId,
        String asignatura,
        BigDecimal valor,
        String descripcion,
        LocalDateTime registradoEn
) {}
