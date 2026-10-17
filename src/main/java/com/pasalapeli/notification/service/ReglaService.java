package com.pasalapeli.notification.service;

import com.pasalapeli.notification.dto.ReglaDTO;
import com.pasalapeli.notification.dto.ReglaRequestDTO;
import com.pasalapeli.notification.entity.DestinoNotificacion;
import com.pasalapeli.notification.entity.ReglaDistribucion;
import com.pasalapeli.notification.exception.BusinessException;
import com.pasalapeli.notification.exception.ResourceNotFoundException;
import com.pasalapeli.notification.repository.DestinoNotificacionRepository;
import com.pasalapeli.notification.repository.ReglaDistribucionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/** CRUD encapsulado de reglas de distribucion. */
@Service
@RequiredArgsConstructor
@Slf4j
public class ReglaService {

    private static final Set<String> EVENTOS_VALIDOS = Set.of(
            "PELICULA_CREADA", "PELICULA_ELIMINADA", "TICKET_PAGADO", "TICKET_DEVUELTO");

    private final ReglaDistribucionRepository reglaRepository;
    private final DestinoNotificacionRepository destinoRepository;

    @Transactional(readOnly = true)
    public List<ReglaDTO> listar() {
        return reglaRepository.findAllByOrderByIdAsc().stream().map(this::toDto).collect(Collectors.toList());
    }

    @Transactional
    public ReglaDTO crear(ReglaRequestDTO request) {
        String evento = request.getEvento() == null ? "" : request.getEvento().trim().toUpperCase();
        if (!EVENTOS_VALIDOS.contains(evento)) {
            throw new BusinessException("Evento invalido. Valores permitidos: " + EVENTOS_VALIDOS);
        }
        DestinoNotificacion destino = destinoRepository.findById(request.getDestinoId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Destino no encontrado: " + request.getDestinoId()));
        if (reglaRepository.existsByEventoAndDestinoId(evento, destino.getId())) {
            throw new BusinessException("Ya existe una regla para el evento " + evento
                    + " con el destino '" + destino.getNombre() + "'");
        }

        ReglaDistribucion regla = ReglaDistribucion.builder()
                .evento(evento)
                .destinoId(destino.getId())
                .activo(request.getActivo() == null || request.getActivo())
                .fechaCreacion(LocalDateTime.now())
                .build();
        return toDto(reglaRepository.save(regla));
    }

    @Transactional
    public void eliminar(Long id) {
        ReglaDistribucion regla = reglaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Regla no encontrada: " + id));
        reglaRepository.delete(regla);
    }

    private ReglaDTO toDto(ReglaDistribucion r) {
        String destinoNombre = destinoRepository.findById(r.getDestinoId())
                .map(DestinoNotificacion::getNombre)
                .orElse("Desconocido");
        return ReglaDTO.builder()
                .id(r.getId())
                .evento(r.getEvento())
                .destinoId(r.getDestinoId())
                .destinoNombre(destinoNombre)
                .activo(r.isActivo())
                .fechaCreacion(r.getFechaCreacion())
                .build();
    }
}
