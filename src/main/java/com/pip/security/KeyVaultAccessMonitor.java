package com.pip.security;

import com.azure.security.keyvault.secrets.SecretClient;
import com.azure.core.http.rest.PagedIterable;
import com.azure.security.keyvault.secrets.models.SecretProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Serviço de Monitoramento de Acesso ao Azure Key Vault
 * 
 * Funcionalidades:
 * - Monitoramento contínuo de acessos
 * - Detecção de acessos suspeitos
 * - Alertas em tempo real
 * - Auditoria completa
 * - Métricas de uso
 */
@Service
public class KeyVaultAccessMonitor {
    
    private static final Logger logger = LoggerFactory.getLogger(KeyVaultAccessMonitor.class);
    private static final Logger auditLogger = LoggerFactory.getLogger("AUDIT");
    
    @Autowired(required = false)
    private SecretClient secretClient;
    
    // Armazena último acesso de cada secret
    private final Map<String, LocalDateTime> lastAccessMap = new ConcurrentHashMap<>();
    
    // Armazena contadores de acesso
    private final Map<String, Long> accessCountMap = new ConcurrentHashMap<>();
    
    // Armazena alertas de segurança
    private final List<SecurityAlert> securityAlerts = Collections.synchronizedList(new ArrayList<>());
    
    // Limites de segurança
    private static final int MAX_ACCESS_PER_MINUTE = 60;
    private static final int MAX_FAILED_ATTEMPTS = 5;
    
    /**
     * Monitora acessos ao Key Vault a cada minuto
     */
    @Scheduled(fixedRate = 60000) // A cada 1 minuto
    public void monitorAccess() {
        if (secretClient == null) {
            logger.warn("SecretClient não configurado. Monitoramento desabilitado.");
            return;
        }
        
        try {
            logger.info("🔍 Iniciando monitoramento de acesso ao Key Vault...");
            
            // Lista todos os secrets
            PagedIterable<SecretProperties> secrets = secretClient.listPropertiesOfSecrets();
            
            int totalSecrets = 0;
            int accessedSecrets = 0;
            
            for (SecretProperties secretProperties : secrets) {
                totalSecrets++;
                String secretName = secretProperties.getName();
                LocalDateTime updatedOn = secretProperties.getUpdatedOn() != null 
                    ? LocalDateTime.ofInstant(secretProperties.getUpdatedOn().toInstant(), ZoneId.systemDefault())
                    : null;
                
                // Verifica se houve acesso recente
                if (updatedOn != null && updatedOn.isAfter(LocalDateTime.now().minusMinutes(1))) {
                    accessedSecrets++;
                    recordAccess(secretName, updatedOn);
                }
                
                // Verifica padrões suspeitos
                checkSuspiciousActivity(secretName);
            }
            
            logger.info("✅ Monitoramento concluído: {} secrets, {} acessados recentemente", 
                totalSecrets, accessedSecrets);
            
            // Gera relatório de métricas
            generateMetricsReport();
            
        } catch (Exception e) {
            logger.error("❌ Erro ao monitorar acesso ao Key Vault: {}", e.getMessage(), e);
            createAlert("MONITORING_ERROR", "Erro no monitoramento: " + e.getMessage(), "HIGH");
        }
    }
    
    /**
     * Registra acesso a um secret
     */
    private void recordAccess(String secretName, LocalDateTime accessTime) {
        lastAccessMap.put(secretName, accessTime);
        accessCountMap.merge(secretName, 1L, Long::sum);
        
        auditLogger.info("KEY_VAULT_ACCESS | secret={} | time={} | count={}", 
            secretName, accessTime, accessCountMap.get(secretName));
    }
    
    /**
     * Verifica atividades suspeitas
     */
    private void checkSuspiciousActivity(String secretName) {
        Long accessCount = accessCountMap.getOrDefault(secretName, 0L);
        
        // Alerta: Muitos acessos em curto período
        if (accessCount > MAX_ACCESS_PER_MINUTE) {
            createAlert(
                "EXCESSIVE_ACCESS",
                String.format("Secret '%s' acessado %d vezes em 1 minuto (limite: %d)", 
                    secretName, accessCount, MAX_ACCESS_PER_MINUTE),
                "HIGH"
            );
        }
        
        // Alerta: Acesso a secret sensível
        if (secretName.contains("prod") || secretName.contains("master")) {
            auditLogger.warn("⚠️ SENSITIVE_SECRET_ACCESS | secret={} | count={}", 
                secretName, accessCount);
        }
    }
    
