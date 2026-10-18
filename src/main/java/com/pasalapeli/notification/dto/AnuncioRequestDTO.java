package com.pasalapeli.notification.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Cuerpo de un anuncio manual enviado por un administrador
 * (POST /api/notificaciones/admin/anuncios).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AnuncioRequestDTO {

    @NotBlank(message = "El titulo del anuncio es obligatorio")
    @Size(max = 150, message = "El titulo no puede superar 150 caracteres")
    private String titulo;

    @NotBlank(message = "El mensaje del anuncio es obligatorio")
    @Size(max = 500, message = "El mensaje no puede superar 500 caracteres")
    private String mensaje;
}
