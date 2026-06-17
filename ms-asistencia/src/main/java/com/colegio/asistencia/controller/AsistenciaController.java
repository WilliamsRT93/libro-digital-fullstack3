package com.colegio.asistencia.controller;

import com.colegio.asistencia.dto.AsistenciaRequest;
import com.colegio.asistencia.dto.AsistenciaResponse;
import com.colegio.asistencia.service.AsistenciaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

/**
 * Controller REST de asistencia.
 * RBAC: solo los roles DOCENTE o ADMIN pueden registrar asistencia.
 */
@RestController
@RequestMapping("/api/v1/asistencias")
@RequiredArgsConstructor
public class AsistenciaController {

    private final AsistenciaService service;

    @PostMapping
    @PreAuthorize("hasAnyRole('DOCENTE','ADMIN')")
    public ResponseEntity<AsistenciaResponse> registrar(
            @Valid @RequestBody AsistenciaRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        // El claim "uid" es inyectado por MS-Auth al momento del login.
        Long uid = jwt.getClaim("uid");
        AsistenciaResponse response = service.registrar(request, uid);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
