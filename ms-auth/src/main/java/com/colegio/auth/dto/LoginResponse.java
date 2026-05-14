package com.colegio.auth.dto;

import java.util.Set;

/**
 * DTO de salida que entrega el JWT y la identidad minima del usuario al BFF.
 */
public record LoginResponse(
        String token,
        String tokenType,
        long expiresInSeconds,
        String username,
        Set<String> roles
) {}
