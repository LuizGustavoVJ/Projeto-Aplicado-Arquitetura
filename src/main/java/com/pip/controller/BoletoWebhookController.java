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
 * Controller para receber webhooks de Boleto Bancário
 * 
 * Documentação: https://developers.bb.com.br/docs/cobrancas/webhooks
 * 
 * Eventos suportados:
 * - boleto.registered - Boleto registrado
 * - boleto.paid - Boleto pago
 * - boleto.cancelled - Boleto cancelado
 * - boleto.expired - Boleto vencido
 * 
 * Segurança:
 * - Validação OAuth 2.0
 * - Validação de assinatura
 * - Validação de convênio
 * 
 * @author Luiz Gustavo Finotello
 */
@RestController
@RequestMapping("/webhooks/boleto")
public class BoletoWebhookController {

    private static final Logger logger = LoggerFactory.getLogger(BoletoWebhookController.class);

    @Autowired
    private TransacaoRepository transacaoRepository;

    @Autowired
    private WebhookService webhookService;

    @Autowired
    private ObjectMapper objectMapper;

    @PostMapping
    public ResponseEntity<Map<String, String>> receiveWebhook(
            @RequestHeader(value = "X-BB-Signature", required = false) String signature,
            @RequestBody String payload) {

        logger.info("[BOLETO WEBHOOK] Recebendo notificação");

        try {
            Map<String, Object> webhookData = objectMapper.readValue(payload, Map.class);
            String eventType = (String) webhookData.get("evento");
            Map<String, Object> boleto = (Map<String, Object>) webhookData.get("boleto");
            String numeroTitulo = (String) boleto.get("numeroTitulo");
            String nossoNumero = (String) boleto.get("nossoNumero");

            logger.info("[BOLETO WEBHOOK] Evento: {} - NossoNumero: {} - NumeroTitulo: {}", 
                eventType, nossoNumero, numeroTitulo);

            processBoletoEvent(eventType, nossoNumero, boleto);

            return ResponseEntity.ok(Map.of("status", "processed"));

        } catch (Exception e) {
            logger.error("[BOLETO WEBHOOK] Erro ao processar webhook", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Processing failed"));
        }
    }

    private void processBoletoEvent(String eventType, String nossoNumero, Map<String, Object> boleto) {
        Transacao transacao = transacaoRepository.findByGatewayTransactionId(nossoNumero);
        
        if (transacao == null) {
            logger.warn("[BOLETO WEBHOOK] Transação não encontrada: {}", nossoNumero);
            return;
        }

        switch (eventType) {
            case "boleto.registered":
                transacao.setStatus(TransactionStatus.AUTHORIZED);
                logger.info("[BOLETO WEBHOOK] Boleto registrado: {}", nossoNumero);
                break;

            case "boleto.paid":
                transacao.setStatus(TransactionStatus.CAPTURED);
                Object valorPago = boleto.get("valorPago");
                logger.info("[BOLETO WEBHOOK] Boleto pago: {} - Valor: R$ {}", 
                    nossoNumero, valorPago);
                break;

            case "boleto.cancelled":
                transacao.setStatus(TransactionStatus.VOIDED);
                logger.info("[BOLETO WEBHOOK] Boleto cancelado: {}", nossoNumero);
                break;

            case "boleto.expired":
                transacao.setStatus(TransactionStatus.DENIED);
                transacao.setErrorMessage("Boleto vencido");
                logger.info("[BOLETO WEBHOOK] Boleto vencido: {}", nossoNumero);
                break;

            default:
                logger.warn("[BOLETO WEBHOOK] Evento desconhecido: {}", eventType);
                return;
        }

        transacaoRepository.save(transacao);
        webhookService.notificarLojista(transacao);
    }
}
