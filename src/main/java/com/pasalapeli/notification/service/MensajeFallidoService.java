package com.pasalapeli.notification.service;

import com.pasalapeli.notification.dto.MensajeFallidoDTO;
import com.pasalapeli.notification.entity.MensajeFallido;
import com.pasalapeli.notification.repository.MensajeFallidoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Registro y consulta de los mensajes derivados a la Dead-Letter Queue.
 * Permite diagnosticar y auditar los eventos que no pudieron procesarse.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MensajeFallidoService {

    private final MensajeFallidoRepository mensajeFallidoRepository;

    @Transactional(readOnly = true)
    public List<MensajeFallidoDTO> listar() {
        return mensajeFallidoRepository.findAllByOrderByFechaCreacionDesc()
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    @Transactional
    public MensajeFallidoDTO registrar(String cola, String payload, String error, int intentos) {
        MensajeFallido mensaje = MensajeFallido.builder()
                .cola(cola != null ? cola : "desconocida")
                .payload(payload)
                .error(error)
                .intentos(intentos)
                .fechaCreacion(LocalDateTime.now())
                .build();
        MensajeFallido guardado = mensajeFallidoRepository.save(mensaje);
        log.warn("Mensaje fallido registrado [id={}] cola={} intentos={}", guardado.getId(), cola, intentos);
        return toDto(guardado);
    }

    @Transactional
    public void eliminar(Long id) {
        mensajeFallidoRepository.deleteById(id);
    }

    private MensajeFallidoDTO toDto(MensajeFallido m) {
        return MensajeFallidoDTO.builder()
                .id(m.getId())
                .cola(m.getCola())
                .payload(m.getPayload())
                .error(m.getError())
                .intentos(m.getIntentos())
                .fechaCreacion(m.getFechaCreacion())
                .build();
    }
}
