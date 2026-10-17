package com.pasalapeli.notification.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Regla de distribucion: asocia un evento de mensajeria con un destino.
 * Si un evento no tiene reglas activas, el consumidor aplica la distribucion
 * por defecto (usuario del evento o difusion a todos).
 */
@Entity
@Table(name = "ReglaDistribucion", indexes = {
        @Index(name = "idx_regla_evento", columnList = "evento")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReglaDistribucion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Evento de origen: PELICULA_CREADA, PELICULA_ELIMINADA, TICKET_PAGADO, TICKET_DEVUELTO. */
    @Column(nullable = false, length = 50)
    private String evento;

    @Column(name = "destino_id", nullable = false)
    private Long destinoId;

    @Column(nullable = false)
    private boolean activo;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion;
}
