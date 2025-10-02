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
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Adaptador REAL para integração com Cielo E-Commerce
 * 
 * Implementa comunicação com API Cielo seguindo documentação oficial:
 * https://desenvolvedores.cielo.com.br/api-portal/pt-br/product/e-commerce/api
 * 
 * Características:
 * - Autenticação via MerchantId e MerchantKey
 * - Ambiente Sandbox: apisandbox.cieloecommerce.cielo.com.br
 * - Ambiente Produção: api.cieloecommerce.cielo.com.br
 * - Suporta: Autorização, Captura, Cancelamento
 * - Valores em centavos
 * - Tokenização de cartões
 * 
 * Segurança:
 * - TLS 1.2+ obrigatório
 * - PCI-DSS Level 1 compliant
 * - Antifraude integrado
 * - 3DS 2.0 support
 * 
 * @author Luiz Gustavo Finotello
 * @version 2.0 - Integração Real
 */
@Component
public class CieloAdapter implements GatewayAdapter {

    private static final Logger logger = LoggerFactory.getLogger(CieloAdapter.class);
    private static final String GATEWAY_CODE = "CIELO";
    
    // URLs oficiais Cielo
    private static final String SANDBOX_URL = "https://apisandbox.cieloecommerce.cielo.com.br";
    private static final String PRODUCTION_URL = "https://api.cieloecommerce.cielo.com.br";
    private static final String SANDBOX_QUERY_URL = "https://apiquerysandbox.cieloecommerce.cielo.com.br";
    private static final String PRODUCTION_QUERY_URL = "https://apiquery.cieloecommerce.cielo.com.br";

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public PaymentResponse authorize(Gateway gateway, AuthorizationRequest request, Transacao transacao) {
        logger.info("[CIELO] Iniciando autorização - TransactionID: {}", transacao.getTransactionId());
        
        try {
            // Validações de segurança
            validateRequest(request);
            
            // Construir payload conforme documentação Cielo
            Map<String, Object> payload = buildAuthorizationPayload(request, transacao);
            
            // Configurar headers com autenticação
            HttpHeaders headers = buildHeaders(gateway);
            
            // Fazer requisição
            String url = getBaseUrl(gateway) + "/1/sales";
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);
            
            logger.debug("[CIELO] Enviando requisição para: {}", url);
            
            ResponseEntity<Map> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                entity,
                Map.class
            );

