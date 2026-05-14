package com.colegio.academico.repository;

import com.colegio.academico.entity.Nota;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repositorio JPA para la entidad Nota.
 */
@Repository
public interface NotaRepository extends JpaRepository<Nota, Long> {
    List<Nota> findByAlumnoIdOrderByAsignaturaAscRegistradoEnAsc(Long alumnoId);
}
