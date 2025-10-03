package com.pip.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pip.messaging.WebhookProducer;
import com.pip.model.Lojista;
import com.pip.model.Transacao;
import com.pip.model.WebhookEvent;
import com.pip.repository.WebhookEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * Serviço de gerenciamento e envio de webhooks
 * 
 * Implementa:
 * - Criação de webhooks para eventos de transação
 * - Envio com assinatura HMAC-SHA256
 * - Retry com backoff exponencial
 * - Registro de tentativas e respostas
 * 
 * @author Luiz Gustavo Finotello
 */
@Service
public class WebhookService {

    private static final Logger logger = LoggerFactory.getLogger(WebhookService.class);
    private static final String HMAC_ALGORITHM = "HmacSHA256";

    @Autowired
    private WebhookEventRepository webhookEventRepository;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private WebhookProducer webhookProducer;

    /**
     * Cria webhook para evento de transação
     * 
     * @param lojista Lojista destinatário
     * @param transacao Transação que gerou o evento
     * @param evento Tipo do evento
     */
    public WebhookEvent criarWebhook(Lojista lojista, Transacao transacao, String evento) {
        logger.info("Criando webhook para evento {} da transação {}", evento, transacao.getTransactionId());

        // Verificar se lojista tem webhook configurado
        if (lojista.getWebhookUrl() == null || lojista.getWebhookUrl().trim().isEmpty()) {
            logger.warn("Lojista {} não tem webhook configurado", lojista.getId());
            return null;
        }

        // Criar payload do webhook
        Map<String, Object> payload = criarPayload(transacao, evento);

        // Converter payload para JSON
        String payloadJson;
        try {
            payloadJson = objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            logger.error("Erro ao serializar payload do webhook: {}", e.getMessage());
            return null;
        }

        // Gerar assinatura HMAC
        String signature = null;
        if (lojista.getWebhookSecret() != null && !lojista.getWebhookSecret().trim().isEmpty()) {
            signature = gerarAssinatura(payloadJson, lojista.getWebhookSecret());
        }

        // Criar entidade WebhookEvent
        WebhookEvent webhookEvent = new WebhookEvent();
        webhookEvent.setLojista(lojista);
        webhookEvent.setTransacao(transacao);
        webhookEvent.setEvento(evento);
        webhookEvent.setUrl(lojista.getWebhookUrl());
        webhookEvent.setPayload(payloadJson);
        webhookEvent.setSignature(signature);
        webhookEvent.setStatus("PENDING");
        webhookEvent.setTentativas(0);
        webhookEvent.setMaxTentativas(5);
        webhookEvent.setCreatedAt(ZonedDateTime.now());

        // Salvar webhook event
        webhookEvent = webhookEventRepository.save(webhookEvent);

        logger.info("Webhook event criado com ID: {}", webhookEvent.getId());

        // Enviar para fila RabbitMQ para processamento assíncrono
        try {
            webhookProducer.sendWebhook(webhookEvent.getId());
            logger.info("Webhook {} enviado para fila RabbitMQ", webhookEvent.getId());
        } catch (Exception e) {
            logger.error("Erro ao enviar webhook para fila: {}", e.getMessage(), e);
        }

        return webhookEvent;
    }

