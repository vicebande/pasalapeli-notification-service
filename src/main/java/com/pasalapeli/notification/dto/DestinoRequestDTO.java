package com.pasalapeli.notification.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DestinoRequestDTO {

    @NotBlank(message = "El nombre del destino es obligatorio")
    private String nombre;

    /** TODOS | ROL | USUARIO | USUARIO_EVENTO */
    @NotBlank(message = "El tipo de destino es obligatorio")
    private String tipo;

    private String valor;
    private String descripcion;
    private Boolean activo;
}
