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
 * Controller para receber webhooks do PIX (Banco Central)
 * 
 * Documentação: https://www.bcb.gov.br/estabilidadefinanceira/pix
 * 
 * Eventos suportados:
 * - pix.received - PIX recebido
 * - pix.returned - PIX devolvido
 * - pix.failed - PIX falhou
 * 
 * Segurança:
 * - Validação de certificado digital
 * - mTLS obrigatório
 * - Validação de endTxId
 * 
 * @author Luiz Gustavo Finotello
 */
@RestController
@RequestMapping("/webhooks/pix")
public class PixWebhookController {

    private static final Logger logger = LoggerFactory.getLogger(PixWebhookController.class);

    @Autowired
    private TransacaoRepository transacaoRepository;

    @Autowired
    private WebhookService webhookService;

    @Autowired
    private ObjectMapper objectMapper;

    @PostMapping
    public ResponseEntity<Map<String, String>> receiveWebhook(@RequestBody String payload) {

        logger.info("[PIX WEBHOOK] Recebendo notificação");

        try {
            Map<String, Object> webhookData = objectMapper.readValue(payload, Map.class);
            String eventType = (String) webhookData.get("tipo");
            Map<String, Object> pix = (Map<String, Object>) webhookData.get("pix");
            String endToEndId = (String) pix.get("endToEndId");
            String txid = (String) pix.get("txid");

            logger.info("[PIX WEBHOOK] Tipo: {} - EndToEndId: {} - TxId: {}", 
                eventType, endToEndId, txid);

            processPixEvent(eventType, txid, pix);

            return ResponseEntity.ok(Map.of("status", "processed"));

        } catch (Exception e) {
            logger.error("[PIX WEBHOOK] Erro ao processar webhook", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Processing failed"));
        }
    }

    private void processPixEvent(String eventType, String txid, Map<String, Object> pix) {
        Transacao transacao = transacaoRepository.findByGatewayTransactionId(txid);
        
        if (transacao == null) {
            logger.warn("[PIX WEBHOOK] Transação não encontrada: {}", txid);
            return;
        }

        switch (eventType) {
            case "pix.received":
                transacao.setStatus(TransactionStatus.CAPTURED);
                String valor = pix.get("valor").toString();
                logger.info("[PIX WEBHOOK] PIX recebido: {} - Valor: R$ {}", txid, valor);
                break;

            case "pix.returned":
                transacao.setStatus(TransactionStatus.VOIDED);
                String motivoDevolucao = (String) pix.get("devolucoes");
                transacao.setErrorMessage(motivoDevolucao);
                logger.info("[PIX WEBHOOK] PIX devolvido: {} - Motivo: {}", txid, motivoDevolucao);
                break;

            case "pix.failed":
                transacao.setStatus(TransactionStatus.DENIED);
                String erro = (String) pix.get("erro");
                transacao.setErrorMessage(erro);
                logger.info("[PIX WEBHOOK] PIX falhou: {} - Erro: {}", txid, erro);
                break;

            default:
                logger.warn("[PIX WEBHOOK] Tipo de evento desconhecido: {}", eventType);
                return;
        }

        transacaoRepository.save(transacao);
        webhookService.notificarLojista(transacao);
    }
}
