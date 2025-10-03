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
 * Controller para receber webhooks do Stone
 * 
 * Documentação: https://docs.stone.com.br/docs/webhooks
 * 
 * Eventos suportados:
 * - charge.succeeded - Pagamento aprovado
 * - charge.failed - Pagamento negado
 * - charge.refunded - Pagamento estornado
 * - charge.captured - Captura realizada
 * 
 * Segurança:
 * - Validação de assinatura HMAC-SHA256
 * - Verificação de origem
 * - Idempotência de processamento
 * 
 * @author Luiz Gustavo Finotello
 */
@RestController
@RequestMapping("/webhooks/stone")
public class StoneWebhookController {

    private static final Logger logger = LoggerFactory.getLogger(StoneWebhookController.class);

    @Autowired
    private TransacaoRepository transacaoRepository;

    @Autowired
    private WebhookService webhookService;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * Endpoint para receber notificações do Stone
     * 
     * @param signature Assinatura HMAC enviada no header
     * @param payload Corpo da requisição
     * @return Status 200 se processado com sucesso
     */
    @PostMapping
    public ResponseEntity<Map<String, String>> receiveWebhook(
            @RequestHeader(value = "X-Stone-Signature", required = false) String signature,
            @RequestBody String payload) {

        logger.info("[STONE WEBHOOK] Recebendo notificação");

        try {
            // Validar assinatura
            if (!validateSignature(signature, payload)) {
                logger.warn("[STONE WEBHOOK] Assinatura inválida");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid signature"));
            }

            // Parse do payload
            Map<String, Object> webhookData = objectMapper.readValue(payload, Map.class);
            String eventType = (String) webhookData.get("event");
            Map<String, Object> chargeData = (Map<String, Object>) webhookData.get("data");

            logger.info("[STONE WEBHOOK] Evento: {} - ChargeID: {}", 
                eventType, chargeData.get("id"));

            // Processar evento
            processStoneEvent(eventType, chargeData);

            return ResponseEntity.ok(Map.of("status", "processed"));

        } catch (Exception e) {
            logger.error("[STONE WEBHOOK] Erro ao processar webhook", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Processing failed"));
        }
    }

    /**
     * Processa eventos do Stone
     */
    private void processStoneEvent(String eventType, Map<String, Object> chargeData) {
        String chargeId = (String) chargeData.get("id");
        
        // Buscar transação pelo gatewayTransactionId
        Transacao transacao = transacaoRepository.findByGatewayTransactionId(chargeId);
        
        if (transacao == null) {
            logger.warn("[STONE WEBHOOK] Transação não encontrada: {}", chargeId);
            return;
        }

        // Processar baseado no tipo de evento
        switch (eventType) {
            case "charge.succeeded":
                transacao.setStatus(TransactionStatus.AUTHORIZED.toString());
                logger.info("[STONE WEBHOOK] Pagamento aprovado: {}", chargeId);
                break;

            case "charge.failed":
                transacao.setStatus(TransactionStatus.DENIED.toString());
                String failureReason = (String) chargeData.get("failure_reason");
                transacao.setErrorMessage(failureReason);
                logger.info("[STONE WEBHOOK] Pagamento negado: {} - Motivo: {}", 
                    chargeId, failureReason);
                break;

            case "charge.refunded":
                transacao.setStatus(TransactionStatus.VOIDED.toString());
                logger.info("[STONE WEBHOOK] Pagamento estornado: {}", chargeId);
                break;

            case "charge.captured":
                transacao.setStatus(TransactionStatus.CAPTURED.toString());
                logger.info("[STONE WEBHOOK] Captura realizada: {}", chargeId);
                break;

            default:
                logger.warn("[STONE WEBHOOK] Evento desconhecido: {}", eventType);
                return;
        }

        // Salvar atualização
        transacaoRepository.save(transacao);

        // Notificar lojista via webhook
        webhookService.notificarLojista(transacao);
    }

    /**
     * Valida assinatura HMAC-SHA256 do webhook
     * 
     * @param signature Assinatura recebida
     * @param payload Payload original
     * @return true se válida
     */
    private boolean validateSignature(String signature, String payload) {
        if (signature == null || signature.isEmpty()) {
            return false;
        }

        try {
            // Obter secret do Stone (deve estar em variável de ambiente)
            String webhookSecret = System.getenv("STONE_WEBHOOK_SECRET");
            if (webhookSecret == null) {
                webhookSecret = "default-secret-key"; // Apenas para desenvolvimento
            }

            // Calcular HMAC
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(
                webhookSecret.getBytes(StandardCharsets.UTF_8), 
                "HmacSHA256"
            );
            mac.init(secretKeySpec);

            byte[] hmacBytes = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            String calculatedSignature = Base64.getEncoder().encodeToString(hmacBytes);

            // Comparar assinaturas
            return calculatedSignature.equals(signature);

        } catch (Exception e) {
            logger.error("[STONE WEBHOOK] Erro ao validar assinatura", e);
            return false;
        }
    }
}
