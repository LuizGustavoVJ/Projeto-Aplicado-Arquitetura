package com.pip.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuração do RabbitMQ para processamento assíncrono de webhooks
 * 
 * Define as filas, exchanges e bindings para o sistema de webhooks:
 * - webhook.queue: Fila principal para webhooks pendentes
 * - webhook.retry.queue: Fila para retry de webhooks falhados
 * - webhook.dlq: Dead Letter Queue para webhooks que falharam todas as tentativas
 * 
 * @author Luiz Gustavo Finotello
 */
@Configuration
public class RabbitMQConfig {

    // Nomes das filas
    public static final String WEBHOOK_QUEUE = "webhook.queue";
    public static final String WEBHOOK_RETRY_QUEUE = "webhook.retry.queue";
    public static final String WEBHOOK_DLQ = "webhook.dlq";
    
    // Nomes dos exchanges
    public static final String WEBHOOK_EXCHANGE = "webhook.exchange";
    public static final String WEBHOOK_RETRY_EXCHANGE = "webhook.retry.exchange";
    public static final String WEBHOOK_DLQ_EXCHANGE = "webhook.dlq.exchange";
    
    // Routing keys
    public static final String WEBHOOK_ROUTING_KEY = "webhook.send";
    public static final String WEBHOOK_RETRY_ROUTING_KEY = "webhook.retry";
    public static final String WEBHOOK_DLQ_ROUTING_KEY = "webhook.dlq";

    /**
     * Fila principal de webhooks
     */
    @Bean
    public Queue webhookQueue() {
        return QueueBuilder.durable(WEBHOOK_QUEUE)
            .withArgument("x-dead-letter-exchange", WEBHOOK_DLQ_EXCHANGE)
            .withArgument("x-dead-letter-routing-key", WEBHOOK_DLQ_ROUTING_KEY)
            .build();
    }

    /**
     * Fila de retry de webhooks
     */
    @Bean
    public Queue webhookRetryQueue() {
        return QueueBuilder.durable(WEBHOOK_RETRY_QUEUE)
            .withArgument("x-dead-letter-exchange", WEBHOOK_EXCHANGE)
            .withArgument("x-dead-letter-routing-key", WEBHOOK_ROUTING_KEY)
            .withArgument("x-message-ttl", 60000) // 1 minuto de espera antes de reprocessar
            .build();
    }

    /**
     * Dead Letter Queue para webhooks que falharam todas as tentativas
     */
    @Bean
    public Queue webhookDLQ() {
        return QueueBuilder.durable(WEBHOOK_DLQ)
            .build();
    }

    /**
     * Exchange principal
     */
    @Bean
    public DirectExchange webhookExchange() {
        return new DirectExchange(WEBHOOK_EXCHANGE);
    }

    /**
     * Exchange de retry
     */
    @Bean
    public DirectExchange webhookRetryExchange() {
        return new DirectExchange(WEBHOOK_RETRY_EXCHANGE);
    }

    /**
     * Exchange da DLQ
     */
    @Bean
    public DirectExchange webhookDLQExchange() {
        return new DirectExchange(WEBHOOK_DLQ_EXCHANGE);
    }

    /**
     * Binding da fila principal
     */
    @Bean
    public Binding webhookBinding(Queue webhookQueue, DirectExchange webhookExchange) {
        return BindingBuilder.bind(webhookQueue)
            .to(webhookExchange)
            .with(WEBHOOK_ROUTING_KEY);
    }

    /**
     * Binding da fila de retry
     */
    @Bean
    public Binding webhookRetryBinding(Queue webhookRetryQueue, DirectExchange webhookRetryExchange) {
        return BindingBuilder.bind(webhookRetryQueue)
            .to(webhookRetryExchange)
            .with(WEBHOOK_RETRY_ROUTING_KEY);
    }

    /**
     * Binding da DLQ
     */
    @Bean
    public Binding webhookDLQBinding(Queue webhookDLQ, DirectExchange webhookDLQExchange) {
        return BindingBuilder.bind(webhookDLQ)
            .to(webhookDLQExchange)
            .with(WEBHOOK_DLQ_ROUTING_KEY);
    }

    /**
     * Converter para serializar mensagens em JSON
     */
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    /**
     * RabbitTemplate configurado com converter JSON
     */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(jsonMessageConverter());
        return rabbitTemplate;
    }
}
