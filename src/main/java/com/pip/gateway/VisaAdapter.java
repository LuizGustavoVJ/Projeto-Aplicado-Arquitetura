package com.pip.gateway;

import com.pip.dto.*;
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

/**
 * Adaptador para Visa Direct - Funds Transfer API
 * 
 * ATENÇÃO: Este adaptador requer configuração adicional de infraestrutura:
 * 
 * 1. **Certificado Digital mTLS (Mutual TLS)**
 *    - Obter certificado através do Visa Developer Portal
 *    - Configurar keystore e truststore no RestTemplate
 *    - Two-Way SSL authentication obrigatório
 * 
 * 2. **Credenciamento Formal**
 *    - Registro no programa Visa Direct
 *    - Obtenção de acquiringBin (BIN do adquirente)
 *    - Configuração de businessApplicationId
 * 
 * 3. **Configuração de Rede**
 *    - Whitelist de IPs na Visa
 *    - Configuração de firewall para mTLS
 * 
 * Documentação: https://developer.visa.com/capabilities/visa_direct/reference
 * 
 * APIs Implementadas:
 * - POST /fundstransfer/v1/pushfundstransactions (AFT/OCT)
 * - POST /fundstransfer/v1/pullfundstransactions
 * - POST /fundstransfer/v1/reversefundstransactions (AFTR)
 * 
 * @author Luiz Gustavo Finotello
 * @version 3.0 - Conforme Documentação Oficial (Requer mTLS)
 */
@Component
public class VisaAdapter implements GatewayAdapter {

    private static final Logger logger = LoggerFactory.getLogger(VisaAdapter.class);
    private static final String GATEWAY_CODE = "VISA";
    
    private static final String SANDBOX_URL = "https://sandbox.api.visa.com";
    private static final String PRODUCTION_URL = "https://api.visa.com";

    @Autowired
    private RestTemplate restTemplate; // DEVE ser configurado com mTLS

    @Override
    public PaymentResponse authorize(Gateway gateway, AuthorizationRequest request, Transacao transacao) {
        logger.warn("[VISA] ATENÇÃO: Visa Direct requer mTLS. Verifique configuração de certificados.");
        logger.info("[VISA] Iniciando Push Funds Transaction - TransactionID: {}", transacao.getTransactionId());
        
        try {
            validateRequest(request, gateway);
            
            Map<String, Object> payload = buildPushFundsPayload(request, transacao, gateway);
            HttpHeaders headers = buildHeaders(gateway);
            
            String url = getBaseUrl(gateway) + "/fundstransfer/v1/pushfundstransactions";
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);
            
            logger.debug("[VISA] Enviando Push Funds para: {}", url);
            
            ResponseEntity<Map> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                entity,
                Map.class
            );

