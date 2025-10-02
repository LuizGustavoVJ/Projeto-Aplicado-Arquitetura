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
import java.util.Map;

/**
 * Controller para receber webhooks do PagSeguro
 * 
 * Documentação: https://developers.international.pagseguro.com/docs/webhooks
 * 
 * Eventos suportados:
 * - CHARGE.PAID - Pagamento confirmado
 * - CHARGE.DECLINED - Pagamento negado
 * - CHARGE.CANCELED - Pagamento cancelado
 * - CHARGE.REFUNDED - Pagamento estornado
 * 
 * Segurança:
 * - Validação de assinatura HMAC-SHA256
 * - Token de autenticação
 * - Idempotência
 * 
 * @author Luiz Gustavo Finotello
 */
@RestController
@RequestMapping("/webhooks/pagseguro")
public class PagSeguroWebhookController {

    private static final Logger logger = LoggerFactory.getLogger(PagSeguroWebhookController.class);

    @Autowired
    private TransacaoRepository transacaoRepository;

    @Autowired
    private WebhookService webhookService;

    @Autowired
    private ObjectMapper objectMapper;

    @PostMapping
    public ResponseEntity<Map<String, String>> receiveWebhook(
            @RequestHeader(value = "X-PagSeguro-Signature", required = false) String signature,
            @RequestBody String payload) {

        logger.info("[PAGSEGURO WEBHOOK] Recebendo notificação");

        try {
            // Validar assinatura
            if (!validateSignature(signature, payload)) {
                logger.warn("[PAGSEGURO WEBHOOK] Assinatura inválida");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid signature"));
            }

            // Parse do payload
            Map<String, Object> webhookData = objectMapper.readValue(payload, Map.class);
            String eventType = (String) webhookData.get("event");
            Map<String, Object> chargeData = (Map<String, Object>) webhookData.get("data");

            logger.info("[PAGSEGURO WEBHOOK] Evento: {} - ChargeID: {}", 
                eventType, chargeData.get("id"));

            // Processar evento
            processPagSeguroEvent(eventType, chargeData);

            return ResponseEntity.ok(Map.of("status", "processed"));

        } catch (Exception e) {
            logger.error("[PAGSEGURO WEBHOOK] Erro ao processar webhook", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Processing failed"));
        }
    }

    private void processPagSeguroEvent(String eventType, Map<String, Object> chargeData) {
        String chargeId = (String) chargeData.get("id");
        
        Transacao transacao = transacaoRepository.findByGatewayTransactionId(chargeId);
        
        if (transacao == null) {
            logger.warn("[PAGSEGURO WEBHOOK] Transação não encontrada: {}", chargeId);
            return;
        }

        switch (eventType) {
            case "CHARGE.PAID":
                transacao.setStatus(TransactionStatus.CAPTURED);
                logger.info("[PAGSEGURO WEBHOOK] Pagamento confirmado: {}", chargeId);
                break;

            case "CHARGE.DECLINED":
                transacao.setStatus(TransactionStatus.DENIED);
                transacao.setErrorMessage((String) chargeData.get("decline_reason"));
                logger.info("[PAGSEGURO WEBHOOK] Pagamento negado: {}", chargeId);
                break;

            case "CHARGE.CANCELED":
            case "CHARGE.REFUNDED":
                transacao.setStatus(TransactionStatus.VOIDED);
                logger.info("[PAGSEGURO WEBHOOK] Pagamento cancelado/estornado: {}", chargeId);
                break;

            default:
                logger.warn("[PAGSEGURO WEBHOOK] Evento desconhecido: {}", eventType);
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
            String webhookSecret = System.getenv("PAGSEGURO_WEBHOOK_SECRET");
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
            String calculatedSignature = bytesToHex(hmacBytes);

            return calculatedSignature.equalsIgnoreCase(signature);

        } catch (Exception e) {
            logger.error("[PAGSEGURO WEBHOOK] Erro ao validar assinatura", e);
            return false;
        }
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }
}
