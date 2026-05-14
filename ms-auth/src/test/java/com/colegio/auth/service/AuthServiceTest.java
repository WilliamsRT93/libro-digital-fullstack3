package com.colegio.auth.service;

import com.colegio.auth.dto.LoginRequest;
import com.colegio.auth.dto.LoginResponse;
import com.colegio.auth.entity.Role;
import com.colegio.auth.entity.UserAccount;
import com.colegio.auth.repository.UserRepository;
import com.colegio.auth.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias para el caso de uso de autenticacion.
 * Se aíslan las dependencias externas (repositorio, encoder, JwtService) con Mockito.
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    // Usuario de prueba reutilizable entre tests
    private UserAccount usuarioActivo;

    @BeforeEach
    void configurar() {
        usuarioActivo = UserAccount.builder()
                .id(1L)
                .username("docente1")
                .passwordHash("$2b$12$hashedPassword")
                .fullName("Docente Uno")
                .roles(Set.of(Role.DOCENTE))
                .enabled(true)
                .build();
    }

    /**
     * Test 1: Login exitoso con credenciales validas.
     * Verifica que se retorna un LoginResponse con token y datos correctos.
     */
    @Test
    @DisplayName("login_credencialesValidas_retornaTokenYDatosUsuario")
    void login_credencialesValidas_retornaTokenYDatosUsuario() {
        // Arrange: configurar mocks para simular autenticacion exitosa
        LoginRequest solicitud = new LoginRequest("docente1", "Docente123!");
        String tokenEsperado = "eyJhbGciOiJSUzI1NiJ9.token";

        when(userRepository.findByUsername("docente1")).thenReturn(Optional.of(usuarioActivo));
        when(passwordEncoder.matches("Docente123!", usuarioActivo.getPasswordHash())).thenReturn(true);
        when(jwtService.generateToken(usuarioActivo)).thenReturn(tokenEsperado);
        when(jwtService.getExpirationSeconds()).thenReturn(3600L);

        // Act
        LoginResponse respuesta = authService.login(solicitud);

        // Assert
        assertThat(respuesta).isNotNull();
        assertThat(respuesta.token()).isEqualTo(tokenEsperado);
        assertThat(respuesta.username()).isEqualTo("docente1");
        assertThat(respuesta.roles()).containsExactly("DOCENTE");
        assertThat(respuesta.tokenType()).isEqualTo("Bearer");

        // Verificar interacciones: el token se genero exactamente una vez
        verify(jwtService, times(1)).generateToken(usuarioActivo);
    }

    /**
     * Test 2: Login con password incorrecto debe lanzar BadCredentialsException.
     * El sistema no debe revelar si el usuario existe; siempre retorna "Invalid credentials".
     */
    @Test
    @DisplayName("login_passwordIncorrecto_lanzaBadCredentialsException")
    void login_passwordIncorrecto_lanzaBadCredentialsException() {
        // Arrange
        LoginRequest solicitud = new LoginRequest("docente1", "passwordErroneo");

        when(userRepository.findByUsername("docente1")).thenReturn(Optional.of(usuarioActivo));
        when(passwordEncoder.matches("passwordErroneo", usuarioActivo.getPasswordHash())).thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> authService.login(solicitud))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Invalid credentials");

        // Verificar que nunca se intento generar un token
        verify(jwtService, never()).generateToken(any());
    }

    /**
     * Test 3: Login con usuario deshabilitado debe lanzar BadCredentialsException.
     * Un usuario con enabled=false no puede autenticarse aunque la contrasena sea correcta.
     */
    @Test
    @DisplayName("login_usuarioDeshabilitado_lanzaBadCredentialsException")
    void login_usuarioDeshabilitado_lanzaBadCredentialsException() {
        // Arrange: mismo usuario pero con enabled=false
        UserAccount usuarioInactivo = UserAccount.builder()
                .id(2L)
                .username("docente1")
                .passwordHash("$2b$12$hashedPassword")
                .fullName("Docente Inactivo")
                .roles(Set.of(Role.DOCENTE))
                .enabled(false)
                .build();

        LoginRequest solicitud = new LoginRequest("docente1", "Docente123!");
        when(userRepository.findByUsername("docente1")).thenReturn(Optional.of(usuarioInactivo));

        // Act & Assert
        assertThatThrownBy(() -> authService.login(solicitud))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("User disabled");

        // Un usuario deshabilitado no deberia llegar al paso de verificacion de password
        verify(passwordEncoder, never()).matches(any(), any());
        verify(jwtService, never()).generateToken(any());
    }
}
