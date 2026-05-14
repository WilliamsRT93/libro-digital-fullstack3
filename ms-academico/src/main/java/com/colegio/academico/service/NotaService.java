package com.colegio.academico.service;

import com.colegio.academico.dto.NotaRequest;
import com.colegio.academico.dto.NotaResponse;
import com.colegio.academico.entity.Nota;
import com.colegio.academico.repository.NotaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Caso de uso del dominio academico. Encapsula la creacion y consulta de notas.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotaService {

    private final NotaRepository repository;

    @Transactional
    public NotaResponse crear(NotaRequest req, Long usuarioActual) {
        // Construccion de la entidad a partir del DTO de entrada.
        Nota nota = Nota.builder()
                .alumnoId(req.alumnoId())
                .cursoId(req.cursoId())
                .asignatura(req.asignatura())
                .valor(req.valor())
                .descripcion(req.descripcion())
                .registradoPor(usuarioActual)
                .build();
        Nota saved = repository.save(nota);
        log.info("Nota creada id={} alumno={} valor={}", saved.getId(), saved.getAlumnoId(), saved.getValor());
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<NotaResponse> notasDeAlumno(Long alumnoId) {
        // Consulta ordenada por asignatura y fecha de registro.
        return repository.findByAlumnoIdOrderByAsignaturaAscRegistradoEnAsc(alumnoId)
                .stream().map(this::toResponse).toList();
    }

    private NotaResponse toResponse(Nota n) {
        return new NotaResponse(n.getId(), n.getAlumnoId(), n.getCursoId(), n.getAsignatura(),
                n.getValor(), n.getDescripcion(), n.getRegistradoEn());
    }
}
