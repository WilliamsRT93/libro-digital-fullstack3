package com.colegio.mensajeria;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

/**
 * Microservicio de Mensajeria.
 * Consume eventos de Kafka y persiste mensajes entre actores del sistema.
 */
@EnableKafka
@EnableMethodSecurity
@SpringBootApplication
public class MensajeriaApplication {
    public static void main(String[] args) {
        SpringApplication.run(MensajeriaApplication.class, args);
    }
}
