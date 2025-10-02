package com.pip.service;

import com.pip.dto.AuthorizationRequest;
import com.pip.dto.CaptureRequest;
import com.pip.dto.VoidRequest;
import com.pip.dto.PaymentResponse;
import com.pip.model.Gateway;
import com.pip.model.Transacao;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Serviço de integração com gateways de pagamento
 * 
 * Implementa padrões de resiliência:
 * - Circuit Breaker: Protege contra falhas em cascata
 * - Retry: Tenta novamente em caso de falhas temporárias
 * - Timeout: Limita tempo de espera
 * - Fallback: Roteamento alternativo em caso de falha
 * 
 * @author Luiz Gustavo Finotello
 */
@Service
public class GatewayIntegrationService {

    private static final Logger logger = LoggerFactory.getLogger(GatewayIntegrationService.class);

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private GatewayRoutingService routingService;

    /**
     * Processa autorização de pagamento com resiliência
     * 
     * @param gateway Gateway para processar
     * @param request Dados da autorização
     * @param transacao Transação sendo processada
     * @return Resposta do gateway
     */
    @CircuitBreaker(name = "gatewayService", fallbackMethod = "authorizeFallback")
    @Retry(name = "gatewayService")
    public PaymentResponse authorize(Gateway gateway, AuthorizationRequest request, Transacao transacao) {
        logger.info("Processando autorização no gateway {} para transação {}", 
            gateway.getCodigo(), transacao.getTransactionId());

        long startTime = System.currentTimeMillis();

        try {
            // Preparar requisição
            String url = gateway.getUrlAtiva() + "/authorize";
            HttpHeaders headers = prepararHeaders(gateway);
            Map<String, Object> payload = prepararPayloadAutorizacao(request, transacao);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);

            // Fazer chamada ao gateway
            ResponseEntity<Map> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                entity,
                Map.class
            );

            long responseTime = System.currentTimeMillis() - startTime;

            // Processar resposta
            PaymentResponse paymentResponse = processarResposta(response, responseTime);

            // Atualizar métricas do gateway
            atualizarMetricasGateway(gateway, true, responseTime);

            logger.info("Autorização processada com sucesso. Gateway: {}, Tempo: {}ms", 
                gateway.getCodigo(), responseTime);

