package com.pip.gateway;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pip.dto.AuthorizationRequest;
import com.pip.dto.CaptureRequest;
import com.pip.dto.VoidRequest;
import com.pip.dto.PaymentResponse;
import com.pip.model.Gateway;
import com.pip.model.Transacao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Adaptador para integração com PIX
 * 
 * Implementa comunicação com API PIX (Banco Central) para:
 * - Geração de QR Code PIX
 * - Consulta de status de pagamento
 * - Devolução de pagamentos
 * 
 * Documentação: https://www.bcb.gov.br/estabilidadefinanceira/pix
 * 
 * @author Luiz Gustavo Finotello
 */
@Component
public class PixAdapter implements GatewayAdapter {

    private static final Logger logger = LoggerFactory.getLogger(PixAdapter.class);
    private static final String GATEWAY_CODE = "PIX";

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public PaymentResponse authorize(Gateway gateway, AuthorizationRequest request, Transacao transacao) {
        logger.info("Iniciando geração de PIX para transação: {}", transacao.getTransactionId());

        try {
            // Construir payload PIX
            Map<String, Object> payload = new HashMap<>();
            payload.put("calendario", Map.of("expiracao", 3600)); // 1 hora de expiração
            payload.put("devedor", Map.of(
                "cpf", request.getCustomer() != null ? request.getCustomer().get("document") : "",
                "nome", request.getCustomer() != null ? request.getCustomer().get("name") : ""
            ));
            payload.put("valor", Map.of("original", String.format("%.2f", request.getAmount())));
            payload.put("chave", gateway.getPixKey()); // Chave PIX do lojista
            payload.put("solicitacaoPagador", request.getDescription());

            // Configurar headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + gateway.getMerchantKey());

            // Fazer requisição para criar cobrança PIX
            String txid = UUID.randomUUID().toString().replace("-", "");
            String url = String.format("%s/v2/cob/%s", gateway.getApiUrl(), txid);
            
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);
            
            ResponseEntity<Map> response = restTemplate.exchange(
                url,
                HttpMethod.PUT,
                entity,
                Map.class
            );

            // Processar resposta
            if (response.getStatusCode() == HttpStatus.CREATED || response.getStatusCode() == HttpStatus.OK) {
                Map<String, Object> responseBody = response.getBody();
                Map<String, Object> loc = (Map<String, Object>) responseBody.get("loc");

                PaymentResponse paymentResponse = new PaymentResponse();
                paymentResponse.setSuccess(true);
                paymentResponse.setStatus("PENDING"); // PIX começa como pendente
                paymentResponse.setTransactionId(transacao.getTransactionId());
                paymentResponse.setGatewayTransactionId(txid);
                
                // Informações específicas do PIX
                Map<String, Object> pixData = new HashMap<>();
                pixData.put("qrcode", responseBody.get("pixCopiaECola")); // QR Code em texto
                pixData.put("qrcode_image", loc != null ? loc.get("qrcode") : null); // URL da imagem do QR Code
                pixData.put("txid", txid);
                pixData.put("expiration", responseBody.get("calendario"));
                paymentResponse.setAdditionalData(pixData);
                
                paymentResponse.setTimestamp(ZonedDateTime.now());

                logger.info("PIX gerado com sucesso: {}", txid);
                return paymentResponse;
            } else {
                return createErrorResponse("PIX_GENERATION_FAILED", "Falha na geração do PIX");
            }

        } catch (Exception e) {
            logger.error("Erro na geração do PIX: {}", e.getMessage(), e);
            return createErrorResponse("GATEWAY_ERROR", e.getMessage());
        }
    }

    @Override
    public PaymentResponse capture(Gateway gateway, CaptureRequest request, Transacao transacao) {
        logger.info("Consultando status do PIX para transação: {}", transacao.getTransactionId());

        try {
            // Configurar headers
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + gateway.getMerchantKey());

            // Consultar status da cobrança PIX
            String url = String.format("%s/v2/cob/%s",
                gateway.getApiUrl(),
                transacao.getGatewayTransactionId()
            );

            HttpEntity<Void> entity = new HttpEntity<>(headers);
            
            ResponseEntity<Map> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                Map.class
            );

            // Processar resposta
            if (response.getStatusCode() == HttpStatus.OK) {
                Map<String, Object> responseBody = response.getBody();
                String status = (String) responseBody.get("status");

                PaymentResponse paymentResponse = new PaymentResponse();
                paymentResponse.setTransactionId(transacao.getTransactionId());
                paymentResponse.setGatewayTransactionId(transacao.getGatewayTransactionId());
                paymentResponse.setTimestamp(ZonedDateTime.now());

                // Mapear status do PIX
                if ("CONCLUIDA".equals(status)) {
                    paymentResponse.setSuccess(true);
                    paymentResponse.setStatus("CAPTURED");
                    logger.info("PIX pago com sucesso: {}", transacao.getGatewayTransactionId());
                } else if ("ATIVA".equals(status)) {
                    paymentResponse.setSuccess(false);
                    paymentResponse.setStatus("PENDING");
                    paymentResponse.setErrorMessage("PIX ainda não foi pago");
                } else {
                    paymentResponse.setSuccess(false);
                    paymentResponse.setStatus("EXPIRED");
                    paymentResponse.setErrorMessage("PIX expirado ou removido");
                }

                return paymentResponse;
            } else {
                return createErrorResponse("PIX_QUERY_FAILED", "Falha na consulta do PIX");
            }

        } catch (Exception e) {
            logger.error("Erro na consulta do PIX: {}", e.getMessage(), e);
            return createErrorResponse("GATEWAY_ERROR", e.getMessage());
        }
    }

    @Override
    public PaymentResponse voidTransaction(Gateway gateway, VoidRequest request, Transacao transacao) {
        logger.info("Iniciando devolução PIX para transação: {}", transacao.getTransactionId());

        try {
            // Construir payload de devolução
            Map<String, Object> payload = new HashMap<>();
            payload.put("valor", String.format("%.2f", request.getAmount()));

            // Configurar headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + gateway.getMerchantKey());

            // Fazer requisição de devolução
            String e2eid = transacao.getGatewayTransactionId();
            String idDevolucao = UUID.randomUUID().toString();
            String url = String.format("%s/v2/pix/%s/devolucao/%s",
                gateway.getApiUrl(),
                e2eid,
                idDevolucao
            );

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);
            
            ResponseEntity<Map> response = restTemplate.exchange(
                url,
                HttpMethod.PUT,
                entity,
                Map.class
            );

            // Processar resposta
            if (response.getStatusCode() == HttpStatus.CREATED || response.getStatusCode() == HttpStatus.OK) {
                PaymentResponse paymentResponse = new PaymentResponse();
                paymentResponse.setSuccess(true);
                paymentResponse.setStatus("VOIDED");
                paymentResponse.setTransactionId(transacao.getTransactionId());
                paymentResponse.setGatewayTransactionId(transacao.getGatewayTransactionId());
                paymentResponse.setTimestamp(ZonedDateTime.now());

                logger.info("Devolução PIX bem-sucedida: {}", transacao.getGatewayTransactionId());
                return paymentResponse;
            } else {
                return createErrorResponse("PIX_REFUND_FAILED", "Falha na devolução PIX");
            }

        } catch (Exception e) {
            logger.error("Erro na devolução PIX: {}", e.getMessage(), e);
            return createErrorResponse("GATEWAY_ERROR", e.getMessage());
        }
    }

    @Override
    public boolean healthCheck(Gateway gateway) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + gateway.getMerchantKey());

            String url = gateway.getApiUrl() + "/v2/health";
            HttpEntity<Void> entity = new HttpEntity<>(headers);
            
            ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                String.class
            );

            return response.getStatusCode().is2xxSuccessful();

        } catch (Exception e) {
            logger.warn("Health check PIX falhou: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public String getGatewayCode() {
        return GATEWAY_CODE;
    }

    private PaymentResponse createErrorResponse(String errorCode, String errorMessage) {
        PaymentResponse response = new PaymentResponse();
        response.setSuccess(false);
        response.setErrorCode(errorCode);
        response.setErrorMessage(errorMessage);
        response.setTimestamp(ZonedDateTime.now());
        return response;
    }
}