    /**
     * Envia webhook para o lojista
     * 
     * @param webhook Webhook a ser enviado
     * @return true se enviado com sucesso, false caso contrário
     */
    public boolean enviarWebhook(WebhookEvent webhook) {
        logger.info("Enviando webhook {} para URL {}", webhook.getId(), webhook.getUrl());

        // Verificar se já atingiu número máximo de tentativas
        if (webhook.getTentativas() >= webhook.getMaxTentativas()) {
            logger.warn("Webhook {} atingiu número máximo de tentativas", webhook.getId());
            webhook.setStatus("FAILED");
            webhook.setErrorMessage("Número máximo de tentativas excedido");
            webhook.setUpdatedAt(ZonedDateTime.now());
            webhookEventRepository.save(webhook);
            return false;
        }

        // Atualizar status para SENDING
        webhook.setStatus("SENDING");
        webhook.setTentativas(webhook.getTentativas() + 1);
        webhook.setEnviadoAt(ZonedDateTime.now());
        webhook.setUpdatedAt(ZonedDateTime.now());
        webhookEventRepository.save(webhook);

        try {
            // Preparar requisição
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            // Adicionar assinatura se disponível
            if (webhook.getSignature() != null) {
                headers.set("X-Webhook-Signature", webhook.getSignature());
            }
            
            // Adicionar headers adicionais
            headers.set("X-Webhook-Event", webhook.getEvento());
            headers.set("X-Webhook-Id", webhook.getId().toString());
            headers.set("X-Webhook-Attempt", String.valueOf(webhook.getTentativas()));

            HttpEntity<String> entity = new HttpEntity<>(webhook.getPayload(), headers);

            // Enviar webhook
            ResponseEntity<String> response = restTemplate.exchange(
                webhook.getUrl(),
                HttpMethod.POST,
                entity,
                String.class
            );

            // Processar resposta
            webhook.setHttpStatus(response.getStatusCode().value());
            webhook.setResponseBody(response.getBody());

            if (response.getStatusCode().is2xxSuccessful()) {
                webhook.setStatus("SUCCESS");
                webhook.setSucessoAt(ZonedDateTime.now());
                webhook.setUpdatedAt(ZonedDateTime.now());
                webhookEventRepository.save(webhook);

                logger.info("Webhook {} enviado com sucesso. Status: {}", 
                    webhook.getId(), response.getStatusCode());

                return true;
            } else {
                // Status não é 2xx - considerar falha
                webhook.setStatus("FAILED");
                webhook.setErrorMessage("HTTP Status: " + response.getStatusCode());
                webhook.setUpdatedAt(ZonedDateTime.now());
                
                // Agendar próxima tentativa
                agendarProximaTentativa(webhook);
                
                webhookEventRepository.save(webhook);

                logger.warn("Webhook {} falhou com status {}. Tentativa {}/{}",
                    webhook.getId(), response.getStatusCode(), 
                    webhook.getTentativas(), webhook.getMaxTentativas());

                return false;
            }

        } catch (Exception e) {
            logger.error("Erro ao enviar webhook {}: {}", webhook.getId(), e.getMessage());

            webhook.setStatus("FAILED");
            webhook.setErrorMessage(e.getMessage());
            webhook.setUpdatedAt(ZonedDateTime.now());
            
            // Agendar próxima tentativa
            agendarProximaTentativa(webhook);
            
            webhookEventRepository.save(webhook);

            return false;
        }
    }

    /**
     * Agenda próxima tentativa de envio com backoff exponencial
     * 
     * @param webhook Webhook que falhou
     */
    private void agendarProximaTentativa(WebhookEvent webhook) {
        if (webhook.getTentativas() < webhook.getMaxTentativas()) {
            // Backoff exponencial: 1min, 2min, 4min, 8min, 16min
            long minutosEspera = (long) Math.pow(2, webhook.getTentativas() - 1);
            ZonedDateTime proximaTentativa = ZonedDateTime.now().plusMinutes(minutosEspera);
            
            webhook.setProximaTentativa(proximaTentativa);
            webhook.setStatus("PENDING");

            logger.info("Próxima tentativa do webhook {} agendada para {}", 
                webhook.getId(), proximaTentativa);
        } else {
            webhook.setStatus("FAILED");
            webhook.setProximaTentativa(null);
            
            logger.warn("Webhook {} falhou definitivamente após {} tentativas", 
                webhook.getId(), webhook.getTentativas());
        }
    }

