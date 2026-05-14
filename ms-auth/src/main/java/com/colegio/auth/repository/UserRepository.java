package com.colegio.auth.repository;

import com.colegio.auth.entity.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Abstraccion de Repository Pattern sobre la capa de persistencia.
 * Spring Data JPA genera la implementacion en tiempo de ejecucion.
 */
@Repository
public interface UserRepository extends JpaRepository<UserAccount, Long> {

    Optional<UserAccount> findByUsername(String username);

    boolean existsByUsername(String username);
}
