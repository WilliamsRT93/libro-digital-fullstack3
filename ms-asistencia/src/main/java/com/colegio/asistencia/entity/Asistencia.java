package com.colegio.asistencia.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Registro de asistencia por alumno y por sesion de clase.
 */
@Entity
@Table(name = "asistencias", indexes = {
        @Index(name = "idx_asist_alumno_fecha", columnList = "alumnoId,fecha")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Asistencia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long alumnoId;

    @Column(nullable = false)
    private Long cursoId;

    @Column(nullable = false)
    private LocalDate fecha;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoAsistencia estado;

    @Column(length = 250)
    private String observacion;

    @Column(nullable = false)
    private Long registradoPor;

    private LocalDateTime registradoEn;

    @PrePersist
    public void onCreate() {
        this.registradoEn = LocalDateTime.now();
    }
}
