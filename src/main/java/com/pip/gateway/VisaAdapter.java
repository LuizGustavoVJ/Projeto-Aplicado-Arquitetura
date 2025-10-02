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
 * Adaptador para integração com Visa Direct
 * 
 * Implementa comunicação com API Visa Direct para:
 * - Autorização de pagamentos com cartão Visa
 * - Captura de pagamentos
 * - Cancelamento e estorno de pagamentos
 * 
 * Documentação: https://developer.visa.com/capabilities/visa_direct
 * 
 * @author Luiz Gustavo Finotello
 */
@Component
public class VisaAdapter implements GatewayAdapter {

    private static final Logger logger = LoggerFactory.getLogger(VisaAdapter.class);
    private static final String GATEWAY_CODE = "VISA";

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public PaymentResponse authorize(Gateway gateway, AuthorizationRequest request, Transacao transacao) {
        logger.info("Iniciando autorização Visa Direct para transação: {}", transacao.getTransactionId());

        try {
            // Construir payload Visa Direct
            Map<String, Object> payload = new HashMap<>();
            payload.put("systemsTraceAuditNumber", generateTraceNumber());
            payload.put("retrievalReferenceNumber", transacao.getTransactionId().substring(0, 12));
            payload.put("localTransactionDateTime", ZonedDateTime.now().toString());
            
            // Dados do valor
            payload.put("amount", String.format("%.2f", request.getAmount()));
            payload.put("currencyCode", getCurrencyCode(request.getCurrency()));
            
            // Dados do cartão
            Map<String, Object> cardAcceptor = new HashMap<>();
            cardAcceptor.put("idCode", gateway.getMerchantId());
            cardAcceptor.put("name", "PIP Platform");
            cardAcceptor.put("terminalId", "PIP001");
            payload.put("cardAcceptor", cardAcceptor);

            // Dados da transação
            Map<String, Object> transactionData = new HashMap<>();
            transactionData.put("transactionIdentifier", transacao.getTransactionId());
            transactionData.put("transactionType", "AUTHORIZATION");
            transactionData.put("captureFlag", "false"); // Autorização sem captura automática
            payload.put("transactionData", transactionData);

            // Token do cartão
            payload.put("cardToken", request.getCardToken());

            // Configurar headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Basic " + gateway.getMerchantKey());
            headers.set("Accept", "application/json");

            // Fazer requisição
            String url = gateway.getApiUrl() + "/visadirect/fundstransfer/v1/pullfundstransactions";
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);
            
            ResponseEntity<Map> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                entity,
                Map.class
            );

            // Processar resposta
            if (response.getStatusCode() == HttpStatus.OK || response.getStatusCode() == HttpStatus.CREATED) {
                Map<String, Object> responseBody = response.getBody();

                PaymentResponse paymentResponse = new PaymentResponse();
                paymentResponse.setSuccess(true);
                paymentResponse.setStatus("AUTHORIZED");
                paymentResponse.setTransactionId(transacao.getTransactionId());
                paymentResponse.setGatewayTransactionId((String) responseBody.get("transactionIdentifier"));
                paymentResponse.setAuthorizationCode((String) responseBody.get("approvalCode"));
                paymentResponse.setNsu((String) responseBody.get("retrievalReferenceNumber"));
                paymentResponse.setTimestamp(ZonedDateTime.now());

                logger.info("Autorização Visa Direct bem-sucedida: {}", paymentResponse.getGatewayTransactionId());
                return paymentResponse;
            } else {
                return createErrorResponse("AUTHORIZATION_FAILED", "Falha na autorização Visa Direct");
            }

        } catch (Exception e) {
            logger.error("Erro na autorização Visa Direct: {}", e.getMessage(), e);
            return createErrorResponse("GATEWAY_ERROR", e.getMessage());
        }
    }

    @Override
    public PaymentResponse capture(Gateway gateway, CaptureRequest request, Transacao transacao) {
        logger.info("Iniciando captura Visa Direct para transação: {}", transacao.getTransactionId());

        try {
            // Construir payload de captura
            Map<String, Object> payload = new HashMap<>();
            payload.put("systemsTraceAuditNumber", generateTraceNumber());
            payload.put("retrievalReferenceNumber", transacao.getTransactionId().substring(0, 12));
            payload.put("amount", String.format("%.2f", request.getAmount()));
            payload.put("transactionIdentifier", transacao.getGatewayTransactionId());
            payload.put("captureFlag", "true");

            // Configurar headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Basic " + gateway.getMerchantKey());

            // Fazer requisição de captura
            String url = gateway.getApiUrl() + "/visadirect/fundstransfer/v1/capture";
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);
            
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
                paymentResponse.setStatus("CAPTURED");
                paymentResponse.setTransactionId(transacao.getTransactionId());
                paymentResponse.setGatewayTransactionId(transacao.getGatewayTransactionId());
                paymentResponse.setTimestamp(ZonedDateTime.now());

                logger.info("Captura Visa Direct bem-sucedida: {}", transacao.getGatewayTransactionId());
                return paymentResponse;
            } else {
                return createErrorResponse("CAPTURE_FAILED", "Falha na captura Visa Direct");
            }

        } catch (Exception e) {
            logger.error("Erro na captura Visa Direct: {}", e.getMessage(), e);
            return createErrorResponse("GATEWAY_ERROR", e.getMessage());
        }
    }

    @Override
    public PaymentResponse voidTransaction(Gateway gateway, VoidRequest request, Transacao transacao) {
        logger.info("Iniciando cancelamento Visa Direct para transação: {}", transacao.getTransactionId());

        try {
            // Construir payload de reversão
            Map<String, Object> payload = new HashMap<>();
            payload.put("systemsTraceAuditNumber", generateTraceNumber());
            payload.put("retrievalReferenceNumber", transacao.getTransactionId().substring(0, 12));
            payload.put("originalDataElements", Map.of(
                "transactionIdentifier", transacao.getGatewayTransactionId(),
                "approvalCode", transacao.getAuthorizationCode()
            ));

            // Configurar headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Basic " + gateway.getMerchantKey());

            // Fazer requisição de reversão
            String url = gateway.getApiUrl() + "/visadirect/fundstransfer/v1/reversefundstransactions";
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);
            
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

                logger.info("Cancelamento Visa Direct bem-sucedido: {}", transacao.getGatewayTransactionId());
                return paymentResponse;
            } else {
                return createErrorResponse("VOID_FAILED", "Falha no cancelamento Visa Direct");
            }

        } catch (Exception e) {
            logger.error("Erro no cancelamento Visa Direct: {}", e.getMessage(), e);
            return createErrorResponse("GATEWAY_ERROR", e.getMessage());
        }
    }

    @Override
    public boolean healthCheck(Gateway gateway) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Basic " + gateway.getMerchantKey());

            String url = gateway.getApiUrl() + "/visadirect/fundstransfer/v1/health";
            HttpEntity<Void> entity = new HttpEntity<>(headers);
            
            ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                String.class
            );

            return response.getStatusCode().is2xxSuccessful();

        } catch (Exception e) {
            logger.warn("Health check Visa Direct falhou: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public String getGatewayCode() {
        return GATEWAY_CODE;
    }

    /**
     * Gera número de rastreamento único
     */
    private String generateTraceNumber() {
        return String.valueOf(System.currentTimeMillis() % 1000000);
    }

    /**
     * Converte código de moeda para formato numérico ISO 4217
     */
    private String getCurrencyCode(String currency) {
        switch (currency.toUpperCase()) {
            case "BRL": return "986";
            case "USD": return "840";
            case "EUR": return "978";
            default: return "986"; // BRL por padrão
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