    /**
     * Cria payload do webhook
     */
    private Map<String, Object> criarPayload(Transacao transacao, String evento) {
        Map<String, Object> payload = new HashMap<>();
        
        payload.put("event", evento);
        payload.put("timestamp", ZonedDateTime.now().toString());
        
        // Dados da transação
        Map<String, Object> transactionData = new HashMap<>();
        transactionData.put("transaction_id", transacao.getTransactionId());
        transactionData.put("gateway_transaction_id", transacao.getGatewayTransactionId());
        transactionData.put("status", transacao.getStatus());
        transactionData.put("amount", transacao.getValor());
        transactionData.put("currency", transacao.getMoeda());
        transactionData.put("installments", transacao.getParcelas());
        transactionData.put("authorization_code", transacao.getAuthorizationCode());
        transactionData.put("nsu", transacao.getNsu());
        transactionData.put("tid", transacao.getTid());
        transactionData.put("created_at", transacao.getCreatedAt().toString());
        
        if (transacao.getAuthorizedAt() != null) {
            transactionData.put("authorized_at", transacao.getAuthorizedAt().toString());
        }
        if (transacao.getCapturedAt() != null) {
            transactionData.put("captured_at", transacao.getCapturedAt().toString());
        }
        if (transacao.getVoidedAt() != null) {
            transactionData.put("voided_at", transacao.getVoidedAt().toString());
        }
        
        // Dados do cartão (apenas últimos 4 dígitos)
        if (transacao.getCardLastDigits() != null) {
            Map<String, Object> cardData = new HashMap<>();
            cardData.put("brand", transacao.getCardBrand());
            cardData.put("last_digits", transacao.getCardLastDigits());
            transactionData.put("card", cardData);
        }
        
        // Dados do cliente
        if (transacao.getCustomerName() != null) {
            Map<String, Object> customerData = new HashMap<>();
            customerData.put("name", transacao.getCustomerName());
            customerData.put("email", transacao.getCustomerEmail());
            customerData.put("document", transacao.getCustomerDocument());
            transactionData.put("customer", customerData);
        }
        
        payload.put("transaction", transactionData);
        
        return payload;
    }

    /**
     * Gera assinatura HMAC-SHA256 do payload
     */
    private String gerarAssinatura(String payload, String secret) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            SecretKeySpec secretKeySpec = new SecretKeySpec(
                secret.getBytes(StandardCharsets.UTF_8), 
                HMAC_ALGORITHM
            );
            mac.init(secretKeySpec);
            
            byte[] hmacBytes = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hmacBytes);
            
        } catch (Exception e) {
            logger.error("Erro ao gerar assinatura HMAC: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Verifica assinatura do webhook
     * 
     * @param payload Payload do webhook
     * @param signature Assinatura recebida
     * @param secret Secret do lojista
     * @return true se assinatura é válida
     */
    public boolean verificarAssinatura(String payload, String signature, String secret) {
        String expectedSignature = gerarAssinatura(payload, secret);
        return expectedSignature != null && expectedSignature.equals(signature);
    }

    /**
     * Cancela um webhook pendente ou falhado
     * 
     * @param webhookId ID do webhook
     */
    public void cancelarWebhook(String webhookEventId) {
        webhookEventRepository.findById(java.util.UUID.fromString(webhookEventId))
            .ifPresent(webhookEvent -> {
                if ("PENDING".equals(webhookEvent.getStatus()) || "FAILED".equals(webhookEvent.getStatus())) {
                    webhookEvent.setStatus("CANCELLED");
                    webhookEvent.setUpdatedAt(ZonedDateTime.now());
                    webhookEventRepository.save(webhookEvent);
                    
                    logger.info("Webhook event {} cancelado", webhookEventId);
                }
            });
    }

    /**
     * Notifica o lojista sobre uma transação via webhook
     * 
     * @param transacao Transação a ser notificada
     */
    public void notificarLojista(Transacao transacao) {
        try {
            logger.info("Notificando lojista sobre transação {}", transacao.getId());
            
            // Criar evento de webhook
            criarWebhookParaTransacao(transacao, "transaction.updated");
            
        } catch (Exception e) {
            logger.error("Erro ao notificar lojista sobre transação {}: {}", 
                transacao.getId(), e.getMessage(), e);
        }
    }
}
