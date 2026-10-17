package com.pasalapeli.notification.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pasalapeli.notification.dto.EventoNotificacionDTO;
import com.pasalapeli.notification.service.NotificacionService;
import io.awspring.cloud.sqs.annotation.SqsListener;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Consumidor principal de eventos de negocio desde SQS (raw_message_delivery:
 * el body es el JSON del evento, con "tipo" como atributo SQS copiado desde SNS).
 * Si el procesamiento falla, el mensaje vuelve a la cola por visibilidad y,
 * tras maxReceiveCount=3, SQS lo deriva automaticamente a la DLQ (redrive).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class NotificacionConsumer {

    private final NotificacionService notificacionService;
    private final ObjectMapper objectMapper;

    @SqsListener("${app.sqs.queue-url:pasalapeli-notificaciones}")
    public void onEvento(String payload) {
        EventoNotificacionDTO evento;
        try {
            evento = objectMapper.readValue(payload, EventoNotificacionDTO.class);
        } catch (Exception e) {
            // Payload ilegible: lanzar hace que SQS reintente y luego derive a DLQ
            throw new IllegalArgumentException("Payload ilegible en la cola de notificaciones", e);
        }
        log.info("Evento de notificacion recibido: tipo={} usuarioId={} referencia={}",
                evento.getTipo(), evento.getUsuarioId(), evento.getCodigoTicket());
        notificacionService.procesarEvento(evento);
    }
}
