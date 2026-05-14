package com.colegio.bff.controller;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * Punto unico de entrada para el frontend React.
 * Reenvia las llamadas al API Gateway y aplica circuit breaker para resiliencia downstream.
 */
@RestController
@RequestMapping("/bff")
@RequiredArgsConstructor
public class BffController {

    private final WebClient gatewayClient;

    @PostMapping("/login")
    @CircuitBreaker(name = "gatewayCB")
    public Mono<ResponseEntity<Map>> login(@RequestBody Map<String, String> body) {
        // Proxy del login: el BFF no procesa credenciales, solo las reenvia al Gateway.
        return gatewayClient.post()
                .uri("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .toEntity(Map.class);
    }

    @PostMapping("/asistencias")
    @CircuitBreaker(name = "gatewayCB")
    public Mono<ResponseEntity<Map>> registrarAsistencia(@RequestBody Map<String, Object> body,
                                                         @RequestHeader(HttpHeaders.AUTHORIZATION) String auth) {
        // Reenvio del header Authorization para que el Gateway valide el JWT.
        return gatewayClient.post()
                .uri("/api/asistencias")
                .header(HttpHeaders.AUTHORIZATION, auth)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .toEntity(Map.class);
    }

    @GetMapping("/notas/reporte-pdf")
    @CircuitBreaker(name = "gatewayCB")
    public Mono<ResponseEntity<byte[]>> reportePdf(@RequestParam Long alumnoId,
                                                   @RequestHeader(HttpHeaders.AUTHORIZATION) String auth) {
        // Devuelve el PDF como arreglo de bytes para que el frontend lo presente como descarga.
        return gatewayClient.get()
                .uri(uri -> uri.path("/api/notas/reporte-pdf").queryParam("alumnoId", alumnoId).build())
                .header(HttpHeaders.AUTHORIZATION, auth)
                .retrieve()
                .toEntity(byte[].class);
    }

    // ----- Gestion de usuarios (solo ADMIN) -----

    @GetMapping("/admin/users")
    @CircuitBreaker(name = "gatewayCB")
    public Mono<ResponseEntity<Object>> listarUsuarios(@RequestHeader(HttpHeaders.AUTHORIZATION) String auth) {
        return gatewayClient.get()
                .uri("/admin/users")
                .header(HttpHeaders.AUTHORIZATION, auth)
                .retrieve()
                .toEntity(Object.class);
    }

    @PostMapping("/admin/users")
    @CircuitBreaker(name = "gatewayCB")
    public Mono<ResponseEntity<Object>> crearUsuario(@RequestBody Map<String, Object> body,
                                                     @RequestHeader(HttpHeaders.AUTHORIZATION) String auth) {
        return gatewayClient.post()
                .uri("/admin/users")
                .header(HttpHeaders.AUTHORIZATION, auth)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .toEntity(Object.class);
    }

    @DeleteMapping("/admin/users/{id}")
    @CircuitBreaker(name = "gatewayCB")
    public Mono<ResponseEntity<Void>> eliminarUsuario(@PathVariable Long id,
                                                     @RequestHeader(HttpHeaders.AUTHORIZATION) String auth) {
        return gatewayClient.delete()
                .uri("/admin/users/" + id)
                .header(HttpHeaders.AUTHORIZATION, auth)
                .retrieve()
                .toBodilessEntity()
                .map(r -> ResponseEntity.status(r.getStatusCode()).<Void>build());
    }
}
