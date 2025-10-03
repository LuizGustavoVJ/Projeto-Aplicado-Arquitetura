package com.pip.dto;

import java.time.ZonedDateTime;
import java.util.List;

/**
 * DTO com informações da API Key
 */
public class ApiKeyInfo {
    
    private String keyId;
    private String lojistaId;
    private String lojistaNome;
    private List<String> scopes;
    private ZonedDateTime createdAt;
    private ZonedDateTime expiresAt;
    private boolean active;
    private String environment; // SANDBOX ou PRODUCTION
    
    public ApiKeyInfo() {
    }
    
    public ApiKeyInfo(String keyId, String lojistaId, String lojistaNome) {
        this.keyId = keyId;
        this.lojistaId = lojistaId;
        this.lojistaNome = lojistaNome;
    }
    
    // Getters and Setters
    public String getKeyId() {
        return keyId;
    }
    
    public void setKeyId(String keyId) {
        this.keyId = keyId;
    }
    
    public String getLojistaId() {
        return lojistaId;
    }
    
    public void setLojistaId(String lojistaId) {
        this.lojistaId = lojistaId;
    }
    
    public String getLojistaNome() {
        return lojistaNome;
    }
    
    public void setLojistaNome(String lojistaNome) {
        this.lojistaNome = lojistaNome;
    }
    
    public List<String> getScopes() {
        return scopes;
    }
    
    public void setScopes(List<String> scopes) {
        this.scopes = scopes;
    }
    
    public ZonedDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(ZonedDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public ZonedDateTime getExpiresAt() {
        return expiresAt;
    }
    
    public void setExpiresAt(ZonedDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }
    
    public boolean isActive() {
        return active;
    }
    
    public void setActive(boolean active) {
        this.active = active;
    }
    
    public String getEnvironment() {
        return environment;
    }
    
    public void setEnvironment(String environment) {
        this.environment = environment;
    }
}
