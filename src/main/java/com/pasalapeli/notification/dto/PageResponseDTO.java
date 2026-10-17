package com.pasalapeli.notification.dto;

import java.util.List;

/**
 * Respuesta paginada generica para la bandeja de notificaciones (pagina infinita "cargar más").
 */
public record PageResponseDTO<T>(List<T> content, int page, int size, long totalElements, int totalPages) {
}
