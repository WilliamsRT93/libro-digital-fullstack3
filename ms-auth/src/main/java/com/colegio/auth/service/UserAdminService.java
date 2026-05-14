package com.colegio.auth.service;

import com.colegio.auth.dto.CreateUserRequest;
import com.colegio.auth.dto.UpdateRolesRequest;
import com.colegio.auth.dto.UserResponse;
import com.colegio.auth.entity.UserAccount;
import com.colegio.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.util.HashSet;
import java.util.List;

/**
 * Casos de uso administrativos sobre usuarios.
 * Solo invocable por usuarios con rol ADMIN (controlado en el controller).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserAdminService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public UserResponse crear(CreateUserRequest req) {
        if (userRepository.existsByUsername(req.username())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El usuario ya existe");
        }
        UserAccount u = UserAccount.builder()
                .username(req.username())
                .passwordHash(passwordEncoder.encode(req.password()))
                .fullName(req.fullName())
                .roles(new HashSet<>(req.roles()))
                .enabled(true)
                .build();
        UserAccount saved = userRepository.save(u);
        log.info("Usuario creado id={} username={} roles={}", saved.getId(), saved.getUsername(), saved.getRoles());
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<UserResponse> listar() {
        return userRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional
    public UserResponse actualizarRoles(Long id, UpdateRolesRequest req) {
        UserAccount u = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no existe"));
        u.setRoles(new HashSet<>(req.roles()));
        log.info("Roles actualizados para usuario id={} -> {}", id, req.roles());
        return toResponse(u);
    }

    @Transactional
    public UserResponse cambiarEstado(Long id, boolean enabled) {
        UserAccount u = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no existe"));
        u.setEnabled(enabled);
        log.info("Estado actualizado para usuario id={} enabled={}", id, enabled);
        return toResponse(u);
    }

    @Transactional
    public void eliminar(Long id) {
        if (!userRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no existe");
        }
        userRepository.deleteById(id);
        log.info("Usuario eliminado id={}", id);
    }

    private UserResponse toResponse(UserAccount u) {
        return new UserResponse(u.getId(), u.getUsername(), u.getFullName(),
                u.getRoles(), u.isEnabled(), u.getCreatedAt());
    }
}
