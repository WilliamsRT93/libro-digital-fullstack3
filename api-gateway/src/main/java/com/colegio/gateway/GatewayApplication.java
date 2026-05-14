package com.colegio.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * API Gateway centralizado.
 * Valida los JWT firmados con RS256 usando el JWKS publico expuesto por MS-Auth,
 * aplica rate limiting y enruta el trafico hacia los microservicios internos.
 */
@SpringBootApplication
public class GatewayApplication {
    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }
}
