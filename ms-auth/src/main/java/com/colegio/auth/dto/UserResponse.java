package com.colegio.auth.dto;

import com.colegio.auth.entity.Role;
import java.time.Instant;
import java.util.Set;

/**
 * DTO de salida que representa un usuario para el panel de administracion.
 * Nunca expone el hash de password.
 */
public record UserResponse(
        Long id,
        String username,
        String fullName,
        Set<Role> roles,
        boolean enabled,
        Instant createdAt
) {}
