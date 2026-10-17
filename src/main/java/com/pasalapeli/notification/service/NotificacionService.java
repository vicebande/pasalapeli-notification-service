package com.pasalapeli.notification.service;

import com.pasalapeli.notification.dto.EventoNotificacionDTO;
import com.pasalapeli.notification.dto.NotificacionDTO;
import com.pasalapeli.notification.entity.DestinoNotificacion;
import com.pasalapeli.notification.entity.Notificacion;
import com.pasalapeli.notification.entity.ReglaDistribucion;
import com.pasalapeli.notification.entity.Usuario;
import com.pasalapeli.notification.exception.BusinessException;
import com.pasalapeli.notification.exception.ResourceNotFoundException;
import com.pasalapeli.notification.repository.DestinoNotificacionRepository;
import com.pasalapeli.notification.repository.NotificacionRepository;
import com.pasalapeli.notification.repository.ReglaDistribucionRepository;
import com.pasalapeli.notification.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificacionService {

    public static final String TIPO_PELICULA_CREADA = "PELICULA_CREADA";
    public static final String TIPO_TICKET_PAGADO = "TICKET_PAGADO";
    public static final String TIPO_TICKET_DEVUELTO = "TICKET_DEVUELTO";

    private final NotificacionRepository notificacionRepository;
    private final ReglaDistribucionRepository reglaRepository;
    private final DestinoNotificacionRepository destinoRepository;
    private final UsuarioRepository usuarioRepository;

    // ============================ Consulta (usuario) ============================

    @Transactional(readOnly = true)
    public List<NotificacionDTO> listarPorUsuario(Long usuarioId) {
        return notificacionRepository.findByUsuarioIdOrderByFechaCreacionDesc(usuarioId)
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public long contarNoLeidas(Long usuarioId) {
        return notificacionRepository.countByUsuarioIdAndLeidaFalse(usuarioId);
    }

    @Transactional
    public NotificacionDTO marcarLeida(Long id, Long usuarioId) {
        Notificacion n = notificacionRepository.findByIdAndUsuarioId(id, usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Notificacion no encontrada: " + id));
        n.setLeida(true);
        return toDto(notificacionRepository.save(n));
    }

    @Transactional
    public int marcarTodasLeidas(Long usuarioId) {
        List<Notificacion> pendientes = notificacionRepository
                .findByUsuarioIdOrderByFechaCreacionDesc(usuarioId)
                .stream().filter(n -> !n.isLeida()).collect(Collectors.toList());
        pendientes.forEach(n -> n.setLeida(true));
        notificacionRepository.saveAll(pendientes);
        return pendientes.size();
    }

    // ======================== Procesamiento asincrono ==========================

    /**
     * Procesa un evento proveniente de RabbitMQ: resuelve los destinatarios
     * (reglas de distribucion) y persiste una notificacion INDEPENDIENTE por
     * usuario. Lanza excepcion si el evento es invalido para derivarlo a la DLQ.
     */
    @Transactional
    public void procesarEvento(EventoNotificacionDTO evento) {
        if (evento == null || evento.getTipo() == null || evento.getTipo().isBlank()) {
            throw new BusinessException("Evento de notificacion invalido: tipo ausente");
        }

        Set<Long> destinatarios = resolverDestinatarios(evento);
        if (destinatarios.isEmpty()) {
            log.warn("Evento {} sin destinatarios resueltos; no se generan notificaciones", evento.getTipo());
            return;
        }

        ContenidoNotificacion contenido = construirContenido(evento);
        List<Notificacion> nuevas = destinatarios.stream()
                .map(usuarioId -> Notificacion.builder()
                        .usuarioId(usuarioId)
                        .tipo(evento.getTipo())
                        .titulo(contenido.titulo())
                        .mensaje(contenido.mensaje())
                        .referencia(contenido.referencia())
                        .leida(false)
                        .fechaCreacion(LocalDateTime.now())
                        .build())
                .collect(Collectors.toList());

        notificacionRepository.saveAll(nuevas);
        log.info("Evento {} procesado: {} notificacion(es) generada(s)", evento.getTipo(), nuevas.size());
    }

    private Set<Long> resolverDestinatarios(EventoNotificacionDTO evento) {
        Set<Long> ids = new LinkedHashSet<>();
        List<ReglaDistribucion> reglas = reglaRepository.findByEventoAndActivoTrue(evento.getTipo());

        if (!reglas.isEmpty()) {
            for (ReglaDistribucion regla : reglas) {
                destinoRepository.findById(regla.getDestinoId())
                        .filter(DestinoNotificacion::isActivo)
                        .ifPresent(destino -> ids.addAll(usuariosDe(destino, evento)));
            }
            return ids;
        }

        // Distribucion por defecto cuando no hay reglas configuradas.
        if (evento.getUsuarioId() != null) {
            ids.add(evento.getUsuarioId());
        } else {
            ids.addAll(todosLosUsuarios());
        }
        return ids;
    }

    private Set<Long> usuariosDe(DestinoNotificacion destino, EventoNotificacionDTO evento) {
        Set<Long> ids = new LinkedHashSet<>();
        String tipo = destino.getTipo() == null ? "" : destino.getTipo().toUpperCase();
        switch (tipo) {
            case "TODOS" -> ids.addAll(todosLosUsuarios());
            case "ROL" -> usuarioRepository.findByRol(destino.getValor())
                    .forEach(u -> ids.add(u.getId()));
            case "USUARIO" -> {
                if (destino.getValor() != null) {
                    try {
                        ids.add(Long.parseLong(destino.getValor().trim()));
                    } catch (NumberFormatException ex) {
                        log.warn("Destino {} con valor de usuario invalido: {}", destino.getId(), destino.getValor());
                    }
                }
            }
            case "USUARIO_EVENTO" -> {
                if (evento.getUsuarioId() != null) {
                    ids.add(evento.getUsuarioId());
                }
            }
            default -> log.warn("Tipo de destino desconocido: {}", destino.getTipo());
        }
        return ids;
    }

    private List<Long> todosLosUsuarios() {
        return usuarioRepository.findAll().stream().map(Usuario::getId).collect(Collectors.toList());
    }

    private ContenidoNotificacion construirContenido(EventoNotificacionDTO evento) {
        String pelicula = evento.getTituloPelicula() != null ? evento.getTituloPelicula() : "la pelicula";
        String cantidad = evento.getCantidad() != null ? String.valueOf(evento.getCantidad()) : "?";
        String codigo = evento.getCodigoTicket() != null ? evento.getCodigoTicket() : "S/N";

        return switch (evento.getTipo()) {
            case TIPO_PELICULA_CREADA -> new ContenidoNotificacion(
                    "Nueva pelicula en cartelera",
                    "Se agrego '" + pelicula + "' a la cartelera. Ya puedes comprar tus entradas.",
                    evento.getPeliculaId() != null ? String.valueOf(evento.getPeliculaId()) : pelicula);
            case TIPO_TICKET_PAGADO -> new ContenidoNotificacion(
                    "Compra confirmada",
                    "Tu ticket " + codigo + " para '" + pelicula + "' (" + cantidad + " entrada/s) fue pagado correctamente.",
                    codigo);
            case TIPO_TICKET_DEVUELTO -> new ContenidoNotificacion(
                    "Devolucion procesada",
                    "El ticket " + codigo + " para '" + pelicula + "' (" + cantidad + " entrada/s) fue devuelto y los cupos liberados.",
                    codigo);
            default -> new ContenidoNotificacion(
                    "Notificacion",
                    "Nuevo evento: " + evento.getTipo(),
                    evento.getCodigoTicket());
        };
    }

    private NotificacionDTO toDto(Notificacion n) {
        return NotificacionDTO.builder()
                .id(n.getId())
                .tipo(n.getTipo())
                .titulo(n.getTitulo())
                .mensaje(n.getMensaje())
                .referencia(n.getReferencia())
                .leida(n.isLeida())
                .fechaCreacion(n.getFechaCreacion())
                .build();
    }

    private record ContenidoNotificacion(String titulo, String mensaje, String referencia) {
    }
}
