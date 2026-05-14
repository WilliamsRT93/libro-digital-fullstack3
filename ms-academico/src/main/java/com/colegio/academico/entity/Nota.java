package com.colegio.academico.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Registro de nota de un alumno en una asignatura.
 */
@Entity
@Table(name = "notas", indexes = {
        @Index(name = "idx_notas_alumno", columnList = "alumnoId"),
        @Index(name = "idx_notas_curso", columnList = "cursoId")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Nota {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long alumnoId;

    @Column(nullable = false)
    private Long cursoId;

    @Column(nullable = false, length = 80)
    private String asignatura;

    @Column(nullable = false, precision = 3, scale = 1)
    private BigDecimal valor;

    @Column(length = 200)
    private String descripcion;

    @Column(nullable = false)
    private Long registradoPor;

    private LocalDateTime registradoEn;

    @PrePersist
    public void onCreate() {
        this.registradoEn = LocalDateTime.now();
    }
}
