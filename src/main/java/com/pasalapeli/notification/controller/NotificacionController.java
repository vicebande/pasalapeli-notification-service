package com.pasalapeli.notification.controller;

import com.pasalapeli.notification.dto.CountDTO;
import com.pasalapeli.notification.dto.NotificacionDTO;
import com.pasalapeli.notification.service.NotificacionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * API interna de notificaciones. Es consumida por el BFF, que resuelve el
 * usuario autenticado y propaga su id. No se expone directamente al navegador.
 */
@RestController
@RequestMapping("/api/notificaciones")
@RequiredArgsConstructor
@Slf4j
public class NotificacionController {

    private final NotificacionService notificacionService;

    @GetMapping("/usuario/{usuarioId}")
    public ResponseEntity<List<NotificacionDTO>> listar(@PathVariable Long usuarioId) {
        return ResponseEntity.ok(notificacionService.listarPorUsuario(usuarioId));
    }

    @GetMapping("/usuario/{usuarioId}/no-leidas/count")
    public ResponseEntity<CountDTO> contarNoLeidas(@PathVariable Long usuarioId) {
        return ResponseEntity.ok(CountDTO.builder()
                .count(notificacionService.contarNoLeidas(usuarioId))
                .build());
    }

    @PostMapping("/{id}/leer")
    public ResponseEntity<NotificacionDTO> marcarLeida(@PathVariable Long id,
                                                       @RequestParam Long usuarioId) {
        return ResponseEntity.ok(notificacionService.marcarLeida(id, usuarioId));
    }

    @PostMapping("/usuario/{usuarioId}/leer-todas")
    public ResponseEntity<Map<String, Object>> marcarTodasLeidas(@PathVariable Long usuarioId) {
        int actualizadas = notificacionService.marcarTodasLeidas(usuarioId);
        return ResponseEntity.ok(Map.of(
                "actualizadas", actualizadas,
                "noLeidas", notificacionService.contarNoLeidas(usuarioId)));
    }
}