    /**
     * Cria alerta de segurança
     */
    private void createAlert(String type, String message, String severity) {
        SecurityAlert alert = new SecurityAlert(
            UUID.randomUUID().toString(),
            type,
            message,
            severity,
            LocalDateTime.now()
        );
        
        securityAlerts.add(alert);
        
        logger.warn("🚨 SECURITY_ALERT | type={} | severity={} | message={}", 
            type, severity, message);
        
        auditLogger.warn("SECURITY_ALERT | {}", alert);
        
        // Em produção, enviar para sistema de alertas (Slack, PagerDuty, etc)
        sendAlertNotification(alert);
    }
    
    /**
     * Envia notificação de alerta
     */
    private void sendAlertNotification(SecurityAlert alert) {
        // TODO: Integrar com sistema de notificações
        // Exemplos: Slack, PagerDuty, Email, SMS
        logger.info("📧 Enviando notificação de alerta: {}", alert.getMessage());
    }
    
    /**
     * Gera relatório de métricas
     */
    private void generateMetricsReport() {
        long totalAccesses = accessCountMap.values().stream().mapToLong(Long::longValue).sum();
        int uniqueSecrets = accessCountMap.size();
        int totalAlerts = securityAlerts.size();
        
        logger.info("📊 MÉTRICAS KEY VAULT:");
        logger.info("   - Total de acessos: {}", totalAccesses);
        logger.info("   - Secrets únicos acessados: {}", uniqueSecrets);
        logger.info("   - Alertas de segurança: {}", totalAlerts);
        
        // Top 5 secrets mais acessados
        accessCountMap.entrySet().stream()
            .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
            .limit(5)
            .forEach(entry -> 
                logger.info("   - {} : {} acessos", entry.getKey(), entry.getValue())
            );
    }
    
    /**
     * Limpa dados antigos (executado diariamente)
     */
    @Scheduled(cron = "0 0 0 * * *") // Meia-noite todos os dias
    public void cleanupOldData() {
        logger.info("🧹 Limpando dados antigos de monitoramento...");
        
        LocalDateTime cutoff = LocalDateTime.now().minusDays(7);
        
        // Remove acessos antigos
        lastAccessMap.entrySet().removeIf(entry -> entry.getValue().isBefore(cutoff));
        
        // Reseta contadores
        accessCountMap.clear();
        
        // Remove alertas antigos
        securityAlerts.removeIf(alert -> alert.getTimestamp().isBefore(cutoff));
        
        logger.info("✅ Limpeza concluída");
    }
    
    /**
     * Retorna alertas de segurança recentes
     */
    public List<SecurityAlert> getRecentAlerts(int hours) {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(hours);
        return securityAlerts.stream()
            .filter(alert -> alert.getTimestamp().isAfter(cutoff))
            .toList();
    }
    
    /**
     * Retorna métricas de acesso
     */
    public Map<String, Object> getAccessMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("totalAccesses", accessCountMap.values().stream().mapToLong(Long::longValue).sum());
        metrics.put("uniqueSecrets", accessCountMap.size());
        metrics.put("totalAlerts", securityAlerts.size());
        metrics.put("lastMonitoring", LocalDateTime.now());
        return metrics;
    }
    
    /**
     * Classe interna para alertas de segurança
     */
    public static class SecurityAlert {
        private final String id;
        private final String type;
        private final String message;
        private final String severity;
        private final LocalDateTime timestamp;
        
        public SecurityAlert(String id, String type, String message, String severity, LocalDateTime timestamp) {
            this.id = id;
            this.type = type;
            this.message = message;
            this.severity = severity;
            this.timestamp = timestamp;
        }
        
        public String getId() { return id; }
        public String getType() { return type; }
        public String getMessage() { return message; }
        public String getSeverity() { return severity; }
        public LocalDateTime getTimestamp() { return timestamp; }
        
        @Override
        public String toString() {
            return String.format("SecurityAlert{id='%s', type='%s', severity='%s', message='%s', timestamp=%s}",
                id, type, severity, message, timestamp);
        }
    }
}