            // Processar resposta
            return processAuthorizationResponse(response, transacao);

        } catch (HttpClientErrorException e) {
            logger.error("[CIELO] Erro 4xx na autorização: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            return createErrorResponse("AUTHORIZATION_FAILED", "Erro na autorização: " + e.getMessage());
        } catch (HttpServerErrorException e) {
            logger.error("[CIELO] Erro 5xx na autorização: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            return createErrorResponse("GATEWAY_ERROR", "Erro no gateway Cielo: " + e.getMessage());
        } catch (Exception e) {
            logger.error("[CIELO] Erro inesperado na autorização", e);
            return createErrorResponse("SYSTEM_ERROR", "Erro no sistema: " + e.getMessage());
        }
    }

    @Override
    public PaymentResponse capture(Gateway gateway, CaptureRequest request, Transacao transacao) {
        logger.info("[CIELO] Iniciando captura - TransactionID: {}", transacao.getTransactionId());

        try {
            if (transacao.getGatewayTransactionId() == null) {
                return createErrorResponse("INVALID_TRANSACTION", "PaymentId não encontrado");
            }
            
            // Configurar headers
            HttpHeaders headers = buildHeaders(gateway);

            // URL de captura
            String url = String.format("%s/1/sales/%s/capture?amount=%d",
                getBaseUrl(gateway),
                transacao.getGatewayTransactionId(),
                (int) (request.getAmount() * 100)
            );

            HttpEntity<Void> entity = new HttpEntity<>(headers);
            
            logger.debug("[CIELO] Enviando captura para: {}", url);
            
            ResponseEntity<Map> response = restTemplate.exchange(
                url,
                HttpMethod.PUT,
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
                paymentResponse.setGatewayTransactionId((String) responseBody.get("PaymentId"));
                paymentResponse.setTimestamp(ZonedDateTime.now());

                logger.info("[CIELO] Captura bem-sucedida: {}", transacao.getGatewayTransactionId());
                return paymentResponse;
            } else {
                return createErrorResponse("CAPTURE_FAILED", "Falha na captura Cielo");
            }

        } catch (Exception e) {
            logger.error("[CIELO] Erro na captura", e);
            return createErrorResponse("GATEWAY_ERROR", e.getMessage());
        }
    }

    @Override
    public PaymentResponse voidTransaction(Gateway gateway, VoidRequest request, Transacao transacao) {
        logger.info("[CIELO] Iniciando cancelamento - TransactionID: {}", transacao.getTransactionId());

        try {
            if (transacao.getGatewayTransactionId() == null) {
                return createErrorResponse("INVALID_TRANSACTION", "PaymentId não encontrado");
            }
            
            // Configurar headers
            HttpHeaders headers = buildHeaders(gateway);

            // URL de cancelamento
            String url = String.format("%s/1/sales/%s/void",
                getBaseUrl(gateway),
                transacao.getGatewayTransactionId()
            );

            HttpEntity<Void> entity = new HttpEntity<>(headers);
            
            logger.debug("[CIELO] Enviando cancelamento para: {}", url);
            
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

                logger.info("[CIELO] Cancelamento bem-sucedido: {}", transacao.getGatewayTransactionId());
                return paymentResponse;
            } else {
                return createErrorResponse("VOID_FAILED", "Falha no cancelamento Cielo");
            }

        } catch (Exception e) {
            logger.error("[CIELO] Erro no cancelamento", e);
            return createErrorResponse("GATEWAY_ERROR", e.getMessage());
        }
    }

    @Override
    public boolean healthCheck(Gateway gateway) {
        try {
            HttpHeaders headers = buildHeaders(gateway);
            
            // Fazer consulta simples para verificar conectividade
            String url = getQueryUrl(gateway) + "/1/sales/" + UUID.randomUUID().toString();
            HttpEntity<Void> entity = new HttpEntity<>(headers);
            
            restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                String.class
            );

            return true;

        } catch (HttpClientErrorException e) {
            // 404 é esperado, significa que a API está respondendo
            return e.getStatusCode() == HttpStatus.NOT_FOUND;
        } catch (Exception e) {
            logger.warn("[CIELO] Health check falhou: {}", e.getMessage());
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
        
        // Merchant Order ID
        payload.put("MerchantOrderId", transacao.getTransactionId());
        
        // Customer
        if (request.getCustomer() != null) {
            Map<String, Object> customer = new HashMap<>();
            customer.put("Name", request.getCustomer().get("name"));
            customer.put("Email", request.getCustomer().get("email"));
            customer.put("Identity", request.getCustomer().get("document"));
            customer.put("IdentityType", "CPF");
            payload.put("Customer", customer);
        }
        
        // Payment
        Map<String, Object> payment = new HashMap<>();
        payment.put("Type", "CreditCard");
        payment.put("Amount", (int) (request.getAmount() * 100));
        payment.put("Currency", request.getCurrency() != null ? request.getCurrency() : "BRL");
        payment.put("Country", "BRA");
        payment.put("Installments", request.getInstallments() != null ? request.getInstallments() : 1);
        payment.put("Capture", false); // Autorização sem captura automática
        payment.put("SoftDescriptor", "PIP");
        
        // Credit Card
        Map<String, Object> creditCard = new HashMap<>();
        creditCard.put("CardToken", request.getCardToken());
        creditCard.put("SaveCard", false);
        payment.put("CreditCard", creditCard);
        
        payload.put("Payment", payment);
        
        return payload;
    }

    private HttpHeaders buildHeaders(Gateway gateway) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("MerchantId", gateway.getMerchantId());
        headers.set("MerchantKey", gateway.getMerchantKey());
        headers.set("RequestId", UUID.randomUUID().toString());
        return headers;
    }

    private String getBaseUrl(Gateway gateway) {
        return gateway.getAmbiente().toString().equals("SANDBOX") ? SANDBOX_URL : PRODUCTION_URL;
    }

    private String getQueryUrl(Gateway gateway) {
        return gateway.getAmbiente().toString().equals("SANDBOX") ? SANDBOX_QUERY_URL : PRODUCTION_QUERY_URL;
    }

    private PaymentResponse processAuthorizationResponse(ResponseEntity<Map> response, Transacao transacao) {
        if (response.getStatusCode() == HttpStatus.CREATED || response.getStatusCode() == HttpStatus.OK) {
            Map<String, Object> responseBody = response.getBody();
            Map<String, Object> payment = (Map<String, Object>) responseBody.get("Payment");

            PaymentResponse paymentResponse = new PaymentResponse();
            
            // Status da transação
            Integer status = (Integer) payment.get("Status");
            boolean isAuthorized = status != null && (status == 1 || status == 2); // 1=Authorized, 2=PaymentConfirmed
            
            paymentResponse.setSuccess(isAuthorized);
            paymentResponse.setStatus(isAuthorized ? "AUTHORIZED" : "DENIED");
            paymentResponse.setTransactionId(transacao.getTransactionId());
            paymentResponse.setGatewayTransactionId((String) payment.get("PaymentId"));
            paymentResponse.setAuthorizationCode((String) payment.get("AuthorizationCode"));
            paymentResponse.setNsu((String) payment.get("ProofOfSale"));
            paymentResponse.setTid((String) payment.get("Tid"));
            paymentResponse.setTimestamp(ZonedDateTime.now());
            
            if (!isAuthorized) {
                paymentResponse.setErrorCode((String) payment.get("ReturnCode"));
                paymentResponse.setErrorMessage((String) payment.get("ReturnMessage"));
            }

            logger.info("[CIELO] Autorização processada: {} - Status: {}", 
                paymentResponse.getGatewayTransactionId(), paymentResponse.getStatus());
            return paymentResponse;
        } else {
            return createErrorResponse("AUTHORIZATION_FAILED", "Falha na autorização Cielo");
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
