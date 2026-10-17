package com.pasalapeli.notification.dto;

import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DestinoDTO {
    private Long id;
    private String nombre;
    private String tipo;
    private String valor;
    private String descripcion;
    private boolean activo;
    private LocalDateTime fechaCreacion;
}
