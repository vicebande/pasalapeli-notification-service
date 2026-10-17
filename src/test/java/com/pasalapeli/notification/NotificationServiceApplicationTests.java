package com.pasalapeli.notification;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Prueba de integracion base. Se desactiva el listener de SQS para que
 * el contexto levante sin requerir AWS disponible en el entorno de tests.
 */
@SpringBootTest(properties = {
        "spring.cloud.aws.sqs.listener.auto-startup=false"
})
class NotificationServiceApplicationTests {

    @Test
    void contextLoads() {
    }
}
