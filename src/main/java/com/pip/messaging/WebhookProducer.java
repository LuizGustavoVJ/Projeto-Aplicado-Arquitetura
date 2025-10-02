package com.pip.messaging;

import com.pip.config.RabbitMQConfig;
import com.pip.dto.WebhookMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Producer para enviar webhooks para a fila RabbitMQ
 * 
 * @author Luiz Gustavo Finotello
 */
@Component
public class WebhookProducer {

    private static final Logger logger = LoggerFactory.getLogger(WebhookProducer.class);

    @Autowired
    private RabbitTemplate rabbitTemplate;

    /**
     * Envia webhook para a fila principal
     * 
     * @param webhookEventId ID do evento de webhook
     */
    public void sendWebhook(UUID webhookEventId) {
        sendWebhook(webhookEventId, 1);
    }

    /**
     * Envia webhook para a fila principal com número de tentativa
     * 
     * @param webhookEventId ID do evento de webhook
     * @param attemptNumber Número da tentativa
     */
    public void sendWebhook(UUID webhookEventId, int attemptNumber) {
        try {
            WebhookMessage message = new WebhookMessage(webhookEventId, attemptNumber);
            
            rabbitTemplate.convertAndSend(
                RabbitMQConfig.WEBHOOK_EXCHANGE,
                RabbitMQConfig.WEBHOOK_ROUTING_KEY,
                message
            );
            
            logger.info("Webhook enviado para fila: {} (tentativa {})", webhookEventId, attemptNumber);
            
        } catch (Exception e) {
            logger.error("Erro ao enviar webhook para fila: {}", e.getMessage(), e);
            throw new RuntimeException("Falha ao enviar webhook para fila", e);
        }
    }

    /**
     * Envia webhook para a fila de retry
     * 
     * @param webhookEventId ID do evento de webhook
     * @param attemptNumber Número da tentativa
     */
    public void sendWebhookToRetry(UUID webhookEventId, int attemptNumber) {
        try {
            WebhookMessage message = new WebhookMessage(webhookEventId, attemptNumber);
            
            rabbitTemplate.convertAndSend(
                RabbitMQConfig.WEBHOOK_RETRY_EXCHANGE,
                RabbitMQConfig.WEBHOOK_RETRY_ROUTING_KEY,
                message
            );
            
            logger.info("Webhook enviado para fila de retry: {} (tentativa {})", webhookEventId, attemptNumber);
            
        } catch (Exception e) {
            logger.error("Erro ao enviar webhook para fila de retry: {}", e.getMessage(), e);
        }
    }
}
