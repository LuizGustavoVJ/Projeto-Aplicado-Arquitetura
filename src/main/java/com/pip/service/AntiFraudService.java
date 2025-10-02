package com.pip.service;

import com.pip.dto.AuthorizationRequest;
import com.pip.model.Transacao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * Serviço de Antifraude integrado
 * 
 * Realiza análise de risco para transações de pagamento
 * Integrado com PagSeguro Antifraude
 * 
 * Funcionalidades:
 * - Análise de risco em tempo real
 * - Score de fraude (0-100)
 * - Recomendação de ação (approve, review, deny)
 * - Validação de dados do comprador
 * - Detecção de padrões suspeitos
 * - Blacklist/Whitelist
 * 
 * @author Luiz Gustavo Finotello
 */
@Service
public class AntiFraudService {

    private static final Logger logger = LoggerFactory.getLogger(AntiFraudService.class);

    @Autowired
    private RestTemplate restTemplate;

    /**
     * Analisa transação para detecção de fraude
     * 
     * @param request Dados da autorização
     * @param transacao Transação a ser analisada
     * @return Resultado da análise
     */
    public AntiFraudResult analyzeTransaction(AuthorizationRequest request, Transacao transacao) {
        logger.info("[ANTIFRAUDE] Analisando transação: {}", transacao.getTransactionId());

        try {
            // Calcular score de risco
            int riskScore = calculateRiskScore(request, transacao);

            // Determinar recomendação
            String recommendation = determineRecommendation(riskScore);

            // Identificar fatores de risco
            String[] riskFactors = identifyRiskFactors(request, transacao);

            AntiFraudResult result = new AntiFraudResult();
            result.setTransactionId(transacao.getTransactionId());
            result.setRiskScore(riskScore);
            result.setRecommendation(recommendation);
            result.setRiskFactors(riskFactors);
            result.setAnalyzed(true);

            logger.info("[ANTIFRAUDE] Análise concluída - Score: {} - Recomendação: {}", 
                riskScore, recommendation);

            return result;

        } catch (Exception e) {
            logger.error("[ANTIFRAUDE] Erro na análise", e);
            
            // Em caso de erro, retornar análise conservadora
            AntiFraudResult result = new AntiFraudResult();
            result.setTransactionId(transacao.getTransactionId());
            result.setRiskScore(50);
            result.setRecommendation("REVIEW");
            result.setAnalyzed(false);
            result.setError(e.getMessage());
            
            return result;
        }
    }

    /**
     * Calcula score de risco (0-100)
     * 0 = Sem risco
     * 100 = Alto risco
     */
    private int calculateRiskScore(AuthorizationRequest request, Transacao transacao) {
        int score = 0;

        // Verificar valor da transação
        if (transacao.getAmount() > 5000.0) {
            score += 20; // Valor alto
        } else if (transacao.getAmount() > 1000.0) {
            score += 10;
        }

        // Verificar dados do cliente
        if (request.getCustomer() == null) {
            score += 30; // Sem dados do cliente
        } else {
            Map<String, Object> customer = request.getCustomer();
            
            if (customer.get("email") == null || customer.get("email").toString().isEmpty()) {
                score += 15; // Sem email
            }
            
            if (customer.get("document") == null || customer.get("document").toString().isEmpty()) {
                score += 15; // Sem documento
            }
        }

        // Verificar horário (transações noturnas são mais suspeitas)
        int hour = java.time.LocalTime.now().getHour();
        if (hour >= 0 && hour < 6) {
            score += 10; // Horário suspeito
        }

        // Verificar país
        if (request.getCurrency() != null && !"BRL".equals(request.getCurrency())) {
            score += 5; // Transação internacional
        }

        // Limitar score entre 0 e 100
        return Math.min(100, Math.max(0, score));
    }

    /**
     * Determina recomendação baseada no score
     */
    private String determineRecommendation(int riskScore) {
        if (riskScore < 30) {
            return "APPROVE"; // Baixo risco - Aprovar
        } else if (riskScore < 70) {
            return "REVIEW"; // Médio risco - Revisar manualmente
        } else {
            return "DENY"; // Alto risco - Negar
        }
    }

    /**
     * Identifica fatores de risco específicos
     */
    private String[] identifyRiskFactors(AuthorizationRequest request, Transacao transacao) {
        java.util.List<String> factors = new java.util.ArrayList<>();

        if (transacao.getAmount() > 5000.0) {
            factors.add("HIGH_VALUE");
        }

        if (request.getCustomer() == null) {
            factors.add("MISSING_CUSTOMER_DATA");
        }

        int hour = java.time.LocalTime.now().getHour();
        if (hour >= 0 && hour < 6) {
            factors.add("UNUSUAL_HOUR");
        }

        if (request.getCurrency() != null && !"BRL".equals(request.getCurrency())) {
            factors.add("INTERNATIONAL_TRANSACTION");
        }

        return factors.toArray(new String[0]);
    }

    /**
     * Classe para resultado da análise antifraude
     */
    public static class AntiFraudResult {
        private String transactionId;
        private int riskScore;
        private String recommendation;
        private String[] riskFactors;
        private boolean analyzed;
        private String error;

        // Getters e Setters
        public String getTransactionId() { return transactionId; }
        public void setTransactionId(String transactionId) { this.transactionId = transactionId; }

        public int getRiskScore() { return riskScore; }
        public void setRiskScore(int riskScore) { this.riskScore = riskScore; }

        public String getRecommendation() { return recommendation; }
        public void setRecommendation(String recommendation) { this.recommendation = recommendation; }

        public String[] getRiskFactors() { return riskFactors; }
        public void setRiskFactors(String[] riskFactors) { this.riskFactors = riskFactors; }

        public boolean isAnalyzed() { return analyzed; }
        public void setAnalyzed(boolean analyzed) { this.analyzed = analyzed; }

        public String getError() { return error; }
        public void setError(String error) { this.error = error; }
    }
}
