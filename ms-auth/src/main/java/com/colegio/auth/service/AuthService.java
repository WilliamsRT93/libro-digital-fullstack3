package com.colegio.auth.service;

import com.colegio.auth.dto.LoginRequest;
import com.colegio.auth.dto.LoginResponse;
import com.colegio.auth.entity.UserAccount;
import com.colegio.auth.repository.UserRepository;
import com.colegio.auth.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;

/**
 * Caso de uso de autenticacion.
 * Nota: las contrasenas se almacenan como hash BCrypt; nunca registrar credenciales en texto plano.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public LoginResponse login(LoginRequest request) {
        UserAccount user = userRepository.findByUsername(request.username())
                .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));

        if (!user.isEnabled()) {
            throw new BadCredentialsException("User disabled");
        }

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            log.warn("Failed login attempt for username={}", request.username());
            throw new BadCredentialsException("Invalid credentials");
        }

        String token = jwtService.generateToken(user);
        log.info("User {} authenticated successfully", user.getUsername());

        return new LoginResponse(
                token,
                "Bearer",
                jwtService.getExpirationSeconds(),
                user.getUsername(),
                user.getRoles().stream().map(Enum::name).collect(Collectors.toSet())
        );
    }
}
