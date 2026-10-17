package com.pasalapeli.notification.dto;

import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MensajeFallidoDTO {
    private Long id;
    private String cola;
    private String payload;
    private String error;
    private int intentos;
    private LocalDateTime fechaCreacion;
}
