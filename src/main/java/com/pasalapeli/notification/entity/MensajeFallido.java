package com.pasalapeli.notification.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Registro de un mensaje que no pudo procesarse y fue derivado a la
 * Dead-Letter Queue. Permite diagnosticar fallas.
 */
@Entity
@Table(name = "MensajeFallido")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MensajeFallido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String cola;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String payload;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String error;

    @Column(nullable = false)
    private int intentos;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion;
}
