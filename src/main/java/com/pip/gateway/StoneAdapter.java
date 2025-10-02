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
 * Adaptador para integração com Stone
 * 
 * Implementa comunicação com API Stone Pagamentos para:
 * - Autorização de pagamentos
 * - Captura de pagamentos
 * - Cancelamento de pagamentos
 * 
 * Documentação: https://docs.stone.com.br/
 * 
 * @author Luiz Gustavo Finotello
 */
@Component
public class StoneAdapter implements GatewayAdapter {

    private static final Logger logger = LoggerFactory.getLogger(StoneAdapter.class);
    private static final String GATEWAY_CODE = "STONE";

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public PaymentResponse authorize(Gateway gateway, AuthorizationRequest request, Transacao transacao) {
        logger.info("Iniciando autorização Stone para transação: {}", transacao.getTransactionId());

        try {
            // Construir payload Stone
            Map<String, Object> payload = new HashMap<>();
            payload.put("order_id", transacao.getTransactionId());
            payload.put("amount_in_cents", (int) (request.getAmount() * 100)); // Stone usa centavos
            payload.put("currency", request.getCurrency());
            payload.put("installments", request.getInstallments());
            payload.put("capture", false); // Autorização sem captura automática

            // Dados do cartão
            Map<String, Object> card = new HashMap<>();
            card.put("card_token", request.getCardToken());
            payload.put("card", card);

            // Dados do cliente
            if (request.getCustomer() != null) {
                Map<String, Object> customer = new HashMap<>();
                customer.put("name", request.getCustomer().get("name"));
                customer.put("email", request.getCustomer().get("email"));
                customer.put("document", request.getCustomer().get("document"));
                payload.put("customer", customer);
            }

            // Configurar headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + gateway.getMerchantKey());
            headers.set("X-Stone-Idempotency-Key", transacao.getTransactionId());

            // Fazer requisição
            String url = gateway.getApiUrl() + "/v1/charges";
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
                paymentResponse.setNsu((String) responseBody.get("nsu"));
                paymentResponse.setTid((String) responseBody.get("tid"));
                paymentResponse.setTimestamp(ZonedDateTime.now());

                logger.info("Autorização Stone bem-sucedida: {}", paymentResponse.getGatewayTransactionId());
                return paymentResponse;
            } else {
                return createErrorResponse("AUTHORIZATION_FAILED", "Falha na autorização Stone");
            }

        } catch (Exception e) {
            logger.error("Erro na autorização Stone: {}", e.getMessage(), e);
            return createErrorResponse("GATEWAY_ERROR", e.getMessage());
        }
    }

    @Override
    public PaymentResponse capture(Gateway gateway, CaptureRequest request, Transacao transacao) {
        logger.info("Iniciando captura Stone para transação: {}", transacao.getTransactionId());

        try {
            // Construir payload
            Map<String, Object> payload = new HashMap<>();
            payload.put("amount_in_cents", (int) (request.getAmount() * 100));

            // Configurar headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + gateway.getMerchantKey());

            // Fazer requisição de captura
            String url = String.format("%s/v1/charges/%s/capture",
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

                logger.info("Captura Stone bem-sucedida: {}", transacao.getGatewayTransactionId());
                return paymentResponse;
            } else {
                return createErrorResponse("CAPTURE_FAILED", "Falha na captura Stone");
            }

        } catch (Exception e) {
            logger.error("Erro na captura Stone: {}", e.getMessage(), e);
            return createErrorResponse("GATEWAY_ERROR", e.getMessage());
        }
    }

    @Override
    public PaymentResponse voidTransaction(Gateway gateway, VoidRequest request, Transacao transacao) {
        logger.info("Iniciando cancelamento Stone para transação: {}", transacao.getTransactionId());

        try {
            // Configurar headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + gateway.getMerchantKey());

            // Fazer requisição de cancelamento
            String url = String.format("%s/v1/charges/%s/cancel",
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

                logger.info("Cancelamento Stone bem-sucedido: {}", transacao.getGatewayTransactionId());
                return paymentResponse;
            } else {
                return createErrorResponse("VOID_FAILED", "Falha no cancelamento Stone");
            }

        } catch (Exception e) {
            logger.error("Erro no cancelamento Stone: {}", e.getMessage(), e);
            return createErrorResponse("GATEWAY_ERROR", e.getMessage());
        }
    }

    @Override
    public boolean healthCheck(Gateway gateway) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + gateway.getMerchantKey());

            String url = gateway.getApiUrl() + "/v1/health";
            HttpEntity<Void> entity = new HttpEntity<>(headers);
            
            ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                String.class
            );

            return response.getStatusCode().is2xxSuccessful();

        } catch (Exception e) {
            logger.warn("Health check Stone falhou: {}", e.getMessage());
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
