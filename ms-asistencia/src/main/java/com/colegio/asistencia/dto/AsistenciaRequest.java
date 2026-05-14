package com.colegio.asistencia.dto;

import com.colegio.asistencia.entity.EstadoAsistencia;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/**
 * DTO de entrada para registrar la asistencia de un alumno.
 * Las restricciones se validan en la frontera del controller.
 */
public record AsistenciaRequest(
        @NotNull Long alumnoId,
        @NotNull Long cursoId,
        @NotNull LocalDate fecha,
        @NotNull EstadoAsistencia estado,
        @Size(max = 250) String observacion
) {}
