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
 * Adaptador para integração com Rede
 * 
 * Implementa comunicação com API Rede E-Commerce para:
 * - Autorização de pagamentos
 * - Captura de pagamentos
 * - Cancelamento de pagamentos
 * 
 * Documentação: https://www.userede.com.br/desenvolvedores
 * 
 * @author Luiz Gustavo Finotello
 */
@Component
public class RedeAdapter implements GatewayAdapter {

    private static final Logger logger = LoggerFactory.getLogger(RedeAdapter.class);
    private static final String GATEWAY_CODE = "REDE";

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public PaymentResponse authorize(Gateway gateway, AuthorizationRequest request, Transacao transacao) {
        logger.info("Iniciando autorização Rede para transação: {}", transacao.getTransactionId());

        try {
            // Construir payload Rede
            Map<String, Object> payload = new HashMap<>();
            payload.put("reference", transacao.getTransactionId());
            payload.put("amount", (int) (request.getAmount() * 100)); // Rede usa centavos
            payload.put("installments", request.getInstallments());
            payload.put("capture", false); // Autorização sem captura automática

            // Dados do cartão
            Map<String, Object> cardData = new HashMap<>();
            cardData.put("cardToken", request.getCardToken());
            payload.put("cardData", cardData);

            // Configurar headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + gateway.getMerchantKey());

            // Fazer requisição
            String url = gateway.getApiUrl() + "/v1/transactions";
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
                paymentResponse.setGatewayTransactionId((String) responseBody.get("tid"));
                paymentResponse.setAuthorizationCode((String) responseBody.get("authorizationCode"));
                paymentResponse.setNsu((String) responseBody.get("nsu"));
                paymentResponse.setTid((String) responseBody.get("tid"));
                paymentResponse.setTimestamp(ZonedDateTime.now());

                logger.info("Autorização Rede bem-sucedida: {}", paymentResponse.getGatewayTransactionId());
                return paymentResponse;
            } else {
                return createErrorResponse("AUTHORIZATION_FAILED", "Falha na autorização Rede");
            }

        } catch (Exception e) {
            logger.error("Erro na autorização Rede: {}", e.getMessage(), e);
            return createErrorResponse("GATEWAY_ERROR", e.getMessage());
        }
    }

    @Override
    public PaymentResponse capture(Gateway gateway, CaptureRequest request, Transacao transacao) {
        logger.info("Iniciando captura Rede para transação: {}", transacao.getTransactionId());

        try {
            // Construir payload
            Map<String, Object> payload = new HashMap<>();
            payload.put("amount", (int) (request.getAmount() * 100));

            // Configurar headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + gateway.getMerchantKey());

            // Fazer requisição de captura
            String url = String.format("%s/v1/transactions/%s/capture",
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

                logger.info("Captura Rede bem-sucedida: {}", transacao.getGatewayTransactionId());
                return paymentResponse;
            } else {
                return createErrorResponse("CAPTURE_FAILED", "Falha na captura Rede");
            }

        } catch (Exception e) {
            logger.error("Erro na captura Rede: {}", e.getMessage(), e);
            return createErrorResponse("GATEWAY_ERROR", e.getMessage());
        }
    }

    @Override
    public PaymentResponse voidTransaction(Gateway gateway, VoidRequest request, Transacao transacao) {
        logger.info("Iniciando cancelamento Rede para transação: {}", transacao.getTransactionId());

        try {
            // Configurar headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + gateway.getMerchantKey());

            // Fazer requisição de cancelamento
            String url = String.format("%s/v1/transactions/%s/refund",
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

                logger.info("Cancelamento Rede bem-sucedido: {}", transacao.getGatewayTransactionId());
                return paymentResponse;
            } else {
                return createErrorResponse("VOID_FAILED", "Falha no cancelamento Rede");
            }

        } catch (Exception e) {
            logger.error("Erro no cancelamento Rede: {}", e.getMessage(), e);
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
            logger.warn("Health check Rede falhou: {}", e.getMessage());
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
