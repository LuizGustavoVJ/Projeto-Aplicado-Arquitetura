package com.pip.service;

import com.pip.dto.GatewayResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Random;
import java.util.UUID;

/**
 * Mock Gateway Service - Simulador de Gateway de Pagamento
 * Implementa diferentes cenários para validação da PoC
 */
@Service
public class MockGatewayService {
    
    private static final Logger logger = LoggerFactory.getLogger(MockGatewayService.class);
    private final Random random = new Random();
    
    /**
     * Processa uma transação simulando comportamento real de gateway
     */
    public GatewayResponse processPayment(String cardToken, Double amount, String currency) {
        logger.info("Processando pagamento - Token: {}, Valor: {} {}", cardToken, amount, currency);
        
        // Simula latência real de gateway (100-500ms)
        simulateLatency();
        
        // Determina cenário baseado no valor
        return determineScenario(amount, cardToken);
    }
    
    /**
     * Simula latência realística de gateway de pagamento
     */
    private void simulateLatency() {
        try {
            int latency = 100 + random.nextInt(400); // 100-500ms
            Thread.sleep(latency);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    /**
     * Determina cenário de resposta baseado em regras de negócio
     */
    private GatewayResponse determineScenario(Double amount, String cardToken) {
        GatewayResponse response = new GatewayResponse();
        response.setTransactionId(UUID.randomUUID().toString());
        response.setTimestamp(LocalDateTime.now());
        response.setAmount(amount);
        
        // Cenários baseados no valor da transação
        if (amount <= 0) {
            return createErrorResponse(response, "INVALID_AMOUNT", "Valor inválido");
        } else if (amount > 10000) {
            return createDeclinedResponse(response, "LIMIT_EXCEEDED", "Limite excedido");
        } else if (cardToken.contains("error")) {
            return createErrorResponse(response, "INVALID_CARD", "Cartão inválido");
        } else if (cardToken.contains("declined")) {
            return createDeclinedResponse(response, "INSUFFICIENT_FUNDS", "Saldo insuficiente");
        } else if (random.nextInt(100) < 5) { // 5% de falha aleatória
            return createErrorResponse(response, "GATEWAY_ERROR", "Erro interno do gateway");
        } else {
            return createApprovedResponse(response);
        }
    }
    
    private GatewayResponse createApprovedResponse(GatewayResponse response) {
        response.setStatus("APPROVED");
        response.setAuthorizationCode("AUTH" + random.nextInt(999999));
        response.setMessage("Transação aprovada com sucesso");
        logger.info("Transação aprovada - ID: {}", response.getTransactionId());
        return response;
    }
    
    private GatewayResponse createDeclinedResponse(GatewayResponse response, String code, String message) {
        response.setStatus("DECLINED");
        response.setErrorCode(code);
        response.setMessage(message);
        logger.warn("Transação negada - ID: {}, Motivo: {}", response.getTransactionId(), code);
        return response;
    }
    
    private GatewayResponse createErrorResponse(GatewayResponse response, String code, String message) {
        response.setStatus("ERROR");
        response.setErrorCode(code);
        response.setMessage(message);
        logger.error("Erro na transação - ID: {}, Erro: {}", response.getTransactionId(), code);
        return response;
    }
}

