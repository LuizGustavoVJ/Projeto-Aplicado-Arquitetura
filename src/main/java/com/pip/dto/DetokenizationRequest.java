package com.pip.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * DTO para requisição de destokenização
 */
public class DetokenizationRequest {
    
    @NotBlank(message = "Token is required")
    private String token;
    
    @NotBlank(message = "Merchant ID is required")
    private String merchantId;
    
    @NotBlank(message = "Purpose is required")
    private String purpose;
    
    // Constructors
    public DetokenizationRequest() {}
    
    public DetokenizationRequest(String token, String merchantId, String purpose) {
        this.token = token;
        this.merchantId = merchantId;
        this.purpose = purpose;
    }
    
    // Builder pattern
    public static DetokenizationRequestBuilder builder() {
        return new DetokenizationRequestBuilder();
    }
    
    // Getters and Setters
    public String getToken() {
        return token;
    }
    
    public void setToken(String token) {
        this.token = token;
    }
    
    public String getMerchantId() {
        return merchantId;
    }
    
    public void setMerchantId(String merchantId) {
        this.merchantId = merchantId;
    }
    
    public String getPurpose() {
        return purpose;
    }
    
    public void setPurpose(String purpose) {
        this.purpose = purpose;
    }
    
    // Builder class
    public static class DetokenizationRequestBuilder {
        private DetokenizationRequest request = new DetokenizationRequest();
        
        public DetokenizationRequestBuilder token(String token) {
            request.token = token;
            return this;
        }
        
        public DetokenizationRequestBuilder merchantId(String merchantId) {
            request.merchantId = merchantId;
            return this;
        }
        
        public DetokenizationRequestBuilder purpose(String purpose) {
            request.purpose = purpose;
            return this;
        }
        
        public DetokenizationRequest build() {
            return request;
        }
    }
}

