package com.pip.dto;

/**
 * Enum para tipos de eventos de segurança auditados
 */
public enum SecurityEventType {
    
    // Eventos de Tokenização
    TOKENIZATION_START("Início de processo de tokenização"),
    TOKENIZATION_SUCCESS("Tokenização realizada com sucesso"),
    TOKENIZATION_FAILURE("Falha na tokenização"),
    
    // Eventos de Destokenização
    DETOKENIZATION_START("Início de processo de destokenização"),
    DETOKENIZATION_SUCCESS("Destokenização realizada com sucesso"),
    DETOKENIZATION_FAILURE("Falha na destokenização"),
    
    // Eventos de API Key
    API_KEY_USAGE("Uso de API Key"),
    API_KEY_GENERATED("API Key gerada"),
    API_KEY_ROTATED("API Key rotacionada"),
    API_KEY_REVOKED("API Key revogada"),
    API_KEY_ROTATION_NEEDED("Rotação de API Key necessária"),
    INVALID_API_KEY("Tentativa de uso de API Key inválida"),
    API_KEY_VALIDATION_ERROR("Erro na validação de API Key"),
    API_KEY_GENERATION_ERROR("Erro na geração de API Key"),
    API_KEY_ROTATION_ERROR("Erro na rotação de API Key"),
    API_KEY_REVOCATION_ERROR("Erro na revogação de API Key"),
    
    // Eventos de Segurança
    UNAUTHORIZED_ACCESS("Tentativa de acesso não autorizado"),
    RATE_LIMIT_EXCEEDED("Limite de requisições excedido"),
    SUSPICIOUS_ACTIVITY("Atividade suspeita detectada"),
    SECURITY_BREACH("Violação de segurança"),
    
    // Eventos de Transação
    PAYMENT_TRANSACTION("Transação de pagamento processada"),
    PAYMENT_AUTHORIZATION("Autorização de pagamento"),
    PAYMENT_CAPTURE("Captura de pagamento"),
    PAYMENT_REFUND("Estorno de pagamento"),
    PAYMENT_CANCELLATION("Cancelamento de pagamento"),
    
    // Eventos de Sistema
    SYSTEM_STARTUP("Inicialização do sistema"),
    SYSTEM_SHUTDOWN("Desligamento do sistema"),
    CONFIGURATION_CHANGE("Alteração de configuração"),
    DATABASE_CONNECTION("Conexão com banco de dados"),
    
    // Eventos de Auditoria
    AUDIT_LOG_ACCESS("Acesso a logs de auditoria"),
    AUDIT_LOG_EXPORT("Exportação de logs de auditoria"),
    COMPLIANCE_CHECK("Verificação de conformidade"),
    
    // Eventos de Incidentes
    INCIDENT_DETECTED("Incidente de segurança detectado"),
    INCIDENT_RESOLVED("Incidente de segurança resolvido"),
    EMERGENCY_MODE_ACTIVATED("Modo de emergência ativado"),
    EMERGENCY_MODE_DEACTIVATED("Modo de emergência desativado");
    
    private final String description;
    
    SecurityEventType(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
    
    /**
     * Determina a severidade padrão do evento
     */
    public String getDefaultSeverity() {
        switch (this) {
            case UNAUTHORIZED_ACCESS:
            case SECURITY_BREACH:
            case INCIDENT_DETECTED:
            case EMERGENCY_MODE_ACTIVATED:
                return "CRITICAL";
                
            case API_KEY_VALIDATION_ERROR:
            case API_KEY_GENERATION_ERROR:
            case SUSPICIOUS_ACTIVITY:
            case RATE_LIMIT_EXCEEDED:
                return "HIGH";
                
            case INVALID_API_KEY:
            case TOKENIZATION_FAILURE:
            case DETOKENIZATION_FAILURE:
            case API_KEY_ROTATION_NEEDED:
                return "MEDIUM";
                
            case API_KEY_USAGE:
            case TOKENIZATION_SUCCESS:
            case DETOKENIZATION_SUCCESS:
            case PAYMENT_TRANSACTION:
                return "LOW";
                
            default:
                return "INFO";
        }
    }
    
    /**
     * Verifica se o evento requer resposta imediata
     */
    public boolean requiresImmediateResponse() {
        return "CRITICAL".equals(getDefaultSeverity()) || "HIGH".equals(getDefaultSeverity());
    }
}

