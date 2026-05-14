package com.colegio.asistencia.service;

import com.colegio.asistencia.dto.AsistenciaRequest;
import com.colegio.asistencia.dto.AsistenciaResponse;
import com.colegio.asistencia.dto.InasistenciaEvent;
import com.colegio.asistencia.entity.Asistencia;
import com.colegio.asistencia.entity.EstadoAsistencia;
import com.colegio.asistencia.repository.AsistenciaRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Orquestador de casos de uso del dominio de asistencia.
 * Persiste el registro y, cuando corresponde, publica un evento de dominio en Kafka.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AsistenciaService {

    private final AsistenciaRepository repository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${kafka.topics.inasistencia}")
    private String inasistenciaTopic;

    @Transactional
    public AsistenciaResponse registrar(AsistenciaRequest request, Long usuarioActual) {
        // Construccion de la entidad a partir del DTO de entrada.
        Asistencia entity = Asistencia.builder()
                .alumnoId(request.alumnoId())
                .cursoId(request.cursoId())
                .fecha(request.fecha())
                .estado(request.estado())
                .observacion(request.observacion())
                .registradoPor(usuarioActual)
                .build();

        Asistencia saved = repository.save(entity);
        log.info("Asistencia registrada id={} alumno={} estado={}", saved.getId(), saved.getAlumnoId(), saved.getEstado());

        // Si el estado es AUSENTE se dispara el evento de dominio para notificacion asincrona.
        if (EstadoAsistencia.AUSENTE.equals(saved.getEstado())) {
            publishInasistencia(saved);
        }

        return toResponse(saved);
    }

    @CircuitBreaker(name = "kafkaCB")
    private void publishInasistencia(Asistencia a) {
        // Publicacion del evento. El circuit breaker protege ante caidas del broker Kafka.
        InasistenciaEvent event = new InasistenciaEvent(a.getId(), a.getAlumnoId(), a.getCursoId(), a.getFecha(), a.getObservacion());
        kafkaTemplate.send(inasistenciaTopic, String.valueOf(a.getAlumnoId()), event);
        log.info("Evento {} publicado para alumno={}", inasistenciaTopic, a.getAlumnoId());
    }

    private AsistenciaResponse toResponse(Asistencia a) {
        return new AsistenciaResponse(a.getId(), a.getAlumnoId(), a.getCursoId(), a.getFecha(), a.getEstado(), a.getObservacion());
    }
}
