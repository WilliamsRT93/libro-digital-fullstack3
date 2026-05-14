package com.colegio.bff;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.beans.factory.annotation.Value;

/**
 * Backend For Frontend.
 * Agrega y adapta los datos provenientes de los microservicios para la UI React.
 */
@SpringBootApplication
public class BffApplication {

    public static void main(String[] args) {
        SpringApplication.run(BffApplication.class, args);
    }

    @Bean
    public WebClient gatewayClient(@Value("${gateway.url}") String baseUrl) {
        // WebClient reactivo apuntando al API Gateway, reutilizable como bean singleton.
        return WebClient.builder().baseUrl(baseUrl).build();
    }
}
