package com.pasalapeli.notification.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReglaRequestDTO {

    /** PELICULA_CREADA | PELICULA_ELIMINADA | TICKET_PAGADO | TICKET_DEVUELTO */
    @NotBlank(message = "El evento es obligatorio")
    private String evento;

    @NotNull(message = "El destinoId es obligatorio")
    private Long destinoId;

    private Boolean activo;
}
