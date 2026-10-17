package com.pasalapeli.notification.dto;

import lombok.*;

import java.math.BigDecimal;

/**
 * Evento de mensajeria publicado por los servicios productores
 * (movie-service / ticket-service) y consumido por este microservicio.
 * Un mismo formato cubre los tres casos de uso de la evaluacion.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventoNotificacionDTO {

    /** PELICULA_CREADA | PELICULA_ELIMINADA | TICKET_PAGADO | TICKET_DEVUELTO */
    private String tipo;

    private Long usuarioId;
    private Long peliculaId;
    private String tituloPelicula;
    private String codigoTicket;
    private Integer cantidad;
    private BigDecimal monto;
    private String sala;
}
