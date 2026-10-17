package com.pasalapeli.notification.messaging;

import com.pasalapeli.notification.service.MensajeFallidoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * Consumidor de la Dead-Letter Queue. Registra el mensaje fallido y su causa
 * para diagnostico de mensajes fallidos y confirma (ack)
 * el mensaje para que no vuelva a reprocesarse indefinidamente.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DeadLetterConsumer {

    private final MensajeFallidoService mensajeFallidoService;

    // containerFactory dlqListenerFactory: sin conversor JSON, para recibir el
    // payload crudo (aunque sea invalido) y registrarlo tal cual para diagnostico.
    @RabbitListener(queues = "${notificaciones.dlq:pasalapeli.notificaciones.dlq}",
            containerFactory = "dlqListenerFactory")
    public void onDeadLetter(Message message) {
        String payload = new String(message.getBody(), StandardCharsets.UTF_8);
        Map<String, Object> headers = message.getMessageProperties().getHeaders();

        String error = extraerError(headers);
        int intentos = extraerIntentos(headers);
        String cola = extraerColaOrigen(headers, message);

        log.error("Mensaje derivado a DLQ [cola={}] tras {} intento(s). Causa: {}", cola, intentos, error);
        mensajeFallidoService.registrar(cola, payload, error, intentos);
    }

    private String extraerError(Map<String, Object> headers) {
        if (headers.get("x-exception-message") != null) {
            return String.valueOf(headers.get("x-exception-message"));
        }
        if (headers.get("x-first-death-reason") != null) {
            return String.valueOf(headers.get("x-first-death-reason"));
        }
        return "Causa desconocida";
    }

    @SuppressWarnings("unchecked")
    private int extraerIntentos(Map<String, Object> headers) {
        Object xDeath = headers.get("x-death");
        if (xDeath instanceof List<?> lista && !lista.isEmpty() && lista.get(0) instanceof Map<?, ?> first) {
            Object count = ((Map<String, Object>) first).get("count");
            if (count != null) {
                try {
                    return Integer.parseInt(String.valueOf(count));
                } catch (NumberFormatException ignored) {
                    // se ignora y se usa el valor por defecto
                }
            }
        }
        return 1;
    }

    @SuppressWarnings("unchecked")
    private String extraerColaOrigen(Map<String, Object> headers, Message message) {
        Object xDeath = headers.get("x-death");
        if (xDeath instanceof List<?> lista && !lista.isEmpty() && lista.get(0) instanceof Map<?, ?> first) {
            Object queue = ((Map<String, Object>) first).get("queue");
            if (queue != null) {
                return String.valueOf(queue);
            }
        }
        return message.getMessageProperties().getConsumerQueue();
    }
}
