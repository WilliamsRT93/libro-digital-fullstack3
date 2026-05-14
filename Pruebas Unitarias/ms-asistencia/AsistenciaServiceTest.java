package com.colegio.asistencia.service;

import com.colegio.asistencia.dto.AsistenciaRequest;
import com.colegio.asistencia.dto.AsistenciaResponse;
import com.colegio.asistencia.entity.Asistencia;
import com.colegio.asistencia.entity.EstadoAsistencia;
import com.colegio.asistencia.repository.AsistenciaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias del servicio de asistencia.
 * Se verifica el flujo de persistencia y la logica de publicacion Kafka
 * segun el estado de asistencia registrado.
 */
@ExtendWith(MockitoExtension.class)
class AsistenciaServiceTest {

    @Mock
    private AsistenciaRepository repository;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    private AsistenciaService asistenciaService;

    /**
     * Inyectar el valor del @Value que normalmente proviene del application.yml.
     * En tests unitarios no hay contexto Spring, se inyecta manualmente con ReflectionTestUtils.
     */
    private static final String TOPIC_INASISTENCIA = "inasistencia.detectada";

    /**
     * Test 9: Registrar alumno PRESENTE no debe publicar ningun evento Kafka.
     * El bus de eventos solo se activa ante inasistencias para no saturar el broker.
     */
    @Test
    @DisplayName("registrar_estadoPresente_noPubicaEventoKafka")
    void registrar_estadoPresente_noPublicaEventoKafka() {
        // Arrange: configurar el topic via reflexion (simula @Value)
        ReflectionTestUtils.setField(asistenciaService, "inasistenciaTopic", TOPIC_INASISTENCIA);

        AsistenciaRequest solicitud = new AsistenciaRequest(
                5L, 2L, LocalDate.of(2026, 5, 14), EstadoAsistencia.PRESENTE, null
        );

        // Simular la entidad persistida que retorna el repositorio
        Asistencia entidadGuardada = Asistencia.builder()
                .id(100L)
                .alumnoId(5L)
                .cursoId(2L)
                .fecha(LocalDate.of(2026, 5, 14))
                .estado(EstadoAsistencia.PRESENTE)
                .observacion(null)
                .registradoPor(1L)
                .build();
        when(repository.save(any(Asistencia.class))).thenReturn(entidadGuardada);

        // Act
        AsistenciaResponse resultado = asistenciaService.registrar(solicitud, 1L);

        // Assert: se persiste correctamente
        assertThat(resultado).isNotNull();
        assertThat(resultado.estado()).isEqualTo(EstadoAsistencia.PRESENTE);
        assertThat(resultado.alumnoId()).isEqualTo(5L);

        // Kafka no debe recibir ningun mensaje para un alumno PRESENTE
        verify(kafkaTemplate, never()).send(anyString(), anyString(), any());
    }

    /**
     * Test 10: Registrar alumno AUSENTE debe publicar un InasistenciaEvent en Kafka.
     * Este evento activa la notificacion asincrona al apoderado via MS-Mensajeria.
     */
    @Test
    @DisplayName("registrar_estadoAusente_publicaEventoKafka")
    void registrar_estadoAusente_publicaEventoKafka() {
        // Arrange
        ReflectionTestUtils.setField(asistenciaService, "inasistenciaTopic", TOPIC_INASISTENCIA);

        AsistenciaRequest solicitud = new AsistenciaRequest(
                7L, 3L, LocalDate.of(2026, 5, 14), EstadoAsistencia.AUSENTE, "Sin justificacion"
        );

        Asistencia entidadGuardada = Asistencia.builder()
                .id(200L)
                .alumnoId(7L)
                .cursoId(3L)
                .fecha(LocalDate.of(2026, 5, 14))
                .estado(EstadoAsistencia.AUSENTE)
                .observacion("Sin justificacion")
                .registradoPor(1L)
                .build();
        when(repository.save(any(Asistencia.class))).thenReturn(entidadGuardada);

        // Act
        AsistenciaResponse resultado = asistenciaService.registrar(solicitud, 1L);

        // Assert: el registro retorna correctamente el estado AUSENTE
        assertThat(resultado.estado()).isEqualTo(EstadoAsistencia.AUSENTE);

        // Verificar que se publico exactamente un evento en el topic correcto
        // con la clave del alumnoId como partition key
        ArgumentCaptor<Object> eventoCaptor = ArgumentCaptor.forClass(Object.class);
        verify(kafkaTemplate, times(1))
                .send(eq(TOPIC_INASISTENCIA), eq("7"), eventoCaptor.capture());

        // Verificar que el payload contiene el ID del alumno correcto
        Object eventoCaptado = eventoCaptor.getValue();
        assertThat(eventoCaptado).hasFieldOrPropertyWithValue("alumnoId", 7L);
    }
}
