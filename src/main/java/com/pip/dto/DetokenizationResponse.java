package com.pip.dto;

/**
 * DTO para resposta de destokenização
 */
public class DetokenizationResponse {
    
    private String pan;
    private String cvv;
    private String expiryDate;
    private String cardholderName;
    private String requestId;
    
    // Constructors
    public DetokenizationResponse() {}
    
    public DetokenizationResponse(String pan, String cvv, String expiryDate, String cardholderName, String requestId) {
        this.pan = pan;
        this.cvv = cvv;
        this.expiryDate = expiryDate;
        this.cardholderName = cardholderName;
        this.requestId = requestId;
    }
    
    // Builder pattern
    public static DetokenizationResponseBuilder builder() {
        return new DetokenizationResponseBuilder();
    }
    
    // Getters and Setters
    public String getPan() {
        return pan;
    }
    
    public void setPan(String pan) {
        this.pan = pan;
    }
    
    public String getCvv() {
        return cvv;
    }
    
    public void setCvv(String cvv) {
        this.cvv = cvv;
    }
    
    public String getExpiryDate() {
        return expiryDate;
    }
    
    public void setExpiryDate(String expiryDate) {
        this.expiryDate = expiryDate;
    }
    
    public String getCardholderName() {
        return cardholderName;
    }
    
    public void setCardholderName(String cardholderName) {
        this.cardholderName = cardholderName;
    }
    
    public String getRequestId() {
        return requestId;
    }
    
    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }
    
    // Builder class
    public static class DetokenizationResponseBuilder {
        private DetokenizationResponse response = new DetokenizationResponse();
        
        public DetokenizationResponseBuilder pan(String pan) {
            response.pan = pan;
            return this;
        }
        
        public DetokenizationResponseBuilder cvv(String cvv) {
            response.cvv = cvv;
            return this;
        }
        
        public DetokenizationResponseBuilder expiryDate(String expiryDate) {
            response.expiryDate = expiryDate;
            return this;
        }
        
        public DetokenizationResponseBuilder cardholderName(String cardholderName) {
            response.cardholderName = cardholderName;
            return this;
        }
        
        public DetokenizationResponseBuilder requestId(String requestId) {
            response.requestId = requestId;
            return this;
        }
        
        public DetokenizationResponse build() {
            return response;
        }
    }
}

