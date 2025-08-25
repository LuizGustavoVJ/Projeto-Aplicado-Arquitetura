package com.pip.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pip.dto.AuditEvent;
import com.pip.dto.SecurityEventType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.security.MessageDigest;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Sistema centralizado de auditoria e logging de segurança
 * Implementa conformidade PCI DSS para rastreamento de eventos
 */
@Service
public class SecurityAuditLogger {
    
    private static final Logger securityLogger = LoggerFactory.getLogger("SECURITY_AUDIT");
    private static final Logger logger = LoggerFactory.getLogger(SecurityAuditLogger.class);
    
    @Autowired
    private KafkaTemplate<String, AuditEvent> kafkaTemplate;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    /**
     * Log de início de tokenização
     */
    public void logTokenizationStart(String requestId, String merchantId) {
        AuditEvent event = createAuditEvent(
            SecurityEventType.TOKENIZATION_START,
            merchantId,
            Map.of(
                "requestId", requestId,
                "action", "TOKENIZATION_INITIATED"
            )
        );
        
        logSecurityEvent(event);
    }
    
    /**
     * Log de tokenização bem-sucedida
     */
    public void logTokenizationSuccess(String requestId, String token, String merchantId) {
        AuditEvent event = createAuditEvent(
            SecurityEventType.TOKENIZATION_SUCCESS,
            merchantId,
            Map.of(
                "requestId", requestId,
                "tokenId", maskToken(token),
                "action", "TOKENIZATION_COMPLETED",
                "result", "SUCCESS"
            )
        );
        
        logSecurityEvent(event);
    }
    
    /**
     * Log de falha na tokenização
     */
    public void logTokenizationFailure(String requestId, String merchantId, Exception error) {
        AuditEvent event = createAuditEvent(
            SecurityEventType.TOKENIZATION_FAILURE,
            merchantId,
            Map.of(
                "requestId", requestId,
                "action", "TOKENIZATION_FAILED",
                "result", "FAILURE",
                "errorType", error.getClass().getSimpleName(),
                "errorMessage", error.getMessage()
            )
        );
        
        logSecurityEvent(event);
    }
    
    /**
     * Log de início de destokenização
     */
    public void logDetokenizationStart(String requestId, String token, String merchantId) {
        AuditEvent event = createAuditEvent(
            SecurityEventType.DETOKENIZATION_START,
            merchantId,
            Map.of(
                "requestId", requestId,
                "tokenId", maskToken(token),
                "action", "DETOKENIZATION_INITIATED"
            )
        );
        
        logSecurityEvent(event);
    }
    
    /**
     * Log de destokenização bem-sucedida
     */
    public void logDetokenizationSuccess(String requestId, String token, String merchantId) {
        AuditEvent event = createAuditEvent(
            SecurityEventType.DETOKENIZATION_SUCCESS,
            merchantId,
            Map.of(
                "requestId", requestId,
                "tokenId", maskToken(token),
                "action", "DETOKENIZATION_COMPLETED",
                "result", "SUCCESS"
            )
        );
        
        logSecurityEvent(event);
    }
    
    /**
     * Log de falha na destokenização
     */
    public void logDetokenizationFailure(String requestId, String token, String merchantId, Exception error) {
        AuditEvent event = createAuditEvent(
            SecurityEventType.DETOKENIZATION_FAILURE,
            merchantId,
            Map.of(
                "requestId", requestId,
                "tokenId", maskToken(token),
                "action", "DETOKENIZATION_FAILED",
                "result", "FAILURE",
                "errorType", error.getClass().getSimpleName(),
                "errorMessage", error.getMessage()
            )
        );
        
        logSecurityEvent(event);
    }
    
    /**
     * Log de tentativa de destokenização não autorizada
     */
    public void logUnauthorizedDetokenization(String requestId, String token, String merchantId) {
        AuditEvent event = createAuditEvent(
            SecurityEventType.UNAUTHORIZED_ACCESS,
            merchantId,
            Map.of(
                "requestId", requestId,
                "tokenId", maskToken(token),
                "action", "UNAUTHORIZED_DETOKENIZATION",
                "result", "BLOCKED",
                "severity", "HIGH"
            )
        );
        
        logSecurityEvent(event);
    }
    
