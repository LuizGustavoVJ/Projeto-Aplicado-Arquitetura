package com.pip.service;

import org.springframework.stereotype.Service;
import java.util.Random;

/**
 * Mock Gateway Service - Versão em Elaboração
 * Simula um gateway de pagamento para testes da PoC
 */
@Service
public class MockGatewayService {
    
    private Random random = new Random();
    
    // TODO: Implementar simulação de respostas
    public String processPayment(String cardToken, Double amount) {
        // Implementação básica
        return "APPROVED";
    }
    
    // TODO: Adicionar diferentes cenários
    // TODO: Simular latência
    // TODO: Implementar respostas de erro
}

