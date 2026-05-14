package com.colegio.auth.dto;

import com.colegio.auth.entity.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.Set;

/**
 * DTO de entrada para que un ADMIN cree un nuevo usuario.
 * El password viene en texto plano y MS-Auth lo persiste como hash BCrypt.
 */
public record CreateUserRequest(
        @NotBlank @Size(min = 3, max = 80) String username,
        @NotBlank @Size(min = 6, max = 100) String password,
        @NotBlank @Size(min = 3, max = 120) String fullName,
        @NotEmpty Set<Role> roles
) {}
