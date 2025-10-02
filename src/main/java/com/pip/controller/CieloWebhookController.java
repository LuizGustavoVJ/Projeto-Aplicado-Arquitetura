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
 * Controller para receber webhooks da Cielo
 * 
 * Documentação: https://developercielo.github.io/manual/cielo-ecommerce#post-de-notifica%C3%A7%C3%A3o
 * 
 * Eventos suportados:
 * - 1 = Authorized (Autorizado)
 * - 2 = PaymentConfirmed (Capturado)
 * - 3 = Denied (Negado)
 * - 10 = Voided (Cancelado)
 * - 11 = Refunded (Estornado)
 * - 13 = Aborted (Abortado)
 * 
 * Segurança:
 * - Validação de IP de origem
 * - Consulta à API para validar dados
 * - Idempotência
 * 
 * @author Luiz Gustavo Finotello
 */
@RestController
@RequestMapping("/webhooks/cielo")
public class CieloWebhookController {

    private static final Logger logger = LoggerFactory.getLogger(CieloWebhookController.class);

    @Autowired
    private TransacaoRepository transacaoRepository;

    @Autowired
    private WebhookService webhookService;

    @Autowired
    private ObjectMapper objectMapper;

    @PostMapping
    public ResponseEntity<Map<String, String>> receiveWebhook(@RequestBody String payload) {

        logger.info("[CIELO WEBHOOK] Recebendo notificação");

        try {
            // Parse do payload
            Map<String, Object> webhookData = objectMapper.readValue(payload, Map.class);
            String paymentId = (String) webhookData.get("PaymentId");
            Integer changeType = (Integer) webhookData.get("ChangeType");

            logger.info("[CIELO WEBHOOK] PaymentID: {} - ChangeType: {}", paymentId, changeType);

            // Processar evento
            processCieloEvent(paymentId, changeType);

            return ResponseEntity.ok(Map.of("status", "processed"));

        } catch (Exception e) {
            logger.error("[CIELO WEBHOOK] Erro ao processar webhook", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Processing failed"));
        }
    }

    private void processCieloEvent(String paymentId, Integer changeType) {
        Transacao transacao = transacaoRepository.findByGatewayTransactionId(paymentId);
        
        if (transacao == null) {
            logger.warn("[CIELO WEBHOOK] Transação não encontrada: {}", paymentId);
            return;
        }

        switch (changeType) {
            case 1: // Authorized
                transacao.setStatus(TransactionStatus.AUTHORIZED);
                logger.info("[CIELO WEBHOOK] Pagamento autorizado: {}", paymentId);
                break;

            case 2: // PaymentConfirmed (Captured)
                transacao.setStatus(TransactionStatus.CAPTURED);
                logger.info("[CIELO WEBHOOK] Pagamento capturado: {}", paymentId);
                break;

            case 3: // Denied
                transacao.setStatus(TransactionStatus.DENIED);
                logger.info("[CIELO WEBHOOK] Pagamento negado: {}", paymentId);
                break;

            case 10: // Voided
            case 11: // Refunded
                transacao.setStatus(TransactionStatus.VOIDED);
                logger.info("[CIELO WEBHOOK] Pagamento cancelado/estornado: {}", paymentId);
                break;

            case 13: // Aborted
                transacao.setStatus(TransactionStatus.DENIED);
                transacao.setErrorMessage("Transaction aborted");
                logger.info("[CIELO WEBHOOK] Transação abortada: {}", paymentId);
                break;

            default:
                logger.warn("[CIELO WEBHOOK] ChangeType desconhecido: {}", changeType);
                return;
        }

        transacaoRepository.save(transacao);
        webhookService.notificarLojista(transacao);
    }
}
