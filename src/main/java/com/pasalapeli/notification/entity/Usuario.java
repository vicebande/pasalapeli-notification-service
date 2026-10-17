package com.pasalapeli.notification.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Vista de solo lectura de la tabla Usuario (compartida en pasalapeli_db).
 * Se usa para resolver el fan-out de notificaciones de difusion.
 */
@Entity
@Table(name = "Usuario")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(nullable = false, unique = true, length = 100)
    private String correo;

    @Column(length = 255)
    private String password;

    /** Columna ENUM('ADMIN','CLIENTE') mapeada como texto. */
    @Column(nullable = false, length = 20)
    private String rol;
}
