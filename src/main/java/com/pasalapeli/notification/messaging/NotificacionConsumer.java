package com.pasalapeli.notification.messaging;

import com.pasalapeli.notification.dto.EventoNotificacionDTO;
import com.pasalapeli.notification.service.NotificacionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Consumidor principal de eventos de negocio. Si el procesamiento falla y se
 * agotan los reintentos configurados, el mensaje se reencamina a la DLQ.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class NotificacionConsumer {

    private final NotificacionService notificacionService;

    @RabbitListener(queues = "${notificaciones.queue:pasalapeli.notificaciones.queue}")
    public void onEvento(EventoNotificacionDTO evento) {
        log.info("Evento de notificacion recibido: tipo={} usuarioId={} referencia={}",
                evento.getTipo(), evento.getUsuarioId(), evento.getCodigoTicket());
        notificacionService.procesarEvento(evento);
    }
}