    /**
     * Log de uso de API Key
     */
    public void logApiKeyUsage(String merchantId, String apiKey) {
        AuditEvent event = createAuditEvent(
            SecurityEventType.API_KEY_USAGE,
            merchantId,
            Map.of(
                "apiKeyId", maskApiKey(apiKey),
                "action", "API_KEY_VALIDATED",
                "result", "SUCCESS"
            )
        );
        
        logSecurityEvent(event);
    }
    
    /**
     * Log de tentativa de API Key inválida
     */
    public void logInvalidApiKeyAttempt(String apiKey, String reason) {
        AuditEvent event = createAuditEvent(
            SecurityEventType.INVALID_API_KEY,
            "UNKNOWN",
            Map.of(
                "apiKeyId", maskApiKey(apiKey),
                "action", "API_KEY_VALIDATION_FAILED",
                "result", "FAILURE",
                "reason", reason,
                "severity", "MEDIUM"
            )
        );
        
        logSecurityEvent(event);
    }
    
    /**
     * Log de rate limit excedido
     */
    public void logRateLimitExceeded(String apiKey, String merchantId) {
        AuditEvent event = createAuditEvent(
            SecurityEventType.RATE_LIMIT_EXCEEDED,
            merchantId,
            Map.of(
                "apiKeyId", maskApiKey(apiKey),
                "action", "RATE_LIMIT_EXCEEDED",
                "result", "BLOCKED",
                "severity", "MEDIUM"
            )
        );
        
        logSecurityEvent(event);
    }
    
    /**
     * Log de geração de API Key
     */
    public void logApiKeyGeneration(String merchantId, String environment) {
        AuditEvent event = createAuditEvent(
            SecurityEventType.API_KEY_GENERATED,
            merchantId,
            Map.of(
                "action", "API_KEY_GENERATED",
                "environment", environment,
                "result", "SUCCESS"
            )
        );
        
        logSecurityEvent(event);
    }
    
    /**
     * Log de rotação de API Key
     */
    public void logApiKeyRotation(String merchantId, String oldApiKey) {
        AuditEvent event = createAuditEvent(
            SecurityEventType.API_KEY_ROTATED,
            merchantId,
            Map.of(
                "oldApiKeyId", maskApiKey(oldApiKey),
                "action", "API_KEY_ROTATED",
                "result", "SUCCESS"
            )
        );
        
        logSecurityEvent(event);
    }
    
    /**
     * Log de necessidade de rotação de API Key
     */
    public void logApiKeyRotationNeeded(String merchantId) {
        AuditEvent event = createAuditEvent(
            SecurityEventType.API_KEY_ROTATION_NEEDED,
            merchantId,
            Map.of(
                "action", "API_KEY_ROTATION_NEEDED",
                "severity", "LOW"
            )
        );
        
        logSecurityEvent(event);
    }
    
    /**
     * Log de revogação de API Key
     */
    public void logApiKeyRevocation(String merchantId, String apiKey) {
        AuditEvent event = createAuditEvent(
            SecurityEventType.API_KEY_REVOKED,
            merchantId,
            Map.of(
                "apiKeyId", maskApiKey(apiKey),
                "action", "API_KEY_REVOKED",
                "result", "SUCCESS"
            )
        );
        
        logSecurityEvent(event);
    }
    
    /**
     * Log de erro na validação de API Key
     */
    public void logApiKeyValidationError(String apiKey, Exception error) {
        AuditEvent event = createAuditEvent(
            SecurityEventType.API_KEY_VALIDATION_ERROR,
            "UNKNOWN",
            Map.of(
                "apiKeyId", maskApiKey(apiKey),
                "action", "API_KEY_VALIDATION_ERROR",
                "result", "ERROR",
                "errorType", error.getClass().getSimpleName(),
                "errorMessage", error.getMessage(),
                "severity", "HIGH"
            )
        );
        
        logSecurityEvent(event);
    }
    
    /**
     * Log de erro na geração de API Key
     */
    public void logApiKeyGenerationError(String merchantId, String environment, Exception error) {
        AuditEvent event = createAuditEvent(
            SecurityEventType.API_KEY_GENERATION_ERROR,
            merchantId,
            Map.of(
                "action", "API_KEY_GENERATION_ERROR",
                "environment", environment,
                "result", "ERROR",
                "errorType", error.getClass().getSimpleName(),
                "errorMessage", error.getMessage(),
                "severity", "HIGH"
            )
        );
        
        logSecurityEvent(event);
    }
    
