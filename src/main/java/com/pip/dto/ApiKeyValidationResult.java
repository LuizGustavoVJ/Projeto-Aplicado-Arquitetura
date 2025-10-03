package com.pip.dto;

/**
 * DTO com resultado da validação de API Key
 */
public class ApiKeyValidationResult {
    
    private boolean valid;
    private String reason;
    private ApiKeyInfo keyInfo;
    
    public ApiKeyValidationResult() {
    }
    
    public ApiKeyValidationResult(boolean valid, String reason) {
        this.valid = valid;
        this.reason = reason;
    }
    
    public static ApiKeyValidationResult valid(ApiKeyInfo keyInfo) {
        ApiKeyValidationResult result = new ApiKeyValidationResult(true, "Valid API Key");
        result.setKeyInfo(keyInfo);
        return result;
    }
    
    public static ApiKeyValidationResult invalid(String reason) {
        return new ApiKeyValidationResult(false, reason);
    }
    
    // Getters and Setters
    public boolean isValid() {
        return valid;
    }
    
    public void setValid(boolean valid) {
        this.valid = valid;
    }
    
    public String getReason() {
        return reason;
    }
    
    public void setReason(String reason) {
        this.reason = reason;
    }
    
    public ApiKeyInfo getKeyInfo() {
        return keyInfo;
    }
    
    public void setKeyInfo(ApiKeyInfo keyInfo) {
        this.keyInfo = keyInfo;
    }
}
