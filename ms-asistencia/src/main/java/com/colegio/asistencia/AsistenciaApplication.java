package com.colegio.asistencia;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

/**
 * Microservicio de Asistencia. Es duenio del contexto delimitado de asistencia.
 */
@EnableKafka
@EnableMethodSecurity
@SpringBootApplication
public class AsistenciaApplication {
    public static void main(String[] args) {
        SpringApplication.run(AsistenciaApplication.class, args);
    }
}
