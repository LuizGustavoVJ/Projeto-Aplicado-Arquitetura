package com.pip.dto;

import java.time.ZonedDateTime;
import java.util.Map;

/**
 * DTO para eventos de auditoria
 */
public class AuditEvent {
    
    private String eventType;
    private String userId;
    private String action;
    private String resource;
    private String ipAddress;
    private String userAgent;
    private ZonedDateTime timestamp;
    private Map<String, Object> metadata;
    private String severity;
    private boolean success;
    
    public AuditEvent() {
        this.timestamp = ZonedDateTime.now();
    }
    
    public AuditEvent(String eventType, String userId, String action) {
        this();
        this.eventType = eventType;
        this.userId = userId;
        this.action = action;
    }
    
    // Getters and Setters
    public String getEventType() {
        return eventType;
    }
    
    public void setEventType(String eventType) {
        this.eventType = eventType;
    }
    
    public String getUserId() {
        return userId;
    }
    
    public void setUserId(String userId) {
        this.userId = userId;
    }
    
    public String getAction() {
        return action;
    }
    
    public void setAction(String action) {
        this.action = action;
    }
    
    public String getResource() {
        return resource;
    }
    
    public void setResource(String resource) {
        this.resource = resource;
    }
    
    public String getIpAddress() {
        return ipAddress;
    }
    
    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }
    
    public String getUserAgent() {
        return userAgent;
    }
    
    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }
    
    public ZonedDateTime getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(ZonedDateTime timestamp) {
        this.timestamp = timestamp;
    }
    
    public Map<String, Object> getMetadata() {
        return metadata;
    }
    
    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }
    
    public String getSeverity() {
        return severity;
    }
    
    public void setSeverity(String severity) {
        this.severity = severity;
    }
    
    public boolean isSuccess() {
        return success;
    }
    
    public void setSuccess(boolean success) {
        this.success = success;
    }
}
