package com.pip.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import java.util.List;

/**
 * DTO para requisição de configuração de webhook
 * 
 * @author Luiz Gustavo Finotello
 */
public class WebhookConfigRequest {

    @NotBlank(message = "URL do webhook é obrigatória")
    @Pattern(regexp = "^https?://.*", message = "URL deve começar com http:// ou https://")
    private String url;

    private String secret;

    private List<String> events;

    private boolean active = true;

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public List<String> getEvents() {
        return events;
    }

    public void setEvents(List<String> events) {
        this.events = events;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
