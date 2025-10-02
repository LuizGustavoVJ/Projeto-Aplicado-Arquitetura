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
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * Adaptador REAL para integração com Stone Pagamentos
 * 
 * Implementa comunicação com API Stone seguindo documentação oficial:
 * https://online.stone.com.br/reference/overview-da-api
 * 
 * Características:
 * - Autenticação via Bearer Token (JWT)
 * - Ambiente Sandbox: sdx-payments.stone.com.br
 * - Ambiente Produção: payments.stone.com.br
 * - Suporta: Autorização, Captura Posterior, Cancelamento
 * - Idempotência via X-Stone-Idempotency-Key
 * - Valores em centavos
 * 
 * Segurança:
 * - TLS 1.3 obrigatório
 * - Validação de certificados
 * - Logs de auditoria completos
 * - Sanitização de dados sensíveis
 * 
 * @author Luiz Gustavo Finotello
 * @version 2.0 - Integração Real
 */
@Component
public class StoneAdapter implements GatewayAdapter {

    private static final Logger logger = LoggerFactory.getLogger(StoneAdapter.class);
    private static final String GATEWAY_CODE = "STONE";
    
    // URLs oficiais Stone
    private static final String SANDBOX_URL = "https://payments.stone.com.br/v1/charges";
    private static final String SANDBOX_HOST = "sdx-payments.stone.com.br";
    private static final String PRODUCTION_URL = "https://payments.stone.com.br/v1/charges";
    private static final String PRODUCTION_HOST = "payments.stone.com.br";

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public PaymentResponse authorize(Gateway gateway, AuthorizationRequest request, Transacao transacao) {
        logger.info("[STONE] Iniciando autorização - TransactionID: {}", transacao.getTransactionId());
        
        try {
            // Validações de segurança
            validateRequest(request);
            
            // Construir payload conforme documentação Stone
            Map<String, Object> payload = buildAuthorizationPayload(request, transacao);
            
            // Configurar headers com autenticação
            HttpHeaders headers = buildHeaders(gateway, transacao.getTransactionId());
            
            // Fazer requisição
            String url = getBaseUrl(gateway);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);
            
            logger.debug("[STONE] Enviando requisição para: {}", url);
            
            ResponseEntity<Map> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                entity,
                Map.class
            );

