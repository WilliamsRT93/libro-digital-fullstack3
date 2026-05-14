package com.colegio.auth.controller;

import com.colegio.auth.dto.LoginRequest;
import com.colegio.auth.dto.LoginResponse;
import com.colegio.auth.security.JwtService;
import com.colegio.auth.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.interfaces.RSAPublicKey;
import java.util.Base64;
import java.util.Map;

/**
 * Superficie REST de MS-Auth.
 * Expone el endpoint de intercambio de credenciales y el endpoint de clave publica
 * consumido por el API Gateway (patron JWKS).
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final JwtService jwtService;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    /**
     * Endpoint minimo estilo JWKS. En produccion se recomienda una estructura JWKS completa.
     */
    @GetMapping("/.well-known/jwks.json")
    public Map<String, Object> jwks() {
        RSAPublicKey pub = (RSAPublicKey) jwtService.getPublicKey();
        return Map.of(
                "keys", new Object[]{
                        Map.of(
                                "kty", "RSA",
                                "alg", "RS256",
                                "use", "sig",
                                "n", Base64.getUrlEncoder().withoutPadding().encodeToString(pub.getModulus().toByteArray()),
                                "e", Base64.getUrlEncoder().withoutPadding().encodeToString(pub.getPublicExponent().toByteArray())
                        )
                }
        );
    }
}
