package com.colegio.mensajeria.service;

import com.colegio.mensajeria.entity.Mensaje;
import com.colegio.mensajeria.repository.MensajeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Consumidor del topic inasistencia.detectada.
 * Por cada evento crea un mensaje de notificacion dirigido al apoderado del alumno.
 * En un sistema real el apoderadoId se resolveria a traves de un servicio de directorio;
 * aqui se utiliza un mapeo simplificado a modo ilustrativo.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InasistenciaConsumer {

    private final MensajeRepository repository;

    @KafkaListener(topics = "${kafka.topics.inasistencia}", groupId = "mensajeria-group")
    public void onInasistencia(Map<String, Object> event) {
        // Deserializacion segura del payload con cast explicito.
        Long alumnoId = ((Number) event.get("alumnoId")).longValue();
        Object fecha = event.get("fecha");

        // Resolucion simplificada: el apoderado se asume con el mismo id que el alumno.
        Long apoderadoId = alumnoId;

        Mensaje notif = Mensaje.builder()
                .emisorId(0L) // 0 representa al sistema como emisor
                .receptorId(apoderadoId)
                .asunto("Notificacion de inasistencia")
                .contenido("Se ha registrado una inasistencia para el alumno " + alumnoId + " el dia " + fecha)
                .leido(false)
                .build();

        repository.save(notif);
        log.info("Notificacion de inasistencia persistida para apoderado={}", apoderadoId);
    }
}