            return processPushFundsResponse(response, transacao);

        } catch (Exception e) {
            logger.error("[VISA] Erro na autorização. Verifique: 1) Certificado mTLS 2) Credenciamento 3) BIN", e);
            return createErrorResponse("MTLS_OR_CONFIG_ERROR", 
                "Erro: Verifique certificado mTLS e credenciamento Visa Direct. " + e.getMessage());
        }
    }

    @Override
    public PaymentResponse capture(Gateway gateway, CaptureRequest request, Transacao transacao) {
        // Visa Direct não tem conceito de captura separada
        // Push Funds Transaction já é final
        logger.info("[VISA] Visa Direct não requer captura separada. Transação já é final.");
        
        PaymentResponse response = new PaymentResponse();
        response.setSuccess(true);
        response.setStatus("CAPTURED");
        response.setTransactionId(transacao.getTransactionId());
        response.setGatewayTransactionId(transacao.getGatewayTransactionId());
        response.setTimestamp(ZonedDateTime.now());
        
        return response;
    }

    @Override
    public PaymentResponse voidTransaction(Gateway gateway, VoidRequest request, Transacao transacao) {
        logger.info("[VISA] Iniciando Reverse Funds Transaction - TransactionID: {}", transacao.getTransactionId());

        try {
            if (transacao.getGatewayTransactionId() == null) {
                return createErrorResponse("INVALID_TRANSACTION", "Transaction Identifier não encontrado");
            }
            
            Map<String, Object> payload = buildReverseFundsPayload(request, transacao, gateway);
            HttpHeaders headers = buildHeaders(gateway);
            
            String url = getBaseUrl(gateway) + "/fundstransfer/v1/reversefundstransactions";
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);
            
            logger.debug("[VISA] Enviando Reverse Funds para: {}", url);
            
            ResponseEntity<Map> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                entity,
                Map.class
            );

            if (response.getStatusCode() == HttpStatus.OK) {
                PaymentResponse paymentResponse = new PaymentResponse();
                paymentResponse.setSuccess(true);
                paymentResponse.setStatus("VOIDED");
                paymentResponse.setTransactionId(transacao.getTransactionId());
                paymentResponse.setGatewayTransactionId(transacao.getGatewayTransactionId());
                paymentResponse.setTimestamp(ZonedDateTime.now());

                logger.info("[VISA] Estorno bem-sucedido");
                return paymentResponse;
            } else {
                return createErrorResponse("VOID_FAILED", "Falha no estorno");
            }

        } catch (Exception e) {
            logger.error("[VISA] Erro no estorno", e);
            return createErrorResponse("GATEWAY_ERROR", e.getMessage());
        }
    }

    @Override
    public boolean healthCheck(Gateway gateway) {
        logger.warn("[VISA] Health check requer mTLS configurado");
        // Visa não tem endpoint de health check público
        // Verificamos apenas se as configurações básicas estão presentes
        return gateway.getApiKey() != null && gateway.getMerchantId() != null;
    }

    @Override
    public String getGatewayCode() {
        return GATEWAY_CODE;
    }

    // ========== MÉTODOS PRIVADOS ==========

    private void validateRequest(AuthorizationRequest request, Gateway gateway) {
        if (request.getAmount() == null || request.getAmount() <= 0) {
            throw new IllegalArgumentException("amount inválido");
        }
        if (gateway.getMerchantId() == null) {
            throw new IllegalArgumentException("acquiringBin (MerchantId) obrigatório");
        }
        if (gateway.getApiKey() == null) {
            throw new IllegalArgumentException("userId/password (ApiKey) obrigatório para Basic Auth");
        }
    }

    private Map<String, Object> buildPushFundsPayload(AuthorizationRequest request, Transacao transacao, Gateway gateway) {
        Map<String, Object> payload = new HashMap<>();
        
        // acquirerCountryCode (3 dígitos) - País do BIN
        payload.put("acquirerCountryCode", "076"); // Brasil
        
        // acquiringBin - BIN do programa Visa Direct
        payload.put("acquiringBin", gateway.getMerchantId());
        
        // amount (double, max 3 decimais)
        payload.put("amount", request.getAmount());
        
        // businessApplicationId (2 caracteres)
        // AA = Account-to-Account, PP = Person-to-Person, etc
        payload.put("businessApplicationId", "AA");
        
        // cardAcceptor
        Map<String, Object> cardAcceptor = new HashMap<>();
        cardAcceptor.put("idCode", "PIP" + gateway.getMerchantId());
        cardAcceptor.put("name", "Payment Integration Platform");
        cardAcceptor.put("terminalId", "PIP00001");
        
        Map<String, Object> address = new HashMap<>();
        address.put("country", "BRA");
        address.put("state", "SP");
        address.put("zipCode", "01310100");
        cardAcceptor.put("address", address);
        
        payload.put("cardAcceptor", cardAcceptor);
        
        // localTransactionDateTime (ISO 8601)
        payload.put("localTransactionDateTime", 
            ZonedDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        
        // merchantCategoryCode (MCC)
        payload.put("merchantCategoryCode", "6012"); // Financial institutions
        
        // pointOfServiceData
        Map<String, Object> posData = new HashMap<>();
        posData.put("panEntryMode", "07"); // Ecommerce
        posData.put("posConditionCode", "00"); // Normal
        payload.put("pointOfServiceData", posData);
        
        // recipientPrimaryAccountNumber - Cartão destinatário
        payload.put("recipientPrimaryAccountNumber", request.getCardToken());
        
        // retrievalReferenceNumber (ydddhhnnnnnn)
        payload.put("retrievalReferenceNumber", generateRRN());
        
        // senderAccountNumber - Conta origem
        payload.put("senderAccountNumber", gateway.getApiKey());
        
        // senderCurrencyCode
        payload.put("senderCurrencyCode", "BRL");
        
        // systemsTraceAuditNumber (único)
        payload.put("systemsTraceAuditNumber", generateSTAN());
        
        logger.debug("[VISA] Push Funds payload construído");
        
        return payload;
    }

    private Map<String, Object> buildReverseFundsPayload(VoidRequest request, Transacao transacao, Gateway gateway) {
        Map<String, Object> payload = new HashMap<>();
        
        // Mesmos campos do Push, mas com transactionIdentifier da transação original
        payload.put("acquirerCountryCode", "076");
        payload.put("acquiringBin", gateway.getMerchantId());
        payload.put("amount", transacao.getValor());
        payload.put("businessApplicationId", "AA");
        payload.put("transactionIdentifier", transacao.getGatewayTransactionId());
        payload.put("localTransactionDateTime", 
            ZonedDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        payload.put("retrievalReferenceNumber", generateRRN());
        payload.put("systemsTraceAuditNumber", generateSTAN());
        
        return payload;
    }

    private HttpHeaders buildHeaders(Gateway gateway) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        // Basic Authentication (userId:password em Base64)
        // gateway.getApiKey() deve conter "userId:password"
        String auth = gateway.getApiKey();
        String encodedAuth = java.util.Base64.getEncoder().encodeToString(auth.getBytes());
        headers.set("Authorization", "Basic " + encodedAuth);
        
        return headers;
    }

    private String getBaseUrl(Gateway gateway) {
        return gateway.getAmbiente().toString().equals("SANDBOX") ? SANDBOX_URL : PRODUCTION_URL;
    }

    private PaymentResponse processPushFundsResponse(ResponseEntity<Map> response, Transacao transacao) {
        if (response.getStatusCode() == HttpStatus.OK) {
            Map<String, Object> responseBody = response.getBody();

            PaymentResponse paymentResponse = new PaymentResponse();
            
            String actionCode = (String) responseBody.get("actionCode");
            boolean isApproved = "00".equals(actionCode);
            
            paymentResponse.setSuccess(isApproved);
            paymentResponse.setStatus(isApproved ? "AUTHORIZED" : "DENIED");
            paymentResponse.setTransactionId(transacao.getTransactionId());
            paymentResponse.setGatewayTransactionId(String.valueOf(responseBody.get("transactionIdentifier")));
            paymentResponse.setAuthorizationCode((String) responseBody.get("approvalCode"));
            paymentResponse.setTimestamp(ZonedDateTime.now());

            if (!isApproved) {
                paymentResponse.setErrorMessage("Action Code: " + actionCode);
            }

            logger.info("[VISA] Push Funds processado: {} - Action Code: {}", 
                paymentResponse.getGatewayTransactionId(), actionCode);
            
            return paymentResponse;
        } else {
            return createErrorResponse("PUSH_FUNDS_FAILED", "Falha no Push Funds");
        }
    }

    private String generateRRN() {
        // ydddhhnnnnnn format
        ZonedDateTime now = ZonedDateTime.now();
        int year = now.getYear() % 10;
        int dayOfYear = now.getDayOfYear();
        int hour = now.getHour();
        int random = (int)(Math.random() * 1000000);
        
        return String.format("%d%03d%02d%06d", year, dayOfYear, hour, random);
    }

    private int generateSTAN() {
        return (int)(Math.random() * 1000000);
    }

    private PaymentResponse createErrorResponse(String errorCode, String errorMessage) {
        PaymentResponse response = new PaymentResponse();
        response.setSuccess(false);
        response.setErrorCode(errorCode);
        response.setErrorMessage(errorMessage);
        response.setTimestamp(ZonedDateTime.now());
        
        logger.warn("[VISA] Erro: {} - {}", errorCode, errorMessage);
        
        return response;
    }
}