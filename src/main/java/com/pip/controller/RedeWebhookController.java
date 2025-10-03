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
 * Controller para receber webhooks da Rede
 * 
 * Documentação: https://developer.userede.com.br/e-rede#tag/Notificacoes
 * 
 * Eventos suportados:
 * - AUTHORIZED - Transação autorizada
 * - CAPTURED - Transação capturada
 * - CANCELLED - Transação cancelada
 * - DENIED - Transação negada
 * 
 * @author Luiz Gustavo Finotello
 */
@RestController
@RequestMapping("/webhooks/rede")
public class RedeWebhookController {

    private static final Logger logger = LoggerFactory.getLogger(RedeWebhookController.class);

    @Autowired
    private TransacaoRepository transacaoRepository;

    @Autowired
    private WebhookService webhookService;

    @Autowired
    private ObjectMapper objectMapper;

    @PostMapping
    public ResponseEntity<Map<String, String>> receiveWebhook(
            @RequestHeader(value = "X-Rede-Signature", required = false) String signature,
            @RequestBody String payload) {

        logger.info("[REDE WEBHOOK] Recebendo notificação");

        try {
            Map<String, Object> webhookData = objectMapper.readValue(payload, Map.class);
            String tid = (String) webhookData.get("tid");
            String status = (String) webhookData.get("status");

            logger.info("[REDE WEBHOOK] TID: {} - Status: {}", tid, status);

            processRedeEvent(tid, status);

            return ResponseEntity.ok(Map.of("status", "processed"));

        } catch (Exception e) {
            logger.error("[REDE WEBHOOK] Erro ao processar webhook", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Processing failed"));
        }
    }

    private void processRedeEvent(String tid, String status) {
        Transacao transacao = transacaoRepository.findByGatewayTransactionId(tid);
        
        if (transacao == null) {
            logger.warn("[REDE WEBHOOK] Transação não encontrada: {}", tid);
            return;
        }

        switch (status.toUpperCase()) {
            case "AUTHORIZED":
                transacao.setStatus(TransactionStatus.AUTHORIZED.toString());
                logger.info("[REDE WEBHOOK] Transação autorizada: {}", tid);
                break;

            case "CAPTURED":
                transacao.setStatus(TransactionStatus.CAPTURED.toString());
                logger.info("[REDE WEBHOOK] Transação capturada: {}", tid);
                break;

            case "CANCELLED":
                transacao.setStatus(TransactionStatus.VOIDED.toString());
                logger.info("[REDE WEBHOOK] Transação cancelada: {}", tid);
                break;

            case "DENIED":
                transacao.setStatus(TransactionStatus.DENIED.toString());
                logger.info("[REDE WEBHOOK] Transação negada: {}", tid);
                break;

            default:
                logger.warn("[REDE WEBHOOK] Status desconhecido: {}", status);
                return;
        }

        transacaoRepository.save(transacao);
        webhookService.notificarLojista(transacao);
    }
}
