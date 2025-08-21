package com.pip.dto;

import java.util.UUID;
import java.util.Map;

/**
 * DTO para resposta de operações de pagamento
 * 
 * @author Luiz Gustavo Finotello
 */
public class PaymentResponse {

    private UUID id;
    private String status;
    private Long amount;
    private Map<String, Object> gatewayDetails;

    // Construtores
    public PaymentResponse() {}

    public PaymentResponse(UUID id, String status, Long amount, Map<String, Object> gatewayDetails) {
        this.id = id;
        this.status = status;
        this.amount = amount;
        this.gatewayDetails = gatewayDetails;
    }

    // Getters e Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Long getAmount() {
        return amount;
    }

    public void setAmount(Long amount) {
        this.amount = amount;
    }

    public Map<String, Object> getGatewayDetails() {
        return gatewayDetails;
    }

    public void setGatewayDetails(Map<String, Object> gatewayDetails) {
        this.gatewayDetails = gatewayDetails;
    }
}

