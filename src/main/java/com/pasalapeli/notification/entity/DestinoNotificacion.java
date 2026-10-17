package com.pasalapeli.notification.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Destino de notificacion administrable.
 * El tipo define como se resuelven los usuarios destinatarios:
 *   TODOS          -> todos los usuarios registrados
 *   ROL            -> usuarios con el rol indicado en {@code valor}
 *   USUARIO        -> el usuario indicado en {@code valor}
 *   USUARIO_EVENTO -> el usuarioId que viaja en el propio evento
 */
@Entity
@Table(name = "DestinoNotificacion")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DestinoNotificacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String nombre;

    @Column(nullable = false, length = 30)
    private String tipo;

    @Column(length = 100)
    private String valor;

    @Column(length = 255)
    private String descripcion;

    @Column(nullable = false)
    private boolean activo;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion;
}
