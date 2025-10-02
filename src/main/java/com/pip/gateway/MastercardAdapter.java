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
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Adaptador para integração com Mastercard Payment Gateway Services
 * 
 * Implementa comunicação com API Mastercard para:
 * - Autorização de pagamentos com cartão Mastercard
 * - Captura de pagamentos
 * - Cancelamento e estorno de pagamentos
 * 
 * Documentação: https://developer.mastercard.com/product/mastercard-payment-gateway-services
 * 
 * @author Luiz Gustavo Finotello
 */
@Component
public class MastercardAdapter implements GatewayAdapter {

    private static final Logger logger = LoggerFactory.getLogger(MastercardAdapter.class);
    private static final String GATEWAY_CODE = "MASTERCARD";

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public PaymentResponse authorize(Gateway gateway, AuthorizationRequest request, Transacao transacao) {
        logger.info("Iniciando autorização Mastercard para transação: {}", transacao.getTransactionId());

        try {
            // Construir payload Mastercard
            Map<String, Object> payload = new HashMap<>();
            
            // Dados da transação
            Map<String, Object> transaction = new HashMap<>();
            transaction.put("reference", transacao.getTransactionId());
            transaction.put("amount", request.getAmount());
            transaction.put("currency", request.getCurrency());
            transaction.put("frequency", "SINGLE");
            payload.put("transaction", transaction);

            // Dados do cartão (usando token)
            Map<String, Object> sourceOfFunds = new HashMap<>();
            sourceOfFunds.put("type", "CARD");
            sourceOfFunds.put("token", request.getCardToken());
            payload.put("sourceOfFunds", sourceOfFunds);

            // Dados do pedido
            Map<String, Object> order = new HashMap<>();
            order.put("reference", transacao.getTransactionId());
            order.put("amount", request.getAmount());
            order.put("currency", request.getCurrency());
            order.put("description", request.getDescription());
            payload.put("order", order);

            // Dados do cliente
            if (request.getCustomer() != null) {
                Map<String, Object> customer = new HashMap<>();
                customer.put("email", request.getCustomer().get("email"));
                customer.put("firstName", getFirstName(request.getCustomer().get("name")));
                customer.put("lastName", getLastName(request.getCustomer().get("name")));
                payload.put("customer", customer);
            }

            // Configuração da transação
            Map<String, Object> apiOperation = new HashMap<>();
            apiOperation.put("operation", "AUTHORIZE");
            apiOperation.put("captureFlag", "MANUAL"); // Captura manual
            payload.put("apiOperation", apiOperation);

            // Configurar headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Basic " + gateway.getMerchantKey());
            headers.set("Accept", "application/json");

            // Fazer requisição
            String url = String.format("%s/api/rest/version/67/merchant/%s/order/%s/transaction/%s",
                gateway.getApiUrl(),
                gateway.getMerchantId(),
                transacao.getTransactionId(),
                UUID.randomUUID().toString()
            );

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);
            
            ResponseEntity<Map> response = restTemplate.exchange(
                url,
                HttpMethod.PUT,
                entity,
                Map.class
            );

            // Processar resposta
            if (response.getStatusCode() == HttpStatus.OK || response.getStatusCode() == HttpStatus.CREATED) {
                Map<String, Object> responseBody = response.getBody();
                Map<String, Object> result = (Map<String, Object>) responseBody.get("result");
                String resultIndicator = (String) result.get("indicator");

                PaymentResponse paymentResponse = new PaymentResponse();
                
                if ("APPROVED".equals(resultIndicator)) {
                    paymentResponse.setSuccess(true);
                    paymentResponse.setStatus("AUTHORIZED");
                    paymentResponse.setTransactionId(transacao.getTransactionId());
                    paymentResponse.setGatewayTransactionId((String) responseBody.get("id"));
                    paymentResponse.setAuthorizationCode((String) responseBody.get("authorizationCode"));
                    
                    Map<String, Object> response_data = (Map<String, Object>) responseBody.get("response");
                    if (response_data != null) {
                        paymentResponse.setNsu((String) response_data.get("acquirerCode"));
                    }
                    
                    paymentResponse.setTimestamp(ZonedDateTime.now());

                    logger.info("Autorização Mastercard bem-sucedida: {}", paymentResponse.getGatewayTransactionId());
                } else {
                    paymentResponse.setSuccess(false);
                    paymentResponse.setErrorCode(resultIndicator);
                    paymentResponse.setErrorMessage((String) result.get("description"));
                    paymentResponse.setTimestamp(ZonedDateTime.now());
                }

                return paymentResponse;
            } else {
                return createErrorResponse("AUTHORIZATION_FAILED", "Falha na autorização Mastercard");
            }

        } catch (Exception e) {
            logger.error("Erro na autorização Mastercard: {}", e.getMessage(), e);
            return createErrorResponse("GATEWAY_ERROR", e.getMessage());
        }
    }

    @Override
    public PaymentResponse capture(Gateway gateway, CaptureRequest request, Transacao transacao) {
        logger.info("Iniciando captura Mastercard para transação: {}", transacao.getTransactionId());

        try {
            // Construir payload de captura
            Map<String, Object> payload = new HashMap<>();
            
            Map<String, Object> transaction = new HashMap<>();
            transaction.put("amount", request.getAmount());
            transaction.put("currency", "BRL");
            payload.put("transaction", transaction);

            Map<String, Object> apiOperation = new HashMap<>();
            apiOperation.put("operation", "CAPTURE");
            payload.put("apiOperation", apiOperation);

            // Configurar headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Basic " + gateway.getMerchantKey());

            // Fazer requisição de captura
            String url = String.format("%s/api/rest/version/67/merchant/%s/order/%s/transaction/%s",
                gateway.getApiUrl(),
                gateway.getMerchantId(),
                transacao.getTransactionId(),
                UUID.randomUUID().toString()
            );

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);
            
            ResponseEntity<Map> response = restTemplate.exchange(
                url,
                HttpMethod.PUT,
                entity,
                Map.class
            );

            // Processar resposta
            if (response.getStatusCode() == HttpStatus.OK) {
                Map<String, Object> responseBody = response.getBody();
                Map<String, Object> result = (Map<String, Object>) responseBody.get("result");
                String resultIndicator = (String) result.get("indicator");

                PaymentResponse paymentResponse = new PaymentResponse();
                
                if ("APPROVED".equals(resultIndicator)) {
                    paymentResponse.setSuccess(true);
                    paymentResponse.setStatus("CAPTURED");
                    paymentResponse.setTransactionId(transacao.getTransactionId());
                    paymentResponse.setGatewayTransactionId(transacao.getGatewayTransactionId());
                    paymentResponse.setTimestamp(ZonedDateTime.now());

                    logger.info("Captura Mastercard bem-sucedida: {}", transacao.getGatewayTransactionId());
                } else {
                    paymentResponse.setSuccess(false);
                    paymentResponse.setErrorCode(resultIndicator);
                    paymentResponse.setErrorMessage((String) result.get("description"));
                }

                return paymentResponse;
            } else {
                return createErrorResponse("CAPTURE_FAILED", "Falha na captura Mastercard");
            }

        } catch (Exception e) {
            logger.error("Erro na captura Mastercard: {}", e.getMessage(), e);
            return createErrorResponse("GATEWAY_ERROR", e.getMessage());
        }
    }

    @Override
    public PaymentResponse voidTransaction(Gateway gateway, VoidRequest request, Transacao transacao) {
        logger.info("Iniciando cancelamento Mastercard para transação: {}", transacao.getTransactionId());

        try {
            // Construir payload de cancelamento
            Map<String, Object> payload = new HashMap<>();
            
            Map<String, Object> transaction = new HashMap<>();
            transaction.put("targetTransactionId", transacao.getGatewayTransactionId());
            payload.put("transaction", transaction);

            Map<String, Object> apiOperation = new HashMap<>();
            apiOperation.put("operation", "VOID");
            payload.put("apiOperation", apiOperation);

            // Configurar headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Basic " + gateway.getMerchantKey());

            // Fazer requisição de cancelamento
            String url = String.format("%s/api/rest/version/67/merchant/%s/order/%s/transaction/%s",
                gateway.getApiUrl(),
                gateway.getMerchantId(),
                transacao.getTransactionId(),
                UUID.randomUUID().toString()
            );

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);
            
            ResponseEntity<Map> response = restTemplate.exchange(
                url,
                HttpMethod.PUT,
                entity,
                Map.class
            );

            // Processar resposta
            if (response.getStatusCode() == HttpStatus.OK) {
                Map<String, Object> responseBody = response.getBody();
                Map<String, Object> result = (Map<String, Object>) responseBody.get("result");
                String resultIndicator = (String) result.get("indicator");

                PaymentResponse paymentResponse = new PaymentResponse();
                
                if ("APPROVED".equals(resultIndicator)) {
                    paymentResponse.setSuccess(true);
                    paymentResponse.setStatus("VOIDED");
                    paymentResponse.setTransactionId(transacao.getTransactionId());
                    paymentResponse.setGatewayTransactionId(transacao.getGatewayTransactionId());
                    paymentResponse.setTimestamp(ZonedDateTime.now());

                    logger.info("Cancelamento Mastercard bem-sucedido: {}", transacao.getGatewayTransactionId());
                } else {
                    paymentResponse.setSuccess(false);
                    paymentResponse.setErrorCode(resultIndicator);
                    paymentResponse.setErrorMessage((String) result.get("description"));
                }

                return paymentResponse;
            } else {
                return createErrorResponse("VOID_FAILED", "Falha no cancelamento Mastercard");
            }

        } catch (Exception e) {
            logger.error("Erro no cancelamento Mastercard: {}", e.getMessage(), e);
            return createErrorResponse("GATEWAY_ERROR", e.getMessage());
        }
    }

    @Override
    public boolean healthCheck(Gateway gateway) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Basic " + gateway.getMerchantKey());

            String url = String.format("%s/api/rest/version/67/merchant/%s/information",
                gateway.getApiUrl(),
                gateway.getMerchantId()
            );

            HttpEntity<Void> entity = new HttpEntity<>(headers);
            
            ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                String.class
            );

            return response.getStatusCode().is2xxSuccessful();

        } catch (Exception e) {
            logger.warn("Health check Mastercard falhou: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public String getGatewayCode() {
        return GATEWAY_CODE;
    }

    /**
     * Extrai primeiro nome
     */
    private String getFirstName(Object nameObj) {
        if (nameObj == null) return "";
        String name = nameObj.toString();
        String[] parts = name.split(" ");
        return parts.length > 0 ? parts[0] : "";
    }

    /**
     * Extrai sobrenome
     */
    private String getLastName(Object nameObj) {
        if (nameObj == null) return "";
        String name = nameObj.toString();
        String[] parts = name.split(" ");
        return parts.length > 1 ? parts[parts.length - 1] : "";
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
