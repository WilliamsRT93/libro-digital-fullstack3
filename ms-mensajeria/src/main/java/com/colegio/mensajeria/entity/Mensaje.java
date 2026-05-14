package com.colegio.mensajeria.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * Mensaje persistido entre actores del sistema (docente, apoderado, sistema).
 */
@Entity
@Table(name = "mensajes")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Mensaje {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false) private Long emisorId;
    @Column(nullable = false) private Long receptorId;
    @Column(length = 200)     private String asunto;
    @Column(length = 2000)    private String contenido;
    @Column(nullable = false) private boolean leido;
    private LocalDateTime enviadoEn;

    @PrePersist
    public void onCreate() {
        // Timestamp del momento en que el mensaje fue persistido.
        this.enviadoEn = LocalDateTime.now();
    }
}
