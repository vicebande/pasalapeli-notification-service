package com.pasalapeli.notification;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Prueba de integracion base. Se desactivan los listeners de RabbitMQ para que
 * el contexto levante sin requerir un broker disponible en el entorno de tests.
 */
@SpringBootTest(properties = {
        "spring.rabbitmq.listener.simple.auto-startup=false"
})
class NotificationServiceApplicationTests {

    @Test
    void contextLoads() {
    }
}
