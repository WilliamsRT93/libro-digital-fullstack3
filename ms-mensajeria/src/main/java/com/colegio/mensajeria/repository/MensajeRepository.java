package com.colegio.mensajeria.repository;

import com.colegio.mensajeria.entity.Mensaje;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repositorio JPA para los mensajes del sistema.
 */
@Repository
public interface MensajeRepository extends JpaRepository<Mensaje, Long> {
    List<Mensaje> findByReceptorIdOrderByEnviadoEnDesc(Long receptorId);
}
