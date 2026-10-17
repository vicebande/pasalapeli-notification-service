package com.pasalapeli.notification.messaging;

import com.pasalapeli.notification.service.MensajeFallidoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;

/**
 * Poller de la Dead-Letter Queue (cada 60 s). SQS no ofrece un listener
 * push para DLQs sin subscribir, asi que se consulta la DLQ, se persiste
 * cada mensaje como MensajeFallido (misma API admin de diagnostico) y se
 * elimina de la cola para no reprocesarlo indefinidamente.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DeadLetterConsumer {

    /** maxReceiveCount configurado en la cola principal (setup-messaging.sh). */
    private static final int MAX_RECEIVE_COUNT_PRINCIPAL = 3;

    private final MensajeFallidoService mensajeFallidoService;
    private final SqsAsyncClient sqsClient;

    @Value("${app.sqs.dlq-url:}")
    private String dlqUrl;

    @Value("${app.sqs.dlq-name:pasalapeli-notificaciones-dlq}")
    private String dlqName;

    @Scheduled(fixedDelay = 60_000, initialDelay = 30_000)
    public void vaciarDlq() {
        if (dlqUrl == null || dlqUrl.isBlank()) {
            return;
        }
        try {
            ReceiveMessageRequest request = ReceiveMessageRequest.builder()
                    .queueUrl(dlqUrl)
                    .maxNumberOfMessages(10)
                    .waitTimeSeconds(10)
                    .build();
            for (Message mensaje : sqsClient.receiveMessage(request).get().messages()) {
                String payload = mensaje.body();
                String error = "Derivado a DLQ tras " + MAX_RECEIVE_COUNT_PRINCIPAL
                        + " intentos fallidos en la cola principal (redrive SQS)";
                log.error("Mensaje en DLQ [{}]: {} byte(s)", dlqName, payload == null ? 0 : payload.length());
                mensajeFallidoService.registrar(dlqName, payload, error, MAX_RECEIVE_COUNT_PRINCIPAL);
                sqsClient.deleteMessage(d -> d.queueUrl(dlqUrl).receiptHandle(mensaje.receiptHandle())).get();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Ciclo de DLQ interrumpido: {}", e.getMessage());
        } catch (Exception e) {
            // No tumbar el ciclo: se reintenta en el proximo scheduled
            log.warn("No se pudo consultar la DLQ en este ciclo: {}", e.getMessage());
        }
    }
}
