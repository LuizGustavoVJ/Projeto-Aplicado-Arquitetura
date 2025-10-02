package com.pip.messaging;

import com.pip.config.RabbitMQConfig;
import com.pip.dto.WebhookMessage;
import com.pip.model.WebhookEvent;
import com.pip.repository.WebhookEventRepository;
import com.pip.service.WebhookService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Consumer para processar webhooks da fila RabbitMQ
 * 
 * @author Luiz Gustavo Finotello
 */
@Component
public class WebhookConsumer {

    private static final Logger logger = LoggerFactory.getLogger(WebhookConsumer.class);
    private static final int MAX_ATTEMPTS = 5;

    @Autowired
    private WebhookService webhookService;

    @Autowired
    private WebhookEventRepository webhookEventRepository;

    @Autowired
    private WebhookProducer webhookProducer;

    /**
     * Processa webhooks da fila principal
     * 
     * @param message Mensagem do webhook
     */
    @RabbitListener(queues = RabbitMQConfig.WEBHOOK_QUEUE)
    public void processWebhook(WebhookMessage message) {
        logger.info("Processando webhook da fila: {} (tentativa {})", 
            message.getWebhookEventId(), message.getAttemptNumber());

        try {
            // Buscar evento de webhook
            Optional<WebhookEvent> eventOpt = webhookEventRepository.findById(message.getWebhookEventId());
            
            if (eventOpt.isEmpty()) {
                logger.warn("Webhook event não encontrado: {}", message.getWebhookEventId());
                return;
            }

            WebhookEvent event = eventOpt.get();

            // Verificar se já foi enviado com sucesso
            if ("SUCCESS".equals(event.getStatus())) {
                logger.info("Webhook já foi enviado com sucesso: {}", message.getWebhookEventId());
                return;
            }

            // Tentar enviar webhook
            boolean success = webhookService.enviarWebhook(event);

            if (success) {
                logger.info("Webhook enviado com sucesso: {}", message.getWebhookEventId());
            } else {
                // Falhou - verificar se deve retentar
                handleWebhookFailure(message, event);
            }

        } catch (Exception e) {
            logger.error("Erro ao processar webhook {}: {}", message.getWebhookEventId(), e.getMessage(), e);
            
            // Tentar buscar o evento para retry
            webhookEventRepository.findById(message.getWebhookEventId())
                .ifPresent(event -> handleWebhookFailure(message, event));
        }
    }

    /**
     * Processa webhooks da Dead Letter Queue
     * 
     * @param message Mensagem do webhook
     */
    @RabbitListener(queues = RabbitMQConfig.WEBHOOK_DLQ)
    public void processWebhookDLQ(WebhookMessage message) {
        logger.error("Webhook movido para DLQ após {} tentativas: {}", 
            message.getAttemptNumber(), message.getWebhookEventId());

        try {
            // Marcar webhook como falhado permanentemente
            webhookEventRepository.findById(message.getWebhookEventId())
                .ifPresent(event -> {
                    event.setStatus("FAILED");
                    event.setErrorMessage("Falhou após " + message.getAttemptNumber() + " tentativas");
                    webhookEventRepository.save(event);
                    
                    logger.info("Webhook marcado como FAILED: {}", message.getWebhookEventId());
                });

        } catch (Exception e) {
            logger.error("Erro ao processar webhook da DLQ: {}", e.getMessage(), e);
        }
    }

    /**
     * Trata falha no envio de webhook
     * 
     * @param message Mensagem do webhook
     * @param event Evento do webhook
     */
    private void handleWebhookFailure(WebhookMessage message, WebhookEvent event) {
        int nextAttempt = message.getAttemptNumber() + 1;

        if (nextAttempt <= MAX_ATTEMPTS) {
            logger.info("Agendando retry {} para webhook: {}", nextAttempt, message.getWebhookEventId());
            
            // Enviar para fila de retry
            webhookProducer.sendWebhookToRetry(message.getWebhookEventId(), nextAttempt);
            
        } else {
            logger.warn("Webhook atingiu número máximo de tentativas: {}", message.getWebhookEventId());
            
            // Marcar como falhado
            event.setStatus("FAILED");
            event.setErrorMessage("Falhou após " + MAX_ATTEMPTS + " tentativas");
            webhookEventRepository.save(event);
        }
    }
}
