package com.pasalapeli.notification.dto;

import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificacionDTO {
    private Long id;
    private String tipo;
    private String titulo;
    private String mensaje;
    private String referencia;
    private boolean leida;
    private LocalDateTime fechaCreacion;
}
