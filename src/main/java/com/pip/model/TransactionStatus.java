package com.pip.model;

/**
 * Enum que define os possíveis status de uma transação
 */
public enum TransactionStatus {
    
    // Status iniciais
    PENDING("pending", "Transação pendente de processamento"),
    AUTHORIZED("authorized", "Transação autorizada, aguardando captura"),
    
    // Status de sucesso
    CAPTURED("captured", "Transação capturada com sucesso"),
    SETTLED("settled", "Transação liquidada"),
    
    // Status de cancelamento
    VOIDED("voided", "Transação cancelada"),
    REFUNDED("refunded", "Transação estornada"),
    
    // Status de falha
    DECLINED("declined", "Transação negada pelo gateway"),
    DENIED("denied", "Transação negada"),
    FAILED("failed", "Transação falhou por erro técnico"),
    EXPIRED("expired", "Transação expirou"),
    
    // Status de processamento
    PROCESSING("processing", "Transação em processamento"),
    REVIEW("review", "Transação em análise de risco");
    
    private final String code;
    private final String description;
    
    TransactionStatus(String code, String description) {
        this.code = code;
        this.description = description;
    }
    
    public String getCode() {
        return code;
    }
    
    public String getDescription() {
        return description;
    }
    
    /**
     * Verifica se a transação pode ser capturada
     */
    public boolean canCapture() {
        return this == AUTHORIZED;
    }
    
    /**
     * Verifica se a transação pode ser cancelada
     */
    public boolean canVoid() {
        return this == AUTHORIZED || this == PENDING;
    }
    
    /**
     * Verifica se a transação pode ser estornada
     */
    public boolean canRefund() {
        return this == CAPTURED || this == SETTLED;
    }
    
    /**
     * Verifica se a transação está em estado final
     */
    public boolean isFinal() {
        return this == CAPTURED || this == SETTLED || this == VOIDED || 
               this == REFUNDED || this == DECLINED || this == FAILED || this == EXPIRED;
    }
    
    /**
     * Converte string para enum
     */
    public static TransactionStatus fromCode(String code) {
        for (TransactionStatus status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Status inválido: " + code);
    }
    
    @Override
    public String toString() {
        return code;
    }
}