    /**
     * Log de erro na rotação de API Key
     */
    public void logApiKeyRotationError(String merchantId, Exception error) {
        AuditEvent event = createAuditEvent(
            SecurityEventType.API_KEY_ROTATION_ERROR,
            merchantId,
            Map.of(
                "action", "API_KEY_ROTATION_ERROR",
                "result", "ERROR",
                "errorType", error.getClass().getSimpleName(),
                "errorMessage", error.getMessage(),
                "severity", "HIGH"
            )
        );
        
        logSecurityEvent(event);
    }
    
    /**
     * Log de erro na revogação de API Key
     */
    public void logApiKeyRevocationError(String merchantId, Exception error) {
        AuditEvent event = createAuditEvent(
            SecurityEventType.API_KEY_REVOCATION_ERROR,
            merchantId,
            Map.of(
                "action", "API_KEY_REVOCATION_ERROR",
                "result", "ERROR",
                "errorType", error.getClass().getSimpleName(),
                "errorMessage", error.getMessage(),
                "severity", "HIGH"
            )
        );
        
        logSecurityEvent(event);
    }
    
    /**
     * Log de transação de pagamento
     */
    public void logPaymentTransaction(String merchantId, String transactionId, String gatewayUsed, String result) {
        AuditEvent event = createAuditEvent(
            SecurityEventType.PAYMENT_TRANSACTION,
            merchantId,
            Map.of(
                "transactionId", transactionId,
                "gatewayUsed", gatewayUsed,
                "action", "PAYMENT_PROCESSED",
                "result", result
            )
        );
        
        logSecurityEvent(event);
    }
    
    /**
     * Log de acesso não autorizado
     */
    public void logUnauthorizedAccess(String ipAddress) {
        AuditEvent event = createAuditEvent(
            SecurityEventType.UNAUTHORIZED_ACCESS,
            "UNKNOWN",
            Map.of(
                "ipAddress", ipAddress,
                "action", "UNAUTHORIZED_ACCESS_ATTEMPT",
                "result", "BLOCKED",
                "severity", "HIGH"
            )
        );
        
        logSecurityEvent(event);
    }
    
    private AuditEvent createAuditEvent(SecurityEventType eventType, String merchantId, Map<String, Object> data) {
        String eventId = UUID.randomUUID().toString();
        Instant timestamp = Instant.now();
        
        // Adicionar informações de contexto
        Map<String, Object> enrichedData = new HashMap<>(data);
        enrichedData.put("source", "pip-api");
        enrichedData.put("version", "1.0.0");
        
        // Calcular checksum para integridade
        String checksum = calculateChecksum(eventId, timestamp, eventType, merchantId, enrichedData);
        
        return AuditEvent.builder()
            .eventId(eventId)
            .timestamp(timestamp)
            .eventType(eventType)
            .merchantId(merchantId)
            .data(enrichedData)
            .checksum(checksum)
            .build();
    }
    
    private void logSecurityEvent(AuditEvent event) {
        try {
            // Configurar MDC para logging estruturado
            MDC.put("eventId", event.getEventId());
            MDC.put("eventType", event.getEventType().toString());
            MDC.put("merchantId", event.getMerchantId());
            MDC.put("timestamp", event.getTimestamp().toString());
            
            // Log local estruturado
            securityLogger.info("SECURITY_EVENT: {}", objectMapper.writeValueAsString(event));
            
            // Enviar para Kafka para processamento assíncrono
            kafkaTemplate.send("security-events", event.getEventId(), event);
            
        } catch (Exception e) {
            logger.error("Failed to log security event", e);
        } finally {
            // Limpar MDC
            MDC.clear();
        }
    }
    
    private String maskToken(String token) {
        if (token == null || token.length() < 8) {
            return "****";
        }
        
        return token.substring(0, 8) + "****" + token.substring(token.length() - 4);
    }
    
    private String maskApiKey(String apiKey) {
        if (apiKey == null || apiKey.length() < 12) {
            return "****";
        }
        
        return apiKey.substring(0, 12) + "****";
    }
    
    private String calculateChecksum(String eventId, Instant timestamp, SecurityEventType eventType, 
                                   String merchantId, Map<String, Object> data) {
        try {
            String input = eventId + timestamp.toString() + eventType.toString() + 
                          merchantId + data.toString();
            
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes("UTF-8"));
            
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            
            return "sha256:" + hexString.toString();
            
        } catch (Exception e) {
            logger.error("Failed to calculate checksum", e);
            return "sha256:error";
        }
    }
}

