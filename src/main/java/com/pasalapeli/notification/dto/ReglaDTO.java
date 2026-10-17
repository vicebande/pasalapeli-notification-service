package com.pasalapeli.notification.dto;

import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReglaDTO {
    private Long id;
    private String evento;
    private Long destinoId;
    private String destinoNombre;
    private boolean activo;
    private LocalDateTime fechaCreacion;
}
