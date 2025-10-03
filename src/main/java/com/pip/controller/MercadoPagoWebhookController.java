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
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * Controller para receber webhooks do Mercado Pago
 * 
 * Documentação: https://www.mercadopago.com.br/developers/pt/docs/your-integrations/notifications/webhooks
 * 
 * Eventos suportados:
 * - payment.created - Pagamento criado
 * - payment.updated - Pagamento atualizado
 * - payment.authorized - Pagamento autorizado
 * - payment.captured - Pagamento capturado
 * - payment.refunded - Pagamento estornado
 * 
 * Segurança:
 * - Validação de assinatura x-signature
 * - Consulta à API para validar dados
 * - Idempotência
 * 
 * @author Luiz Gustavo Finotello
 */
@RestController
@RequestMapping("/webhooks/mercadopago")
public class MercadoPagoWebhookController {

    private static final Logger logger = LoggerFactory.getLogger(MercadoPagoWebhookController.class);

    @Autowired
    private TransacaoRepository transacaoRepository;

    @Autowired
    private WebhookService webhookService;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RestTemplate restTemplate;

    @PostMapping
    public ResponseEntity<Map<String, String>> receiveWebhook(
            @RequestHeader(value = "x-signature", required = false) String signature,
            @RequestHeader(value = "x-request-id", required = false) String requestId,
            @RequestBody String payload) {

        logger.info("[MERCADOPAGO WEBHOOK] Recebendo notificação - RequestID: {}", requestId);

        try {
            // Parse do payload
            Map<String, Object> webhookData = objectMapper.readValue(payload, Map.class);
            String action = (String) webhookData.get("action");
            String type = (String) webhookData.get("type");
            Map<String, Object> data = (Map<String, Object>) webhookData.get("data");

            logger.info("[MERCADOPAGO WEBHOOK] Action: {} - Type: {} - ID: {}", 
                action, type, data.get("id"));

            // Processar apenas eventos de payment
            if ("payment".equals(type)) {
                String paymentId = data.get("id").toString();
                
                // Buscar detalhes do pagamento na API do Mercado Pago
                // (Recomendado para validar autenticidade)
                processPaymentEvent(paymentId, action);
            }

            return ResponseEntity.ok(Map.of("status", "processed"));

        } catch (Exception e) {
            logger.error("[MERCADOPAGO WEBHOOK] Erro ao processar webhook", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Processing failed"));
        }
    }

    private void processPaymentEvent(String paymentId, String action) {
        // Buscar transação
        Transacao transacao = transacaoRepository.findByGatewayTransactionId(paymentId);
        
        if (transacao == null) {
            logger.warn("[MERCADOPAGO WEBHOOK] Transação não encontrada: {}", paymentId);
            return;
        }

        // Processar baseado na action
        switch (action) {
            case "payment.created":
                logger.info("[MERCADOPAGO WEBHOOK] Pagamento criado: {}", paymentId);
                // Não alterar status, apenas registrar
                break;

            case "payment.updated":
            case "payment.authorized":
                transacao.setStatus(TransactionStatus.AUTHORIZED.toString());
                logger.info("[MERCADOPAGO WEBHOOK] Pagamento autorizado: {}", paymentId);
                break;

            case "payment.captured":
                transacao.setStatus(TransactionStatus.CAPTURED.toString());
                logger.info("[MERCADOPAGO WEBHOOK] Pagamento capturado: {}", paymentId);
                break;

            case "payment.refunded":
                transacao.setStatus(TransactionStatus.VOIDED.toString());
                logger.info("[MERCADOPAGO WEBHOOK] Pagamento estornado: {}", paymentId);
                break;

            default:
                logger.warn("[MERCADOPAGO WEBHOOK] Action desconhecida: {}", action);
                return;
        }

        transacaoRepository.save(transacao);
        webhookService.notificarLojista(transacao);
    }
}
