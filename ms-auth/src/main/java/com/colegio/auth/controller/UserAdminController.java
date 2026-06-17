package com.colegio.auth.controller;

import com.colegio.auth.dto.CreateUserRequest;
import com.colegio.auth.dto.UpdateRolesRequest;
import com.colegio.auth.dto.UserResponse;
import com.colegio.auth.service.UserAdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Endpoints administrativos para gestion de usuarios.
 * Solo accesibles con rol ADMIN. El frontend muestra esta seccion solo
 * cuando el JWT contiene el rol correspondiente.
 */
@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class UserAdminController {

    private final UserAdminService service;

    @PostMapping
    public ResponseEntity<UserResponse> crear(@Valid @RequestBody CreateUserRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.crear(request));
    }

    @GetMapping
    public ResponseEntity<List<UserResponse>> listar() {
        return ResponseEntity.ok(service.listar());
    }

    @PatchMapping("/{id}/roles")
    public ResponseEntity<UserResponse> actualizarRoles(@PathVariable Long id,
                                                       @Valid @RequestBody UpdateRolesRequest request) {
        return ResponseEntity.ok(service.actualizarRoles(id, request));
    }

    @PatchMapping("/{id}/estado")
    public ResponseEntity<UserResponse> cambiarEstado(@PathVariable Long id,
                                                     @RequestParam boolean enabled) {
        return ResponseEntity.ok(service.cambiarEstado(id, enabled));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        service.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
