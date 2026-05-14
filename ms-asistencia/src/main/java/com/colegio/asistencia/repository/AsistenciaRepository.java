package com.colegio.asistencia.repository;

import com.colegio.asistencia.entity.Asistencia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * Repositorio que abstrae el acceso a la persistencia de asistencias.
 * Spring Data JPA genera la implementacion en tiempo de ejecucion.
 */
@Repository
public interface AsistenciaRepository extends JpaRepository<Asistencia, Long> {

    List<Asistencia> findByAlumnoIdAndFechaBetween(Long alumnoId, LocalDate from, LocalDate to);

    boolean existsByAlumnoIdAndFecha(Long alumnoId, LocalDate fecha);
}
