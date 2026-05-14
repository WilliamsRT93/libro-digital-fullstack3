package com.colegio.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafka;

/**
 * Punto de entrada del microservicio de Autenticacion.
 * Emite tokens JWT (RS256) consumidos por el API Gateway y los demas microservicios.
 */
@EnableKafka
@SpringBootApplication
public class AuthApplication {
    public static void main(String[] args) {
        SpringApplication.run(AuthApplication.class, args);
    }
}
