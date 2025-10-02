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
 * Adaptador para integração com Cielo
 * 
 * Implementa comunicação com API Cielo E-Commerce para:
 * - Autorização de pagamentos
 * - Captura de pagamentos
 * - Cancelamento de pagamentos
 * 
 * Documentação: https://developercielo.github.io/manual/cielo-ecommerce
 * 
 * @author Luiz Gustavo Finotello
 */
@Component
public class CieloAdapter implements GatewayAdapter {

    private static final Logger logger = LoggerFactory.getLogger(CieloAdapter.class);
    private static final String GATEWAY_CODE = "CIELO";

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public PaymentResponse authorize(Gateway gateway, AuthorizationRequest request, Transacao transacao) {
        logger.info("Iniciando autorização Cielo para transação: {}", transacao.getTransactionId());

        try {
            // Construir payload Cielo
            Map<String, Object> payload = new HashMap<>();
            payload.put("MerchantOrderId", transacao.getTransactionId());
            payload.put("Customer", Map.of(
                "Name", request.getCustomer() != null ? request.getCustomer().get("name") : "Cliente"
            ));

            // Dados do pagamento
            Map<String, Object> payment = new HashMap<>();
            payment.put("Type", "CreditCard");
            payment.put("Amount", (int) (request.getAmount() * 100)); // Cielo usa centavos
            payment.put("Currency", request.getCurrency());
            payment.put("Country", "BRA");
            payment.put("Installments", request.getInstallments());
            payment.put("Capture", false); // Autorização sem captura automática
            payment.put("SoftDescriptor", "PIP");

            // Dados do cartão (usando token)
            Map<String, Object> creditCard = new HashMap<>();
            creditCard.put("CardToken", request.getCardToken());
            payment.put("CreditCard", creditCard);

            payload.put("Payment", payment);

            // Configurar headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("MerchantId", gateway.getMerchantId());
            headers.set("MerchantKey", gateway.getMerchantKey());

            // Fazer requisição
            String url = gateway.getApiUrl() + "/1/sales";
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
                Map<String, Object> paymentData = (Map<String, Object>) responseBody.get("Payment");

                PaymentResponse paymentResponse = new PaymentResponse();
                paymentResponse.setSuccess(true);
                paymentResponse.setStatus("AUTHORIZED");
                paymentResponse.setTransactionId(transacao.getTransactionId());
                paymentResponse.setGatewayTransactionId((String) paymentData.get("PaymentId"));
                paymentResponse.setAuthorizationCode((String) paymentData.get("AuthorizationCode"));
                paymentResponse.setNsu((String) paymentData.get("ProofOfSale"));
                paymentResponse.setTid((String) paymentData.get("Tid"));
                paymentResponse.setTimestamp(ZonedDateTime.now());

                logger.info("Autorização Cielo bem-sucedida: {}", paymentResponse.getGatewayTransactionId());
                return paymentResponse;
            } else {
                return createErrorResponse("AUTHORIZATION_FAILED", "Falha na autorização Cielo");
            }

        } catch (Exception e) {
            logger.error("Erro na autorização Cielo: {}", e.getMessage(), e);
            return createErrorResponse("GATEWAY_ERROR", e.getMessage());
        }
    }

    @Override
    public PaymentResponse capture(Gateway gateway, CaptureRequest request, Transacao transacao) {
        logger.info("Iniciando captura Cielo para transação: {}", transacao.getTransactionId());

        try {
            // Configurar headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("MerchantId", gateway.getMerchantId());
            headers.set("MerchantKey", gateway.getMerchantKey());

            // Fazer requisição de captura
            String url = String.format("%s/1/sales/%s/capture?amount=%d",
                gateway.getApiUrl(),
                transacao.getGatewayTransactionId(),
                (int) (request.getAmount() * 100)
            );

            HttpEntity<Void> entity = new HttpEntity<>(headers);
            
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
                paymentResponse.setGatewayTransactionId(transacao.getGatewayTransactionId());
                paymentResponse.setTimestamp(ZonedDateTime.now());

                logger.info("Captura Cielo bem-sucedida: {}", transacao.getGatewayTransactionId());
                return paymentResponse;
            } else {
                return createErrorResponse("CAPTURE_FAILED", "Falha na captura Cielo");
            }

        } catch (Exception e) {
            logger.error("Erro na captura Cielo: {}", e.getMessage(), e);
            return createErrorResponse("GATEWAY_ERROR", e.getMessage());
        }
    }

    @Override
    public PaymentResponse voidTransaction(Gateway gateway, VoidRequest request, Transacao transacao) {
        logger.info("Iniciando cancelamento Cielo para transação: {}", transacao.getTransactionId());

        try {
            // Configurar headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("MerchantId", gateway.getMerchantId());
            headers.set("MerchantKey", gateway.getMerchantKey());

            // Fazer requisição de cancelamento
            String url = String.format("%s/1/sales/%s/void",
                gateway.getApiUrl(),
                transacao.getGatewayTransactionId()
            );

            HttpEntity<Void> entity = new HttpEntity<>(headers);
            
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

                logger.info("Cancelamento Cielo bem-sucedido: {}", transacao.getGatewayTransactionId());
                return paymentResponse;
            } else {
                return createErrorResponse("VOID_FAILED", "Falha no cancelamento Cielo");
            }

        } catch (Exception e) {
            logger.error("Erro no cancelamento Cielo: {}", e.getMessage(), e);
            return createErrorResponse("GATEWAY_ERROR", e.getMessage());
        }
    }

    @Override
    public boolean healthCheck(Gateway gateway) {
        try {
            // Fazer requisição simples para verificar conectividade
            HttpHeaders headers = new HttpHeaders();
            headers.set("MerchantId", gateway.getMerchantId());
            headers.set("MerchantKey", gateway.getMerchantKey());

            String url = gateway.getApiUrl() + "/1/sales/health";
            HttpEntity<Void> entity = new HttpEntity<>(headers);
            
            ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                String.class
            );

            return response.getStatusCode().is2xxSuccessful();

        } catch (Exception e) {
            logger.warn("Health check Cielo falhou: {}", e.getMessage());
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
