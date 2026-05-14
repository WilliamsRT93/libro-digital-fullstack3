package com.colegio.academico.service;

import com.colegio.academico.dto.NotaRequest;
import com.colegio.academico.dto.NotaResponse;
import com.colegio.academico.entity.Nota;
import com.colegio.academico.repository.NotaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias del servicio de notas academicas.
 * Se valida la logica de persistencia y el mapeo entidad -> DTO sin depender de BD.
 */
@ExtendWith(MockitoExtension.class)
class NotaServiceTest {

    @Mock
    private NotaRepository repository;

    @InjectMocks
    private NotaService notaService;

    /**
     * Test 11: Crear nota con datos validos debe persistir la entidad y retornar el DTO correcto.
     * Valida que el mapeo de NotaRequest a Nota y de Nota a NotaResponse es correcto.
     */
    @Test
    @DisplayName("crear_notaValida_persisteYRetornaResponse")
    void crear_notaValida_persisteYRetornaResponse() {
        // Arrange: solicitud con una nota de 6.5 en Matematicas
        NotaRequest solicitud = new NotaRequest(
                10L, 2L, "Matematicas", new BigDecimal("6.5"), "Prueba final"
        );

        // Entidad que simula lo que retorna el repositorio al persistir
        Nota notaGuardada = Nota.builder()
                .id(50L)
                .alumnoId(10L)
                .cursoId(2L)
                .asignatura("Matematicas")
                .valor(new BigDecimal("6.5"))
                .descripcion("Prueba final")
                .registradoPor(1L)
                .registradoEn(LocalDateTime.now())
                .build();
        when(repository.save(any(Nota.class))).thenReturn(notaGuardada);

        // Act
        NotaResponse resultado = notaService.crear(solicitud, 1L);

        // Assert: el response refleja exactamente la nota persistida
        assertThat(resultado).isNotNull();
        assertThat(resultado.id()).isEqualTo(50L);
        assertThat(resultado.alumnoId()).isEqualTo(10L);
        assertThat(resultado.asignatura()).isEqualTo("Matematicas");
        assertThat(resultado.valor()).isEqualByComparingTo(new BigDecimal("6.5"));
        assertThat(resultado.descripcion()).isEqualTo("Prueba final");

        // Verificar que la nota se construyo con el usuarioActual como registradoPor
        ArgumentCaptor<Nota> captor = ArgumentCaptor.forClass(Nota.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getRegistradoPor()).isEqualTo(1L);
    }

    /**
     * Test 12: Consultar notas de un alumno delega correctamente al repositorio
     * y retorna la lista mapeada a DTO sin modificar el orden.
     */
    @Test
    @DisplayName("notasDeAlumno_alumnoConNotas_retornaListaOrdenada")
    void notasDeAlumno_alumnoConNotas_retornaListaOrdenada() {
        // Arrange: alumno 10 tiene dos notas en distintas asignaturas
        Nota nota1 = Nota.builder()
                .id(1L).alumnoId(10L).cursoId(2L).asignatura("Historia")
                .valor(new BigDecimal("5.5")).descripcion("Prueba 1")
                .registradoPor(1L).registradoEn(LocalDateTime.now()).build();

        Nota nota2 = Nota.builder()
                .id(2L).alumnoId(10L).cursoId(2L).asignatura("Matematicas")
                .valor(new BigDecimal("6.0")).descripcion("Prueba 2")
                .registradoPor(1L).registradoEn(LocalDateTime.now()).build();

        when(repository.findByAlumnoIdOrderByAsignaturaAscRegistradoEnAsc(10L))
                .thenReturn(List.of(nota1, nota2));

        // Act
        List<NotaResponse> resultado = notaService.notasDeAlumno(10L);

        // Assert: se retornan exactamente 2 notas con los valores correctos
        assertThat(resultado).hasSize(2);
        assertThat(resultado.get(0).asignatura()).isEqualTo("Historia");
        assertThat(resultado.get(1).asignatura()).isEqualTo("Matematicas");
        assertThat(resultado).extracting(NotaResponse::alumnoId)
                .containsOnly(10L);

        // Verificar que se consulto el repositorio con el alumnoId correcto
        verify(repository, times(1))
                .findByAlumnoIdOrderByAsignaturaAscRegistradoEnAsc(10L);
    }

    /**
     * Test 13: Consultar notas de alumno sin registros retorna lista vacia.
     * Evitar NullPointerException cuando el alumno no tiene calificaciones aun.
     */
    @Test
    @DisplayName("notasDeAlumno_alumnoSinNotas_retornaListaVacia")
    void notasDeAlumno_alumnoSinNotas_retornaListaVacia() {
        // Arrange: alumno nuevo sin ninguna nota registrada
        when(repository.findByAlumnoIdOrderByAsignaturaAscRegistradoEnAsc(99L))
                .thenReturn(List.of());

        // Act
        List<NotaResponse> resultado = notaService.notasDeAlumno(99L);

        // Assert
        assertThat(resultado).isNotNull();
        assertThat(resultado).isEmpty();
    }
}
