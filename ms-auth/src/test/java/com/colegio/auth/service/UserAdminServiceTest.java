package com.colegio.auth.service;

import com.colegio.auth.dto.CreateUserRequest;
import com.colegio.auth.dto.UpdateRolesRequest;
import com.colegio.auth.dto.UserResponse;
import com.colegio.auth.entity.Role;
import com.colegio.auth.entity.UserAccount;
import com.colegio.auth.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias para los casos de uso administrativos de usuarios.
 * Valida el comportamiento de creacion, consulta y modificacion sin levantar contexto Spring.
 */
@ExtendWith(MockitoExtension.class)
class UserAdminServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserAdminService userAdminService;

    // Entidad de usuario persistida, simulada en el repositorio
    private UserAccount usuarioGuardado;

    @BeforeEach
    void configurar() {
        usuarioGuardado = UserAccount.builder()
                .id(10L)
                .username("profe_nuevo")
                .passwordHash("$2b$12$hashEjemplo")
                .fullName("Profesor Nuevo")
                .roles(Set.of(Role.DOCENTE))
                .enabled(true)
                .build();
        // Simular que la BD asigna createdAt al persistir
        usuarioGuardado.setCreatedAt(Instant.now());
    }

    /**
     * Test 4: Crear usuario nuevo con datos validos.
     * Verifica que el servicio persiste la entidad y retorna un UserResponse correcto.
     */
    @Test
    @DisplayName("crear_usuarioNuevo_persisteYRetornaResponse")
    void crear_usuarioNuevo_persisteYRetornaResponse() {
        // Arrange
        CreateUserRequest solicitud = new CreateUserRequest(
                "profe_nuevo", "Secure123!", "Profesor Nuevo", Set.of(Role.DOCENTE)
        );
        String hashSimulado = "$2b$12$hashEjemplo";

        when(userRepository.existsByUsername("profe_nuevo")).thenReturn(false);
        when(passwordEncoder.encode("Secure123!")).thenReturn(hashSimulado);
        when(userRepository.save(any(UserAccount.class))).thenReturn(usuarioGuardado);

        // Act
        UserResponse resultado = userAdminService.crear(solicitud);

        // Assert: los datos del response coinciden con la entidad persistida
        assertThat(resultado).isNotNull();
        assertThat(resultado.id()).isEqualTo(10L);
        assertThat(resultado.username()).isEqualTo("profe_nuevo");
        assertThat(resultado.fullName()).isEqualTo("Profesor Nuevo");
        assertThat(resultado.roles()).containsExactly(Role.DOCENTE);
        assertThat(resultado.enabled()).isTrue();

        // Verificar que el password fue hasheado antes de persistir
        ArgumentCaptor<UserAccount> captor = ArgumentCaptor.forClass(UserAccount.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getPasswordHash()).isEqualTo(hashSimulado);
    }

    /**
     * Test 5: Crear usuario con nombre de usuario ya existente debe lanzar 409 CONFLICT.
     * Evita duplicados a nivel de servicio antes de llegar a la BD.
     */
    @Test
    @DisplayName("crear_usernameDuplicado_lanzaConflict409")
    void crear_usernameDuplicado_lanzaConflict409() {
        // Arrange: simular que el username ya esta tomado
        CreateUserRequest solicitud = new CreateUserRequest(
                "admin1", "Admin123!", "Admin Repetido", Set.of(Role.ADMIN)
        );
        when(userRepository.existsByUsername("admin1")).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> userAdminService.crear(solicitud))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("El usuario ya existe");

        // Verificar que nunca se llamo a save ni a encode
        verify(userRepository, never()).save(any());
        verify(passwordEncoder, never()).encode(anyString());
    }

    /**
     * Test 6: Listar todos los usuarios retorna la lista completa del repositorio.
     * Valida que el servicio mapea todas las entidades a DTO sin perder registros.
     */
    @Test
    @DisplayName("listar_existenUsuarios_retornaListaCompleta")
    void listar_existenUsuarios_retornaListaCompleta() {
        // Arrange: repositorio con dos usuarios pre-cargados
        UserAccount admin = UserAccount.builder()
                .id(1L).username("admin1").passwordHash("hash").fullName("Admin Uno")
                .roles(Set.of(Role.ADMIN)).enabled(true).createdAt(Instant.now()).build();

        UserAccount apoderado = UserAccount.builder()
                .id(2L).username("apoderado1").passwordHash("hash").fullName("Apoderado Uno")
                .roles(Set.of(Role.APODERADO)).enabled(true).createdAt(Instant.now()).build();

        when(userRepository.findAll()).thenReturn(List.of(admin, apoderado));

        // Act
        List<UserResponse> resultado = userAdminService.listar();

        // Assert
        assertThat(resultado).hasSize(2);
        assertThat(resultado).extracting(UserResponse::username)
                .containsExactlyInAnyOrder("admin1", "apoderado1");
    }

    /**
     * Test 7: Eliminar un usuario que no existe debe lanzar 404 NOT FOUND.
     * El servicio protege contra operaciones sobre IDs inexistentes.
     */
    @Test
    @DisplayName("eliminar_usuarioNoExiste_lanzaNotFound404")
    void eliminar_usuarioNoExiste_lanzaNotFound404() {
        // Arrange: el repositorio informa que el ID 999 no existe
        when(userRepository.existsById(999L)).thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> userAdminService.eliminar(999L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Usuario no existe");

        // Verificar que nunca se llamo a deleteById
        verify(userRepository, never()).deleteById(any());
    }

    /**
     * Test 8: Actualizar roles de usuario existente persiste el nuevo conjunto de roles.
     */
    @Test
    @DisplayName("actualizarRoles_usuarioExiste_actualizaYRetornaResponse")
    void actualizarRoles_usuarioExiste_actualizaYRetornaResponse() {
        // Arrange: usuario con rol DOCENTE que pasara a tener ADMIN tambien
        when(userRepository.findById(10L)).thenReturn(Optional.of(usuarioGuardado));

        UpdateRolesRequest solicitud = new UpdateRolesRequest(Set.of(Role.ADMIN, Role.DOCENTE));

        // Act
        UserResponse resultado = userAdminService.actualizarRoles(10L, solicitud);

        // Assert: los roles en el response reflejan la actualizacion
        assertThat(resultado.roles()).containsExactlyInAnyOrder(Role.ADMIN, Role.DOCENTE);
    }
}
