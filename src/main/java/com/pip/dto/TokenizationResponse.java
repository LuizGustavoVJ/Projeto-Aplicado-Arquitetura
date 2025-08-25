package com.pip.dto;

import java.time.Instant;

/**
 * DTO para resposta de tokenização
 */
public class TokenizationResponse {
    
    private String token;
    private String tokenType;
    private Instant expiresAt;
    private String requestId;
    
    // Constructors
    public TokenizationResponse() {}
    
    public TokenizationResponse(String token, String tokenType, Instant expiresAt, String requestId) {
        this.token = token;
        this.tokenType = tokenType;
        this.expiresAt = expiresAt;
        this.requestId = requestId;
    }
    
    // Builder pattern
    public static TokenizationResponseBuilder builder() {
        return new TokenizationResponseBuilder();
    }
    
    // Getters and Setters
    public String getToken() {
        return token;
    }
    
    public void setToken(String token) {
        this.token = token;
    }
    
    public String getTokenType() {
        return tokenType;
    }
    
    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }
    
    public Instant getExpiresAt() {
        return expiresAt;
    }
    
    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }
    
    public String getRequestId() {
        return requestId;
    }
    
    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }
    
    // Builder class
    public static class TokenizationResponseBuilder {
        private TokenizationResponse response = new TokenizationResponse();
        
        public TokenizationResponseBuilder token(String token) {
            response.token = token;
            return this;
        }
        
        public TokenizationResponseBuilder tokenType(String tokenType) {
            response.tokenType = tokenType;
            return this;
        }
        
        public TokenizationResponseBuilder expiresAt(Instant expiresAt) {
            response.expiresAt = expiresAt;
            return this;
        }
        
        public TokenizationResponseBuilder requestId(String requestId) {
            response.requestId = requestId;
            return this;
        }
        
        public TokenizationResponse build() {
            return response;
        }
    }
}

