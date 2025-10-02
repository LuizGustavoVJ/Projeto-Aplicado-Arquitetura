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

/**
 * Adaptador para integração com Mercado Pago
 * 
 * Implementa comunicação com API Mercado Pago para:
 * - Autorização de pagamentos
 * - Captura de pagamentos
 * - Cancelamento de pagamentos
 * 
 * Documentação: https://www.mercadopago.com.br/developers/pt/reference
 * 
 * @author Luiz Gustavo Finotello
 */
@Component
public class MercadoPagoAdapter implements GatewayAdapter {

    private static final Logger logger = LoggerFactory.getLogger(MercadoPagoAdapter.class);
    private static final String GATEWAY_CODE = "MERCADOPAGO";

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public PaymentResponse authorize(Gateway gateway, AuthorizationRequest request, Transacao transacao) {
        logger.info("Iniciando autorização Mercado Pago para transação: {}", transacao.getTransactionId());

        try {
            // Construir payload Mercado Pago
            Map<String, Object> payload = new HashMap<>();
            payload.put("transaction_amount", request.getAmount());
            payload.put("token", request.getCardToken());
            payload.put("description", request.getDescription());
            payload.put("installments", request.getInstallments());
            payload.put("payment_method_id", "credit_card");
            payload.put("capture", false); // Autorização sem captura automática
            payload.put("external_reference", transacao.getTransactionId());

            // Dados do pagador
            if (request.getCustomer() != null) {
                Map<String, Object> payer = new HashMap<>();
                payer.put("email", request.getCustomer().get("email"));
                
                Map<String, Object> identification = new HashMap<>();
                identification.put("type", "CPF");
                identification.put("number", request.getCustomer().get("document"));
                payer.put("identification", identification);
                
                payload.put("payer", payer);
            }

            // Configurar headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + gateway.getMerchantKey());
            headers.set("X-Idempotency-Key", transacao.getTransactionId());

            // Fazer requisição
            String url = gateway.getApiUrl() + "/v1/payments";
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);
            
            ResponseEntity<Map> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                entity,
                Map.class
            );

            // Processar resposta
            if (response.getStatusCode() == HttpStatus.CREATED || response.getStatusCode() == HttpStatus.OK) {
                Map<String, Object> responseBody = response.getBody();

                PaymentResponse paymentResponse = new PaymentResponse();
                paymentResponse.setSuccess(true);
                paymentResponse.setStatus("AUTHORIZED");
                paymentResponse.setTransactionId(transacao.getTransactionId());
                paymentResponse.setGatewayTransactionId(String.valueOf(responseBody.get("id")));
                paymentResponse.setAuthorizationCode((String) responseBody.get("authorization_code"));
                paymentResponse.setTimestamp(ZonedDateTime.now());

                logger.info("Autorização Mercado Pago bem-sucedida: {}", paymentResponse.getGatewayTransactionId());
                return paymentResponse;
            } else {
                return createErrorResponse("AUTHORIZATION_FAILED", "Falha na autorização Mercado Pago");
            }

        } catch (Exception e) {
            logger.error("Erro na autorização Mercado Pago: {}", e.getMessage(), e);
            return createErrorResponse("GATEWAY_ERROR", e.getMessage());
        }
    }

    @Override
    public PaymentResponse capture(Gateway gateway, CaptureRequest request, Transacao transacao) {
        logger.info("Iniciando captura Mercado Pago para transação: {}", transacao.getTransactionId());

        try {
            // Construir payload
            Map<String, Object> payload = new HashMap<>();
            payload.put("capture", true);
            payload.put("transaction_amount", request.getAmount());

            // Configurar headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + gateway.getMerchantKey());

            // Fazer requisição de captura
            String url = String.format("%s/v1/payments/%s",
                gateway.getApiUrl(),
                transacao.getGatewayTransactionId()
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
                PaymentResponse paymentResponse = new PaymentResponse();
                paymentResponse.setSuccess(true);
                paymentResponse.setStatus("CAPTURED");
                paymentResponse.setTransactionId(transacao.getTransactionId());
                paymentResponse.setGatewayTransactionId(transacao.getGatewayTransactionId());
                paymentResponse.setTimestamp(ZonedDateTime.now());

                logger.info("Captura Mercado Pago bem-sucedida: {}", transacao.getGatewayTransactionId());
                return paymentResponse;
            } else {
                return createErrorResponse("CAPTURE_FAILED", "Falha na captura Mercado Pago");
            }

        } catch (Exception e) {
            logger.error("Erro na captura Mercado Pago: {}", e.getMessage(), e);
            return createErrorResponse("GATEWAY_ERROR", e.getMessage());
        }
    }

    @Override
    public PaymentResponse voidTransaction(Gateway gateway, VoidRequest request, Transacao transacao) {
        logger.info("Iniciando cancelamento Mercado Pago para transação: {}", transacao.getTransactionId());

        try {
            // Configurar headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + gateway.getMerchantKey());

            // Fazer requisição de cancelamento
            String url = String.format("%s/v1/payments/%s",
                gateway.getApiUrl(),
                transacao.getGatewayTransactionId()
            );

            // Payload para cancelamento
            Map<String, Object> payload = new HashMap<>();
            payload.put("status", "cancelled");

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);
            
            ResponseEntity<Map> response = restTemplate.exchange(
                url,
                HttpMethod.PUT,
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

                logger.info("Cancelamento Mercado Pago bem-sucedido: {}", transacao.getGatewayTransactionId());
                return paymentResponse;
            } else {
                return createErrorResponse("VOID_FAILED", "Falha no cancelamento Mercado Pago");
            }

        } catch (Exception e) {
            logger.error("Erro no cancelamento Mercado Pago: {}", e.getMessage(), e);
            return createErrorResponse("GATEWAY_ERROR", e.getMessage());
        }
    }

    @Override
    public boolean healthCheck(Gateway gateway) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + gateway.getMerchantKey());

            String url = gateway.getApiUrl() + "/v1/payment_methods";
            HttpEntity<Void> entity = new HttpEntity<>(headers);
            
            ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                String.class
            );

            return response.getStatusCode().is2xxSuccessful();

        } catch (Exception e) {
            logger.warn("Health check Mercado Pago falhou: {}", e.getMessage());
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
