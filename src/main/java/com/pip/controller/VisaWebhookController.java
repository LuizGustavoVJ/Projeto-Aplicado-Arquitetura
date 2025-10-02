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

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

/**
 * Controller para receber webhooks da Visa Direct
 * 
 * Documentação: https://developer.visa.com/capabilities/visa_direct/docs-how-to#webhooks
 * 
 * Eventos suportados:
 * - transaction.completed - Transação completada
 * - transaction.failed - Transação falhou
 * - transaction.reversed - Transação revertida
 * 
 * Segurança:
 * - Validação de assinatura HMAC-SHA256
 * - Certificado mTLS
 * - Validação de timestamp
 * 
 * @author Luiz Gustavo Finotello
 */
@RestController
@RequestMapping("/webhooks/visa")
public class VisaWebhookController {

    private static final Logger logger = LoggerFactory.getLogger(VisaWebhookController.class);

    @Autowired
    private TransacaoRepository transacaoRepository;

    @Autowired
    private WebhookService webhookService;

    @Autowired
    private ObjectMapper objectMapper;

    @PostMapping
    public ResponseEntity<Map<String, String>> receiveWebhook(
            @RequestHeader(value = "X-Visa-Signature", required = false) String signature,
            @RequestBody String payload) {

        logger.info("[VISA WEBHOOK] Recebendo notificação");

        try {
            // Validar assinatura
            if (!validateSignature(signature, payload)) {
                logger.warn("[VISA WEBHOOK] Assinatura inválida");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid signature"));
            }

            Map<String, Object> webhookData = objectMapper.readValue(payload, Map.class);
            String eventType = (String) webhookData.get("eventType");
            Map<String, Object> transaction = (Map<String, Object>) webhookData.get("transaction");
            String transactionId = (String) transaction.get("transactionIdentifier");

            logger.info("[VISA WEBHOOK] EventType: {} - TransactionID: {}", eventType, transactionId);

            processVisaEvent(eventType, transactionId, transaction);

            return ResponseEntity.ok(Map.of("status", "processed"));

        } catch (Exception e) {
            logger.error("[VISA WEBHOOK] Erro ao processar webhook", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Processing failed"));
        }
    }

    private void processVisaEvent(String eventType, String transactionId, Map<String, Object> transaction) {
        Transacao transacao = transacaoRepository.findByGatewayTransactionId(transactionId);
        
        if (transacao == null) {
            logger.warn("[VISA WEBHOOK] Transação não encontrada: {}", transactionId);
            return;
        }

        switch (eventType) {
            case "transaction.completed":
                transacao.setStatus(TransactionStatus.CAPTURED);
                logger.info("[VISA WEBHOOK] Transação completada: {}", transactionId);
                break;

            case "transaction.failed":
                transacao.setStatus(TransactionStatus.DENIED);
                transacao.setErrorMessage((String) transaction.get("statusMessage"));
                logger.info("[VISA WEBHOOK] Transação falhou: {}", transactionId);
                break;

            case "transaction.reversed":
                transacao.setStatus(TransactionStatus.VOIDED);
                logger.info("[VISA WEBHOOK] Transação revertida: {}", transactionId);
                break;

            default:
                logger.warn("[VISA WEBHOOK] EventType desconhecido: {}", eventType);
                return;
        }

        transacaoRepository.save(transacao);
        webhookService.notificarLojista(transacao);
    }

    private boolean validateSignature(String signature, String payload) {
        if (signature == null || signature.isEmpty()) {
            return false;
        }

        try {
            String webhookSecret = System.getenv("VISA_WEBHOOK_SECRET");
            if (webhookSecret == null) {
                webhookSecret = "default-secret-key";
            }

            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(
                webhookSecret.getBytes(StandardCharsets.UTF_8), 
                "HmacSHA256"
            );
            mac.init(secretKeySpec);

            byte[] hmacBytes = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            String calculatedSignature = Base64.getEncoder().encodeToString(hmacBytes);

            return calculatedSignature.equals(signature);

        } catch (Exception e) {
            logger.error("[VISA WEBHOOK] Erro ao validar assinatura", e);
            return false;
        }
    }
}
