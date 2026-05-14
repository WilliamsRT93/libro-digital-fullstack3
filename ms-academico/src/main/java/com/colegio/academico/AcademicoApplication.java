package com.colegio.academico;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

/**
 * Punto de entrada del microservicio Academico.
 * Encapsula el dominio de notas y la generacion de reportes PDF.
 */
@EnableMethodSecurity
@SpringBootApplication
public class AcademicoApplication {
    public static void main(String[] args) {
        SpringApplication.run(AcademicoApplication.class, args);
    }
}
