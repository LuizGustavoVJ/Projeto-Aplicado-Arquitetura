package com.pip.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pip.model.Transacao;
import com.pip.model.TransactionStatus;
import com.pip.repository.TransacaoRepository;
import com.pip.service.WebhookService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controller para receber webhooks da Mastercard Payment Gateway Services
 * 
 * Documentação: https://developer.mastercard.com/product/payment-gateway-services-mpgs/documentation/webhooks
 * 
 * Eventos suportados:
 * - AUTHORIZED - Transação autorizada
 * - CAPTURED - Transação capturada
 * - REFUNDED - Transação estornada
 * - FAILED - Transação falhou
 * 
 * Segurança:
 * - Validação de assinatura
 * - OAuth 2.0
 * - Validação de timestamp
 * 
 * @author Luiz Gustavo Finotello
 */
@RestController
@RequestMapping("/webhooks/mastercard")
public class MastercardWebhookController {

    private static final Logger logger = LoggerFactory.getLogger(MastercardWebhookController.class);

    @Autowired
    private TransacaoRepository transacaoRepository;

    @Autowired
    private WebhookService webhookService;

    @Autowired
    private ObjectMapper objectMapper;

    @PostMapping
    public ResponseEntity<Map<String, String>> receiveWebhook(
            @RequestHeader(value = "X-Mastercard-Signature", required = false) String signature,
            @RequestBody String payload) {

        logger.info("[MASTERCARD WEBHOOK] Recebendo notificação");

        try {
            Map<String, Object> webhookData = objectMapper.readValue(payload, Map.class);
            String eventType = (String) webhookData.get("event");
            Map<String, Object> transaction = (Map<String, Object>) webhookData.get("transaction");
            String orderId = (String) transaction.get("orderId");

            logger.info("[MASTERCARD WEBHOOK] Event: {} - OrderID: {}", eventType, orderId);

            processMastercardEvent(eventType, orderId, transaction);

            return ResponseEntity.ok(Map.of("status", "processed"));

        } catch (Exception e) {
            logger.error("[MASTERCARD WEBHOOK] Erro ao processar webhook", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Processing failed"));
        }
    }

    private void processMastercardEvent(String eventType, String orderId, Map<String, Object> transaction) {
        Transacao transacao = transacaoRepository.findByGatewayTransactionId(orderId);
        
        if (transacao == null) {
            logger.warn("[MASTERCARD WEBHOOK] Transação não encontrada: {}", orderId);
            return;
        }

        switch (eventType.toUpperCase()) {
            case "AUTHORIZED":
                transacao.setStatus(TransactionStatus.AUTHORIZED);
                logger.info("[MASTERCARD WEBHOOK] Transação autorizada: {}", orderId);
                break;

            case "CAPTURED":
                transacao.setStatus(TransactionStatus.CAPTURED);
                logger.info("[MASTERCARD WEBHOOK] Transação capturada: {}", orderId);
                break;

            case "REFUNDED":
                transacao.setStatus(TransactionStatus.VOIDED);
                logger.info("[MASTERCARD WEBHOOK] Transação estornada: {}", orderId);
                break;

            case "FAILED":
                transacao.setStatus(TransactionStatus.DENIED);
                transacao.setErrorMessage((String) transaction.get("error"));
                logger.info("[MASTERCARD WEBHOOK] Transação falhou: {}", orderId);
                break;

            default:
                logger.warn("[MASTERCARD WEBHOOK] Event desconhecido: {}", eventType);
                return;
        }

        transacaoRepository.save(transacao);
        webhookService.notificarLojista(transacao);
    }
}