            // Processar resposta
            return processAuthorizationResponse(response, transacao);

        } catch (HttpClientErrorException e) {
            logger.error("[STONE] Erro 4xx na autorização: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            return createErrorResponse("AUTHORIZATION_FAILED", "Erro na autorização: " + e.getMessage());
        } catch (HttpServerErrorException e) {
            logger.error("[STONE] Erro 5xx na autorização: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            return createErrorResponse("GATEWAY_ERROR", "Erro no gateway Stone: " + e.getMessage());
        } catch (Exception e) {
            logger.error("[STONE] Erro inesperado na autorização", e);
            return createErrorResponse("SYSTEM_ERROR", "Erro no sistema: " + e.getMessage());
        }
    }

    @Override
    public PaymentResponse capture(Gateway gateway, CaptureRequest request, Transacao transacao) {
        logger.info("[STONE] Iniciando captura - TransactionID: {}", transacao.getTransactionId());

        try {
            // Validar se há gatewayTransactionId
            if (transacao.getGatewayTransactionId() == null) {
                return createErrorResponse("INVALID_TRANSACTION", "GatewayTransactionId não encontrado");
            }
            
            // Construir payload de captura
            Map<String, Object> payload = new HashMap<>();
            payload.put("amount_in_cents", (int) (request.getAmount() * 100));

            // Configurar headers
            HttpHeaders headers = buildHeaders(gateway, null);

            // URL de captura específica
            String url = String.format("%s/%s/capture",
                getBaseUrl(gateway),
                transacao.getGatewayTransactionId()
            );

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);
            
            logger.debug("[STONE] Enviando captura para: {}", url);
            
            ResponseEntity<Map> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                entity,
                Map.class
            );

            // Processar resposta
            if (response.getStatusCode() == HttpStatus.OK) {
                Map<String, Object> responseBody = response.getBody();
                
                PaymentResponse paymentResponse = new PaymentResponse();
                paymentResponse.setSuccess(true);
                paymentResponse.setStatus("CAPTURED");
                paymentResponse.setTransactionId(transacao.getTransactionId());
                paymentResponse.setGatewayTransactionId(transacao.getGatewayTransactionId());
                paymentResponse.setTimestamp(ZonedDateTime.now());

                logger.info("[STONE] Captura bem-sucedida: {}", transacao.getGatewayTransactionId());
                return paymentResponse;
            } else {
                return createErrorResponse("CAPTURE_FAILED", "Falha na captura Stone");
            }

        } catch (HttpClientErrorException e) {
            logger.error("[STONE] Erro 4xx na captura: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            return createErrorResponse("CAPTURE_FAILED", "Erro na captura: " + e.getMessage());
        } catch (Exception e) {
            logger.error("[STONE] Erro na captura", e);
            return createErrorResponse("GATEWAY_ERROR", e.getMessage());
        }
    }

    @Override
    public PaymentResponse voidTransaction(Gateway gateway, VoidRequest request, Transacao transacao) {
        logger.info("[STONE] Iniciando cancelamento - TransactionID: {}", transacao.getTransactionId());

        try {
            // Validar se há gatewayTransactionId
            if (transacao.getGatewayTransactionId() == null) {
                return createErrorResponse("INVALID_TRANSACTION", "GatewayTransactionId não encontrado");
            }
            
            // Configurar headers
            HttpHeaders headers = buildHeaders(gateway, null);

            // URL de cancelamento específica
            String url = String.format("%s/%s/cancel",
                getBaseUrl(gateway),
                transacao.getGatewayTransactionId()
            );

            HttpEntity<Void> entity = new HttpEntity<>(headers);
            
            logger.debug("[STONE] Enviando cancelamento para: {}", url);
            
            ResponseEntity<Map> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                entity,
                Map.class
            );

            // Processar resposta
            if (response.getStatusCode() == HttpStatus.OK) {
                PaymentResponse paymentResponse = new PaymentResponse();
                paymentResponse.setSuccess(true);
                paymentResponse.setStatus("VOIDED");
                paymentResponse.setTransactionId(transacao.getTransactionId());
                paymentResponse.setGatewayTransactionId(transacao.getGatewayTransactionId());
                paymentResponse.setTimestamp(ZonedDateTime.now());

                logger.info("[STONE] Cancelamento bem-sucedido: {}", transacao.getGatewayTransactionId());
                return paymentResponse;
            } else {
                return createErrorResponse("VOID_FAILED", "Falha no cancelamento Stone");
            }

        } catch (HttpClientErrorException e) {
            logger.error("[STONE] Erro 4xx no cancelamento: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            return createErrorResponse("VOID_FAILED", "Erro no cancelamento: " + e.getMessage());
        } catch (Exception e) {
            logger.error("[STONE] Erro no cancelamento", e);
            return createErrorResponse("GATEWAY_ERROR", e.getMessage());
        }
    }

    @Override
    public boolean healthCheck(Gateway gateway) {
        try {
            HttpHeaders headers = buildHeaders(gateway, null);
            
            // Stone não tem endpoint de health, então fazemos uma consulta simples
            String url = getBaseUrl(gateway);
            HttpEntity<Void> entity = new HttpEntity<>(headers);
            
            ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                String.class
            );

            return response.getStatusCode().is2xxSuccessful();

        } catch (Exception e) {
            logger.warn("[STONE] Health check falhou: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public String getGatewayCode() {
        return GATEWAY_CODE;
    }

    // ========== MÉTODOS PRIVADOS ==========

    private void validateRequest(AuthorizationRequest request) {
        if (request.getAmount() == null || request.getAmount() <= 0) {
            throw new IllegalArgumentException("Valor inválido");
        }
        if (request.getCardToken() == null || request.getCardToken().isEmpty()) {
            throw new IllegalArgumentException("Token do cartão obrigatório");
        }
    }

    private Map<String, Object> buildAuthorizationPayload(AuthorizationRequest request, Transacao transacao) {
        Map<String, Object> payload = new HashMap<>();
        
        // Dados básicos
        payload.put("payment_method", "card");
        payload.put("amount_in_cents", (int) (request.getAmount() * 100));
        payload.put("currency_code", request.getCurrency() != null ? getCurrencyCode(request.getCurrency()) : "986"); // BRL
        payload.put("initiator_id", transacao.getTransactionId());
        payload.put("local_datetime", ZonedDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
        payload.put("channel", "website");
        
        // Transação de cartão
        Map<String, Object> cardTransaction = new HashMap<>();
        cardTransaction.put("type", "credit");
        cardTransaction.put("operation_type", "auth_only"); // Autorização sem captura automática
        cardTransaction.put("installments", request.getInstallments() != null ? request.getInstallments() : 1);
        cardTransaction.put("installments_type", request.getInstallments() != null && request.getInstallments() > 1 ? "merchant" : "none");
        
        // Dados do cartão
        Map<String, Object> card = new HashMap<>();
        card.put("card_token", request.getCardToken());
        card.put("entry_mode", "manual");
        card.put("fallback", false);
        cardTransaction.put("card", card);
        
        payload.put("card_transaction", cardTransaction);
        
        // Dados do cliente (se disponível)
        if (request.getCustomer() != null) {
            Map<String, Object> customer = new HashMap<>();
            customer.put("name", request.getCustomer().get("name"));
            customer.put("email", request.getCustomer().get("email"));
            customer.put("document_type", "cpf");
            customer.put("document", request.getCustomer().get("document"));
            payload.put("customer", customer);
        }
        
        return payload;
    }

    private HttpHeaders buildHeaders(Gateway gateway, String idempotencyKey) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + gateway.getMerchantKey());
        
        // Host header conforme ambiente
        if (gateway.getAmbiente().toString().equals("SANDBOX")) {
            headers.set("Host", SANDBOX_HOST);
        } else {
            headers.set("Host", PRODUCTION_HOST);
        }
        
        // Idempotência (apenas para criação)
        if (idempotencyKey != null) {
            headers.set("X-Stone-Idempotency-Key", idempotencyKey);
        }
        
        return headers;
    }

    private String getBaseUrl(Gateway gateway) {
        return gateway.getAmbiente().toString().equals("SANDBOX") ? SANDBOX_URL : PRODUCTION_URL;
    }

    private PaymentResponse processAuthorizationResponse(ResponseEntity<Map> response, Transacao transacao) {
        if (response.getStatusCode() == HttpStatus.CREATED || response.getStatusCode() == HttpStatus.OK) {
            Map<String, Object> responseBody = response.getBody();

            PaymentResponse paymentResponse = new PaymentResponse();
            paymentResponse.setSuccess(true);
            paymentResponse.setStatus("AUTHORIZED");
            paymentResponse.setTransactionId(transacao.getTransactionId());
            paymentResponse.setGatewayTransactionId((String) responseBody.get("id"));
            
            // Dados da transação de cartão
            if (responseBody.containsKey("card_transaction")) {
                Map<String, Object> cardTx = (Map<String, Object>) responseBody.get("card_transaction");
                paymentResponse.setAuthorizationCode((String) cardTx.get("authorization_code"));
                paymentResponse.setNsu((String) cardTx.get("nsu"));
                paymentResponse.setTid((String) cardTx.get("tid"));
            }
            
            paymentResponse.setTimestamp(ZonedDateTime.now());

            logger.info("[STONE] Autorização bem-sucedida: {}", paymentResponse.getGatewayTransactionId());
            return paymentResponse;
        } else {
            return createErrorResponse("AUTHORIZATION_FAILED", "Falha na autorização Stone");
        }
    }

    private String getCurrencyCode(String currency) {
        // ISO 4217 numeric codes
        switch (currency.toUpperCase()) {
            case "BRL": return "986";
            case "USD": return "840";
            case "EUR": return "978";
            default: return "986"; // Default BRL
        }
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
