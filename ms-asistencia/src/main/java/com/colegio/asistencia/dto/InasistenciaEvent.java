package com.colegio.asistencia.dto;

import java.time.LocalDate;

/**
 * Evento de dominio publicado en Kafka cuando un alumno se registra como ausente.
 * Es consumido por MS-Mensajeria para notificar a los apoderados.
 */
public record InasistenciaEvent(
        Long asistenciaId,
        Long alumnoId,
        Long cursoId,
        LocalDate fecha,
        String observacion
) {}
