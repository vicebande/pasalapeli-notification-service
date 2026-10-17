package com.pasalapeli.notification.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.support.converter.DefaultJackson2JavaTypeMapper;
import org.springframework.amqp.support.converter.Jackson2JavaTypeMapper;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.amqp.support.converter.SimpleMessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuracion centralizada de la topologia de mensajeria.
 *
 *   Exchange (topic)  : pasalapeli.events
 *   Cola principal    : pasalapeli.notificaciones.queue
 *   Dead-Letter Exch. : pasalapeli.dlx
 *   Dead-Letter Queue : pasalapeli.notificaciones.dlq
 *
 * La cola principal declara los argumentos x-dead-letter-* para que RabbitMQ
 * derive automaticamente los mensajes que no pueden procesarse (nack/retry
 * agotado) hacia la DLQ.
 */
@Configuration
public class RabbitConfig {

    @Value("${notificaciones.exchange:pasalapeli.events}")
    private String exchangeName;

    @Value("${notificaciones.queue:pasalapeli.notificaciones.queue}")
    private String queueName;

    @Value("${notificaciones.dlx:pasalapeli.dlx}")
    private String dlxName;

    @Value("${notificaciones.dlq:pasalapeli.notificaciones.dlq}")
    private String dlqName;

    @Value("${notificaciones.dlq-routing-key:pasalapeli.notificaciones.dlq}")
    private String dlqRoutingKey;

    @Bean
    public TopicExchange pasalapeliEventsExchange() {
        return ExchangeBuilder.topicExchange(exchangeName).durable(true).build();
    }

    @Bean
    public DirectExchange pasalapeliDlx() {
        return ExchangeBuilder.directExchange(dlxName).durable(true).build();
    }

    @Bean
    public Queue notificacionesQueue() {
        return QueueBuilder.durable(queueName)
                .withArgument("x-dead-letter-exchange", dlxName)
                .withArgument("x-dead-letter-routing-key", dlqRoutingKey)
                .build();
    }

    @Bean
    public Queue notificacionesDlq() {
        return QueueBuilder.durable(dlqName).build();
    }

    @Bean
    public Binding peliculaBinding(Queue notificacionesQueue, TopicExchange pasalapeliEventsExchange) {
        return BindingBuilder.bind(notificacionesQueue).to(pasalapeliEventsExchange).with("pelicula.*");
    }

    @Bean
    public Binding ticketBinding(Queue notificacionesQueue, TopicExchange pasalapeliEventsExchange) {
        return BindingBuilder.bind(notificacionesQueue).to(pasalapeliEventsExchange).with("ticket.*");
    }

    @Bean
    public Binding dlqBinding(Queue notificacionesDlq, DirectExchange pasalapeliDlx) {
        return BindingBuilder.bind(notificacionesDlq).to(pasalapeliDlx).with(dlqRoutingKey);
    }

    /**
     * Factory dedicado para el listener de la DLQ: NO aplica el conversor JSON.
     * El consumidor de fallos recibe el mensaje crudo (byte[]) para poder
     * registrar el payload que fallo tal cual (aunque no sea JSON valido),
     * que es exactamente el caso que llega a una Dead-Letter Queue.
     */
    @Bean
    public SimpleRabbitListenerContainerFactory dlqListenerFactory(ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(new SimpleMessageConverter());
        factory.setAcknowledgeMode(AcknowledgeMode.AUTO);
        factory.setDefaultRequeueRejected(false);
        return factory;
    }

    /**
     * Serializa/deserializa los eventos como JSON. Se fuerza TypePrecedence.INFERRED
     * para que el consumidor deserialice segun el tipo del parametro del listener
     * (EventoNotificacionDTO) e ignore el header __TypeId__ que envia el productor.
     * Asi los servicios quedan desacoplados (no comparten clases de evento).
     */
    @Bean
    public MessageConverter jsonMessageConverter() {
        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter(new ObjectMapper());
        DefaultJackson2JavaTypeMapper typeMapper = new DefaultJackson2JavaTypeMapper();
        typeMapper.setTypePrecedence(Jackson2JavaTypeMapper.TypePrecedence.INFERRED);
        converter.setJavaTypeMapper(typeMapper);
        return converter;
    }
}
