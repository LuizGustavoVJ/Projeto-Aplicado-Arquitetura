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
 * Adaptador para integração com PagSeguro
 * 
 * Implementa comunicação com API PagSeguro para:
 * - Autorização de pagamentos
 * - Captura de pagamentos
 * - Cancelamento de pagamentos
 * 
 * Documentação: https://dev.pagseguro.uol.com.br/reference/charge-intro
 * 
 * @author Luiz Gustavo Finotello
 */
@Component
public class PagSeguroAdapter implements GatewayAdapter {

    private static final Logger logger = LoggerFactory.getLogger(PagSeguroAdapter.class);
    private static final String GATEWAY_CODE = "PAGSEGURO";

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public PaymentResponse authorize(Gateway gateway, AuthorizationRequest request, Transacao transacao) {
        logger.info("Iniciando autorização PagSeguro para transação: {}", transacao.getTransactionId());

        try {
            // Construir payload PagSeguro
            Map<String, Object> payload = new HashMap<>();
            payload.put("reference_id", transacao.getTransactionId());
            payload.put("description", request.getDescription());
            payload.put("amount", Map.of("value", (int) (request.getAmount() * 100), "currency", request.getCurrency()));

            // Dados do método de pagamento
            Map<String, Object> paymentMethod = new HashMap<>();
            paymentMethod.put("type", "CREDIT_CARD");
            paymentMethod.put("installments", request.getInstallments());
            paymentMethod.put("capture", false); // Autorização sem captura automática

            Map<String, Object> card = new HashMap<>();
            card.put("encrypted", request.getCardToken());
            paymentMethod.put("card", card);

            payload.put("payment_method", paymentMethod);

            // Dados do cliente
            if (request.getCustomer() != null) {
                Map<String, Object> customer = new HashMap<>();
                customer.put("name", request.getCustomer().get("name"));
                customer.put("email", request.getCustomer().get("email"));
                customer.put("tax_id", request.getCustomer().get("document"));
                payload.put("customer", customer);
            }

            // Configurar headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + gateway.getMerchantKey());

            // Fazer requisição
            String url = gateway.getApiUrl() + "/charges";
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
                paymentResponse.setGatewayTransactionId((String) responseBody.get("id"));
                paymentResponse.setAuthorizationCode((String) responseBody.get("authorization_code"));
                paymentResponse.setTimestamp(ZonedDateTime.now());

                logger.info("Autorização PagSeguro bem-sucedida: {}", paymentResponse.getGatewayTransactionId());
                return paymentResponse;
            } else {
                return createErrorResponse("AUTHORIZATION_FAILED", "Falha na autorização PagSeguro");
            }

        } catch (Exception e) {
            logger.error("Erro na autorização PagSeguro: {}", e.getMessage(), e);
            return createErrorResponse("GATEWAY_ERROR", e.getMessage());
        }
    }

    @Override
    public PaymentResponse capture(Gateway gateway, CaptureRequest request, Transacao transacao) {
        logger.info("Iniciando captura PagSeguro para transação: {}", transacao.getTransactionId());

        try {
            // Construir payload
            Map<String, Object> payload = new HashMap<>();
            payload.put("amount", Map.of("value", (int) (request.getAmount() * 100)));

            // Configurar headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + gateway.getMerchantKey());

            // Fazer requisição de captura
            String url = String.format("%s/charges/%s/capture",
                gateway.getApiUrl(),
                transacao.getGatewayTransactionId()
            );

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

                logger.info("Captura PagSeguro bem-sucedida: {}", transacao.getGatewayTransactionId());
                return paymentResponse;
            } else {
                return createErrorResponse("CAPTURE_FAILED", "Falha na captura PagSeguro");
            }

        } catch (Exception e) {
            logger.error("Erro na captura PagSeguro: {}", e.getMessage(), e);
            return createErrorResponse("GATEWAY_ERROR", e.getMessage());
        }
    }

    @Override
    public PaymentResponse voidTransaction(Gateway gateway, VoidRequest request, Transacao transacao) {
        logger.info("Iniciando cancelamento PagSeguro para transação: {}", transacao.getTransactionId());

        try {
            // Configurar headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + gateway.getMerchantKey());

            // Fazer requisição de cancelamento
            String url = String.format("%s/charges/%s/cancel",
                gateway.getApiUrl(),
                transacao.getGatewayTransactionId()
            );

            HttpEntity<Void> entity = new HttpEntity<>(headers);
            
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

                logger.info("Cancelamento PagSeguro bem-sucedido: {}", transacao.getGatewayTransactionId());
                return paymentResponse;
            } else {
                return createErrorResponse("VOID_FAILED", "Falha no cancelamento PagSeguro");
            }

        } catch (Exception e) {
            logger.error("Erro no cancelamento PagSeguro: {}", e.getMessage(), e);
            return createErrorResponse("GATEWAY_ERROR", e.getMessage());
        }
    }

    @Override
    public boolean healthCheck(Gateway gateway) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + gateway.getMerchantKey());

            String url = gateway.getApiUrl() + "/health";
            HttpEntity<Void> entity = new HttpEntity<>(headers);
            
            ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                String.class
            );

            return response.getStatusCode().is2xxSuccessful();

        } catch (Exception e) {
            logger.warn("Health check PagSeguro falhou: {}", e.getMessage());
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
