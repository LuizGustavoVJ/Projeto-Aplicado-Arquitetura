package com.pip.service;

import com.pip.dto.GatewayResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Gateway Integration Service - Orquestrador de Gateways
 * Implementa padrão Adapter e lógica de roteamento
 */
@Service
public class GatewayIntegrationService {
    
    private static final Logger logger = LoggerFactory.getLogger(GatewayIntegrationService.class);
    
    @Autowired
    private MockGatewayService mockGatewayService;
    
    /**
     * Processa transação com roteamento inteligente
     */
    public GatewayResponse processTransaction(String cardToken, Double amount, String currency) {
        logger.info("Iniciando processamento - Valor: {} {}", amount, currency);
        
        try {
            // Validações básicas
            validateTransaction(cardToken, amount);
            
            // Seleciona gateway (por enquanto apenas mock)
            String selectedGateway = selectGateway(amount, currency);
            logger.info("Gateway selecionado: {}", selectedGateway);
            
            // Processa no gateway selecionado
            GatewayResponse response = routeToGateway(selectedGateway, cardToken, amount, currency);
            
            // Log do resultado
            logger.info("Transação processada - Status: {}, ID: {}", 
                       response.getStatus(), response.getTransactionId());
            
            return response;
            
        } catch (Exception e) {
            logger.error("Erro no processamento da transação", e);
            return createErrorResponse(e.getMessage());
        }
    }
    
    /**
     * Valida dados da transação
     */
    private void validateTransaction(String cardToken, Double amount) {
        if (cardToken == null || cardToken.trim().isEmpty()) {
            throw new IllegalArgumentException("Token do cartão é obrigatório");
        }
        
        if (amount == null || amount <= 0) {
            throw new IllegalArgumentException("Valor deve ser maior que zero");
        }
        
        if (amount > 50000) {
            throw new IllegalArgumentException("Valor excede limite máximo");
        }
    }
    
    /**
     * Seleciona gateway baseado em regras de negócio
     */
    private String selectGateway(Double amount, String currency) {
        // Lógica de roteamento simples
        if (amount > 1000) {
            return "MOCK_PREMIUM"; // Gateway para valores altos
        } else {
            return "MOCK_STANDARD"; // Gateway padrão
        }
    }
    
    /**
     * Roteia transação para gateway específico
     */
    private GatewayResponse routeToGateway(String gateway, String cardToken, Double amount, String currency) {
        switch (gateway) {
            case "MOCK_PREMIUM":
            case "MOCK_STANDARD":
                return mockGatewayService.processPayment(cardToken, amount, currency);
            default:
                throw new IllegalStateException("Gateway não suportado: " + gateway);
        }
    }
    
    /**
     * Cria resposta de erro padronizada
     */
    private GatewayResponse createErrorResponse(String message) {
        GatewayResponse response = new GatewayResponse();
        response.setStatus("ERROR");
        response.setErrorCode("INTEGRATION_ERROR");
        response.setMessage(message);
        return response;
    }
    
    /**
     * Implementa retry logic para falhas temporárias
     */
    public GatewayResponse processWithRetry(String cardToken, Double amount, String currency, int maxRetries) {
        GatewayResponse lastResponse = null;
        
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                lastResponse = processTransaction(cardToken, amount, currency);
                
                // Se sucesso ou erro definitivo, retorna
                if (!"ERROR".equals(lastResponse.getStatus()) || 
                    !"GATEWAY_ERROR".equals(lastResponse.getErrorCode())) {
                    return lastResponse;
                }
                
                // Se erro temporário e não é última tentativa, aguarda
                if (attempt < maxRetries) {
                    logger.warn("Tentativa {} falhou, tentando novamente...", attempt);
                    Thread.sleep(1000 * attempt); // Backoff exponencial
                }
                
            } catch (Exception e) {
                logger.error("Erro na tentativa {}: {}", attempt, e.getMessage());
                if (attempt == maxRetries) {
                    return createErrorResponse("Falha após " + maxRetries + " tentativas");
                }
            }
        }
        
        return lastResponse;
    }
}

