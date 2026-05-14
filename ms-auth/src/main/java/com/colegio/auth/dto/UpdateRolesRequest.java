package com.colegio.auth.dto;

import com.colegio.auth.entity.Role;
import jakarta.validation.constraints.NotEmpty;

import java.util.Set;

/**
 * DTO para reemplazar el conjunto de roles de un usuario.
 */
public record UpdateRolesRequest(@NotEmpty Set<Role> roles) {}
