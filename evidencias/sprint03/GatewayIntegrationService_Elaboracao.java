package com.pip.service;

import org.springframework.stereotype.Service;

/**
 * Gateway Integration Service - Versão em Elaboração
 * Orquestra chamadas para diferentes gateways
 */
@Service
public class GatewayIntegrationService {
    
    // TODO: Injetar MockGatewayService
    // TODO: Implementar lógica de roteamento
    // TODO: Adicionar tratamento de erros
    // TODO: Implementar retry logic
    
    public String processTransaction(String cardToken, Double amount) {
        // Implementação básica
        return "PROCESSING";
    }
}

