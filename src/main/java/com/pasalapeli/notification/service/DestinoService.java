package com.pasalapeli.notification.service;

import com.pasalapeli.notification.dto.DestinoDTO;
import com.pasalapeli.notification.dto.DestinoRequestDTO;
import com.pasalapeli.notification.entity.DestinoNotificacion;
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

/** CRUD encapsulado de destinos de notificacion. */
@Service
@RequiredArgsConstructor
@Slf4j
public class DestinoService {

    private static final Set<String> TIPOS_VALIDOS = Set.of("TODOS", "ROL", "USUARIO", "USUARIO_EVENTO");

    private final DestinoNotificacionRepository destinoRepository;
    private final ReglaDistribucionRepository reglaRepository;

    @Transactional(readOnly = true)
    public List<DestinoDTO> listar() {
        return destinoRepository.findAllByOrderByIdAsc().stream().map(this::toDto).collect(Collectors.toList());
    }

    @Transactional
    public DestinoDTO crear(DestinoRequestDTO request) {
        String nombre = request.getNombre() == null ? "" : request.getNombre().trim();
        String tipo = request.getTipo() == null ? "" : request.getTipo().trim().toUpperCase();

        if (nombre.isBlank()) {
            throw new BusinessException("El nombre del destino es obligatorio");
        }
        if (!TIPOS_VALIDOS.contains(tipo)) {
            throw new BusinessException("Tipo de destino invalido. Valores permitidos: " + TIPOS_VALIDOS);
        }
        if (("ROL".equals(tipo) || "USUARIO".equals(tipo))
                && (request.getValor() == null || request.getValor().isBlank())) {
            throw new BusinessException("El valor es obligatorio para destinos de tipo " + tipo);
        }
        if (destinoRepository.existsByNombreIgnoreCase(nombre)) {
            throw new BusinessException("Ya existe un destino con el nombre '" + nombre + "'");
        }

        DestinoNotificacion destino = DestinoNotificacion.builder()
                .nombre(nombre)
                .tipo(tipo)
                .valor(request.getValor())
                .descripcion(request.getDescripcion())
                .activo(request.getActivo() == null || request.getActivo())
                .fechaCreacion(LocalDateTime.now())
                .build();
        return toDto(destinoRepository.save(destino));
    }

    @Transactional
    public void eliminar(Long id) {
        DestinoNotificacion destino = destinoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Destino no encontrado: " + id));
        // Se eliminan en cascada las reglas que referencian este destino.
        reglaRepository.deleteByDestinoId(id);
        destinoRepository.delete(destino);
    }

    private DestinoDTO toDto(DestinoNotificacion d) {
        return DestinoDTO.builder()
                .id(d.getId())
                .nombre(d.getNombre())
                .tipo(d.getTipo())
                .valor(d.getValor())
                .descripcion(d.getDescripcion())
                .activo(d.isActivo())
                .fechaCreacion(d.getFechaCreacion())
                .build();
    }
}
