package com.pip.dto;

import java.io.Serializable;
import java.util.UUID;

/**
 * DTO para mensagens de webhook na fila RabbitMQ
 * 
 * @author Luiz Gustavo Finotello
 */
public class WebhookMessage implements Serializable {

    private UUID webhookEventId;
    private int attemptNumber;

    public WebhookMessage() {
    }

    public WebhookMessage(UUID webhookEventId, int attemptNumber) {
        this.webhookEventId = webhookEventId;
        this.attemptNumber = attemptNumber;
    }

    public UUID getWebhookEventId() {
        return webhookEventId;
    }

    public void setWebhookEventId(UUID webhookEventId) {
        this.webhookEventId = webhookEventId;
    }

    public int getAttemptNumber() {
        return attemptNumber;
    }

    public void setAttemptNumber(int attemptNumber) {
        this.attemptNumber = attemptNumber;
    }

    @Override
    public String toString() {
        return "WebhookMessage{" +
                "webhookEventId=" + webhookEventId +
                ", attemptNumber=" + attemptNumber +
                '}';
    }
}
