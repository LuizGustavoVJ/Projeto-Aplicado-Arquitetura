package com.pip.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Serviço para gerenciar logs de auditoria
 * Registra todas as ações críticas do sistema para compliance PCI-DSS e LGPD
 */
@Service
public class AuditLogService {
    
    private static final Logger auditLogger = LoggerFactory.getLogger("com.pip.audit");
    
    public enum AuditAction {
        PAYMENT_CREATED,
        PAYMENT_AUTHORIZED,
        PAYMENT_CAPTURED,
        PAYMENT_VOIDED,
        PAYMENT_REFUNDED,
        API_KEY_CREATED,
        API_KEY_REVOKED,
        WEBHOOK_CONFIGURED,
        WEBHOOK_SENT,
        GATEWAY_CONFIGURED,
        USER_LOGIN,
        USER_LOGOUT,
        SENSITIVE_DATA_ACCESSED,
        CONFIGURATION_CHANGED
    }
    
    /**
     * Registra um evento de auditoria
     */
    public void logAuditEvent(AuditAction action, String userId, String resourceId, Map<String, Object> details) {
        Map<String, Object> auditData = new HashMap<>();
        auditData.put("timestamp", LocalDateTime.now());
        auditData.put("action", action.name());
        auditData.put("userId", userId);
        auditData.put("resourceId", resourceId);
        auditData.put("details", details);
        auditData.put("traceId", MDC.get("traceId"));
        auditData.put("ipAddress", MDC.get("clientIp"));
        
        auditLogger.info("Audit event: {}", auditData);
    }
    
    /**
     * Registra criação de pagamento
     */
    public void logPaymentCreated(String userId, String transactionId, String gateway, Double amount) {
        Map<String, Object> details = new HashMap<>();
        details.put("gateway", gateway);
        details.put("amount", amount);
        details.put("currency", "BRL");
        
        logAuditEvent(AuditAction.PAYMENT_CREATED, userId, transactionId, details);
    }
    
    /**
     * Registra autorização de pagamento
     */
    public void logPaymentAuthorized(String userId, String transactionId, String authorizationCode) {
        Map<String, Object> details = new HashMap<>();
        details.put("authorizationCode", authorizationCode);
        
        logAuditEvent(AuditAction.PAYMENT_AUTHORIZED, userId, transactionId, details);
    }
    
    /**
     * Registra captura de pagamento
     */
    public void logPaymentCaptured(String userId, String transactionId, Double amount) {
        Map<String, Object> details = new HashMap<>();
        details.put("capturedAmount", amount);
        
        logAuditEvent(AuditAction.PAYMENT_CAPTURED, userId, transactionId, details);
    }
    
    /**
     * Registra cancelamento de pagamento
     */
    public void logPaymentVoided(String userId, String transactionId, String reason) {
        Map<String, Object> details = new HashMap<>();
        details.put("reason", reason);
        
        logAuditEvent(AuditAction.PAYMENT_VOIDED, userId, transactionId, details);
    }
    
    /**
     * Registra acesso a dados sensíveis
     */
    public void logSensitiveDataAccess(String userId, String dataType, String resourceId) {
        Map<String, Object> details = new HashMap<>();
        details.put("dataType", dataType);
        details.put("accessTime", LocalDateTime.now());
        
        logAuditEvent(AuditAction.SENSITIVE_DATA_ACCESSED, userId, resourceId, details);
    }
    
    /**
     * Registra mudança de configuração
     */
    public void logConfigurationChange(String userId, String configKey, String oldValue, String newValue) {
        Map<String, Object> details = new HashMap<>();
        details.put("configKey", configKey);
        details.put("oldValue", oldValue);
        details.put("newValue", newValue);
        
        logAuditEvent(AuditAction.CONFIGURATION_CHANGED, userId, configKey, details);
    }
}
