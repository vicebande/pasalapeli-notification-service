package com.pasalapeli.notification.controller;

import com.pasalapeli.notification.dto.CountDTO;
import com.pasalapeli.notification.dto.NotificacionDTO;
import com.pasalapeli.notification.dto.PageResponseDTO;
import com.pasalapeli.notification.service.NotificacionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * API interna de notificaciones. Es consumida por el BFF, que resuelve el
 * usuario autenticado y propaga su id. Ademas valida ownership contra el
 * principal autenticado (defensa en profundidad).
 */
@RestController
@RequestMapping("/api/notificaciones")
@RequiredArgsConstructor
@Slf4j
public class NotificacionController {

    private final NotificacionService notificacionService;

    @GetMapping("/usuario/{usuarioId}")
    public ResponseEntity<PageResponseDTO<NotificacionDTO>> listar(@PathVariable Long usuarioId,
                                                                   @RequestParam(defaultValue = "0") int page,
                                                                   @RequestParam(defaultValue = "20") int size,
                                                                   Authentication authentication) {
        // IDOR: solo el dueño (correo == principal) o un ADMIN puede ver la bandeja
        notificacionService.verificarAccesoAUsuario(authentication, usuarioId);
        return ResponseEntity.ok(notificacionService.listarPorUsuario(usuarioId, page, size));
    }

    @GetMapping("/usuario/{usuarioId}/no-leidas/count")
    public ResponseEntity<CountDTO> contarNoLeidas(@PathVariable Long usuarioId,
                                                   Authentication authentication) {
        notificacionService.verificarAccesoAUsuario(authentication, usuarioId);
        return ResponseEntity.ok(CountDTO.builder()
                .count(notificacionService.contarNoLeidas(usuarioId))
                .build());
    }

    @PostMapping("/{id}/leer")
    public ResponseEntity<NotificacionDTO> marcarLeida(@PathVariable Long id,
                                                       @RequestParam Long usuarioId,
                                                       Authentication authentication) {
        notificacionService.verificarAccesoAUsuario(authentication, usuarioId);
        return ResponseEntity.ok(notificacionService.marcarLeida(id, usuarioId));
    }

    @PostMapping("/usuario/{usuarioId}/leer-todas")
    public ResponseEntity<Map<String, Object>> marcarTodasLeidas(@PathVariable Long usuarioId,
                                                                 Authentication authentication) {
        notificacionService.verificarAccesoAUsuario(authentication, usuarioId);
        int actualizadas = notificacionService.marcarTodasLeidas(usuarioId);
        return ResponseEntity.ok(Map.of(
                "actualizadas", actualizadas,
                "noLeidas", notificacionService.contarNoLeidas(usuarioId)));
    }
}
