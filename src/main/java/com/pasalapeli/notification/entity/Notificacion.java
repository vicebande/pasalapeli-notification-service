package com.pasalapeli.notification.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Notificacion in-app de un usuario. Cada registro es INDEPENDIENTE por usuario
 * (fan-out): un evento de difusion genera una fila por destinatario.
 */
@Entity
@Table(name = "Notificacion", indexes = {
        @Index(name = "idx_notificacion_usuario", columnList = "usuario_id"),
        @Index(name = "idx_notificacion_usuario_leida", columnList = "usuario_id,leida")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notificacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;

    /** Tipo de evento de origen: PELICULA_CREADA, TICKET_PAGADO, TICKET_DEVUELTO. */
    @Column(nullable = false, length = 40)
    private String tipo;

    @Column(nullable = false, length = 150)
    private String titulo;

    @Column(nullable = false, length = 500)
    private String mensaje;

    /** Referencia opcional (id de pelicula, codigo de ticket, etc.). */
    @Column(length = 100)
    private String referencia;

    @Column(nullable = false)
    private boolean leida;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion;
}
