package com.colegio.asistencia.dto;

import com.colegio.asistencia.entity.EstadoAsistencia;
import java.time.LocalDate;

/**
 * DTO de salida que entrega la asistencia persistida al cliente.
 */
public record AsistenciaResponse(
        Long id,
        Long alumnoId,
        Long cursoId,
        LocalDate fecha,
        EstadoAsistencia estado,
        String observacion
) {}
