package com.pasalapeli.notification.controller;

import com.pasalapeli.notification.dto.DestinoDTO;
import com.pasalapeli.notification.dto.DestinoRequestDTO;
import com.pasalapeli.notification.dto.MensajeFallidoDTO;
import com.pasalapeli.notification.dto.ReglaDTO;
import com.pasalapeli.notification.dto.ReglaRequestDTO;
import com.pasalapeli.notification.service.DestinoService;
import com.pasalapeli.notification.service.MensajeFallidoService;
import com.pasalapeli.notification.service.ReglaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * API de administracion de la mensajeria: destinos, reglas de distribucion y
 * diagnostico de mensajes fallidos. El BFF la expone bajo /api/admin
 * protegida con rol ADMIN.
 */
@RestController
@RequestMapping("/api/notificaciones/admin")
@RequiredArgsConstructor
@Slf4j
public class AdminNotificacionController {

    private final DestinoService destinoService;
    private final ReglaService reglaService;
    private final MensajeFallidoService mensajeFallidoService;

    // ------------------------------ Destinos ------------------------------

    @GetMapping("/destinos")
    public ResponseEntity<List<DestinoDTO>> listarDestinos() {
        return ResponseEntity.ok(destinoService.listar());
    }

    @PostMapping("/destinos")
    public ResponseEntity<DestinoDTO> crearDestino(@Valid @RequestBody DestinoRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(destinoService.crear(request));
    }

    @DeleteMapping("/destinos/{id}")
    public ResponseEntity<Void> eliminarDestino(@PathVariable Long id) {
        destinoService.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    // ------------------------- Reglas de distribucion -------------------------

    @GetMapping("/reglas")
    public ResponseEntity<List<ReglaDTO>> listarReglas() {
        return ResponseEntity.ok(reglaService.listar());
    }

    @PostMapping("/reglas")
    public ResponseEntity<ReglaDTO> crearRegla(@Valid @RequestBody ReglaRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(reglaService.crear(request));
    }

    @DeleteMapping("/reglas/{id}")
    public ResponseEntity<Void> eliminarRegla(@PathVariable Long id) {
        reglaService.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    // --------------------------- Mensajes fallidos ---------------------------

    @GetMapping("/mensajes-fallidos")
    public ResponseEntity<List<MensajeFallidoDTO>> listarMensajesFallidos() {
        return ResponseEntity.ok(mensajeFallidoService.listar());
    }

    @DeleteMapping("/mensajes-fallidos/{id}")
    public ResponseEntity<Void> eliminarMensajeFallido(@PathVariable Long id) {
        mensajeFallidoService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