            return paymentResponse;

        } catch (Exception e) {
            long responseTime = System.currentTimeMillis() - startTime;
            
            logger.error("Erro ao processar autorização no gateway {}: {}", 
                gateway.getCodigo(), e.getMessage());

            // Atualizar métricas do gateway (falha)
            atualizarMetricasGateway(gateway, false, responseTime);

            throw new RuntimeException("Falha na comunicação com gateway: " + e.getMessage(), e);
        }
    }

    /**
     * Fallback para autorização em caso de falha
     */
    private PaymentResponse authorizeFallback(Gateway gateway, AuthorizationRequest request, 
                                              Transacao transacao, Exception e) {
        logger.warn("Executando fallback para autorização. Gateway falho: {}, Erro: {}", 
            gateway.getCodigo(), e.getMessage());

        // Tentar gateway alternativo
        Gateway gatewayFallback = routingService.selecionarGatewayFallback(
            transacao.getLojista(), 
            gateway, 
            transacao.getValor()
        );

        if (gatewayFallback != null) {
            logger.info("Tentando gateway alternativo: {}", gatewayFallback.getCodigo());
            return authorize(gatewayFallback, request, transacao);
        }

        // Se não houver alternativa, retornar erro
        PaymentResponse errorResponse = new PaymentResponse();
        errorResponse.setSuccess(false);
        errorResponse.setStatus("FAILED");
        errorResponse.setErrorCode("GATEWAY_UNAVAILABLE");
        errorResponse.setErrorMessage("Todos os gateways estão indisponíveis no momento");
        errorResponse.setTimestamp(ZonedDateTime.now());

        return errorResponse;
    }

    /**
     * Processa captura de pagamento com resiliência
     */
    @CircuitBreaker(name = "gatewayService", fallbackMethod = "captureFallback")
    @Retry(name = "gatewayService")
    public PaymentResponse capture(Gateway gateway, CaptureRequest request, Transacao transacao) {
        logger.info("Processando captura no gateway {} para transação {}", 
            gateway.getCodigo(), transacao.getTransactionId());

        long startTime = System.currentTimeMillis();

        try {
            String url = gateway.getUrlAtiva() + "/capture";
            HttpHeaders headers = prepararHeaders(gateway);
            Map<String, Object> payload = prepararPayloadCaptura(request, transacao);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                entity,
                Map.class
            );

            long responseTime = System.currentTimeMillis() - startTime;

            PaymentResponse paymentResponse = processarResposta(response, responseTime);
            atualizarMetricasGateway(gateway, true, responseTime);

            logger.info("Captura processada com sucesso. Gateway: {}, Tempo: {}ms", 
                gateway.getCodigo(), responseTime);

            return paymentResponse;

        } catch (Exception e) {
            long responseTime = System.currentTimeMillis() - startTime;
            
            logger.error("Erro ao processar captura no gateway {}: {}", 
                gateway.getCodigo(), e.getMessage());

            atualizarMetricasGateway(gateway, false, responseTime);

            throw new RuntimeException("Falha na comunicação com gateway: " + e.getMessage(), e);
        }
    }

    /**
     * Fallback para captura
     */
    private PaymentResponse captureFallback(Gateway gateway, CaptureRequest request, 
                                           Transacao transacao, Exception e) {
        logger.error("Falha na captura sem possibilidade de fallback. Gateway: {}, Erro: {}", 
            gateway.getCodigo(), e.getMessage());

        PaymentResponse errorResponse = new PaymentResponse();
        errorResponse.setSuccess(false);
        errorResponse.setStatus("FAILED");
        errorResponse.setErrorCode("CAPTURE_FAILED");
        errorResponse.setErrorMessage("Falha ao capturar pagamento: " + e.getMessage());
        errorResponse.setTimestamp(ZonedDateTime.now());

        return errorResponse;
    }

    /**
     * Processa cancelamento de pagamento com resiliência
     */
    @CircuitBreaker(name = "gatewayService", fallbackMethod = "voidFallback")
    @Retry(name = "gatewayService")
    public PaymentResponse voidTransaction(Gateway gateway, VoidRequest request, Transacao transacao) {
        logger.info("Processando cancelamento no gateway {} para transação {}", 
            gateway.getCodigo(), transacao.getTransactionId());

        long startTime = System.currentTimeMillis();

        try {
            String url = gateway.getUrlAtiva() + "/void";
            HttpHeaders headers = prepararHeaders(gateway);
            Map<String, Object> payload = prepararPayloadVoid(request, transacao);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                entity,
                Map.class
            );

            long responseTime = System.currentTimeMillis() - startTime;

            PaymentResponse paymentResponse = processarResposta(response, responseTime);
            atualizarMetricasGateway(gateway, true, responseTime);

            logger.info("Cancelamento processado com sucesso. Gateway: {}, Tempo: {}ms", 
                gateway.getCodigo(), responseTime);

            return paymentResponse;

        } catch (Exception e) {
            long responseTime = System.currentTimeMillis() - startTime;
            
            logger.error("Erro ao processar cancelamento no gateway {}: {}", 
                gateway.getCodigo(), e.getMessage());

            atualizarMetricasGateway(gateway, false, responseTime);

            throw new RuntimeException("Falha na comunicação com gateway: " + e.getMessage(), e);
        }
    }

    /**
     * Fallback para cancelamento
     */
    private PaymentResponse voidFallback(Gateway gateway, VoidRequest request, 
                                        Transacao transacao, Exception e) {
        logger.error("Falha no cancelamento sem possibilidade de fallback. Gateway: {}, Erro: {}", 
            gateway.getCodigo(), e.getMessage());

        PaymentResponse errorResponse = new PaymentResponse();
        errorResponse.setSuccess(false);
        errorResponse.setStatus("FAILED");
        errorResponse.setErrorCode("VOID_FAILED");
        errorResponse.setErrorMessage("Falha ao cancelar pagamento: " + e.getMessage());
        errorResponse.setTimestamp(ZonedDateTime.now());

        return errorResponse;
    }

    /**
     * Prepara headers HTTP para requisição ao gateway
     */
    private HttpHeaders prepararHeaders(Gateway gateway) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        if (gateway.getApiKey() != null) {
            headers.set("Authorization", "Bearer " + gateway.getApiKey());
        }
        
        if (gateway.getMerchantId() != null) {
            headers.set("Merchant-Id", gateway.getMerchantId());
        }

        return headers;
    }

    /**
     * Prepara payload para autorização
     */
    private Map<String, Object> prepararPayloadAutorizacao(AuthorizationRequest request, Transacao transacao) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("transaction_id", transacao.getTransactionId());
        payload.put("amount", request.getAmount());
        payload.put("currency", request.getCurrency());
        payload.put("card_token", request.getCardToken());
        payload.put("installments", request.getInstallments());
        payload.put("description", request.getDescription());
        
        if (request.getCustomer() != null) {
            payload.put("customer", request.getCustomer());
        }

        return payload;
    }

    /**
     * Prepara payload para captura
     */
    private Map<String, Object> prepararPayloadCaptura(CaptureRequest request, Transacao transacao) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("transaction_id", transacao.getTransactionId());
        payload.put("gateway_transaction_id", transacao.getGatewayTransactionId());
        payload.put("amount", request.getAmount());

        return payload;
    }

    /**
     * Prepara payload para cancelamento
     */
    private Map<String, Object> prepararPayloadVoid(VoidRequest request, Transacao transacao) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("transaction_id", transacao.getTransactionId());
        payload.put("gateway_transaction_id", transacao.getGatewayTransactionId());
        payload.put("reason", request.getReason());

        return payload;
    }

    /**
     * Processa resposta do gateway
     */
    private PaymentResponse processarResposta(ResponseEntity<Map> response, long responseTime) {
        Map<String, Object> body = response.getBody();

        PaymentResponse paymentResponse = new PaymentResponse();
        paymentResponse.setSuccess(response.getStatusCode().is2xxSuccessful());
        paymentResponse.setStatus((String) body.get("status"));
        paymentResponse.setTransactionId((String) body.get("transaction_id"));
        paymentResponse.setGatewayTransactionId((String) body.get("gateway_transaction_id"));
        paymentResponse.setAuthorizationCode((String) body.get("authorization_code"));
        paymentResponse.setNsu((String) body.get("nsu"));
        paymentResponse.setTid((String) body.get("tid"));
        paymentResponse.setResponseTime(responseTime);
        paymentResponse.setTimestamp(ZonedDateTime.now());

        if (body.containsKey("error_code")) {
            paymentResponse.setErrorCode((String) body.get("error_code"));
            paymentResponse.setErrorMessage((String) body.get("error_message"));
        }

        return paymentResponse;
    }

    /**
     * Atualiza métricas do gateway após processamento
     */
    private void atualizarMetricasGateway(Gateway gateway, boolean sucesso, long tempoResposta) {
        gateway.registrarTransacao(sucesso, 0L, tempoResposta);
        // O save será feito pelo serviço que chamou este método
    }
}
