package com.pip.dto;

import java.util.List;

/**
 * DTO para requisição de Split de Pagamentos
 * 
 * Permite dividir o valor de uma transação entre múltiplos recebedores
 * Usado principalmente no PagSeguro e Mercado Pago (Marketplace)
 * 
 * @author Luiz Gustavo Finotello
 */
public class SplitRequest {
    
    private List<SplitReceiver> receivers;
    private String splitType; // "PERCENTAGE" ou "FIXED"
    private boolean chargeProcessingFee;
    
    public static class SplitReceiver {
        private String receiverId;
        private String receiverName;
        private String receiverDocument;
        private Double amount; // Valor fixo ou percentual
        private String receiverType; // "SELLER", "MARKETPLACE", "PARTNER"
        private boolean chargeFee;
        
        // Getters e Setters
        public String getReceiverId() { return receiverId; }
        public void setReceiverId(String receiverId) { this.receiverId = receiverId; }
        
        public String getReceiverName() { return receiverName; }
        public void setReceiverName(String receiverName) { this.receiverName = receiverName; }
        
        public String getReceiverDocument() { return receiverDocument; }
        public void setReceiverDocument(String receiverDocument) { this.receiverDocument = receiverDocument; }
        
        public Double getAmount() { return amount; }
        public void setAmount(Double amount) { this.amount = amount; }
        
        public String getReceiverType() { return receiverType; }
        public void setReceiverType(String receiverType) { this.receiverType = receiverType; }
        
        public boolean isChargeFee() { return chargeFee; }
        public void setChargeFee(boolean chargeFee) { this.chargeFee = chargeFee; }
    }
    
    // Getters e Setters
    public List<SplitReceiver> getReceivers() { return receivers; }
    public void setReceivers(List<SplitReceiver> receivers) { this.receivers = receivers; }
    
    public String getSplitType() { return splitType; }
    public void setSplitType(String splitType) { this.splitType = splitType; }
    
    public boolean isChargeProcessingFee() { return chargeProcessingFee; }
    public void setChargeProcessingFee(boolean chargeProcessingFee) { this.chargeProcessingFee = chargeProcessingFee; }
}
