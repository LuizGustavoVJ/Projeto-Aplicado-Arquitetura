package com.pip.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/**
 * DTO para requisição de captura de pagamento
 */
public class CaptureRequest {
    
    @NotNull(message = "O valor da captura é obrigatório")
    @DecimalMin(value = "0.01", message = "O valor deve ser maior que zero")
    private BigDecimal amount;
    
    @Size(max = 500, message = "A descrição não pode exceder 500 caracteres")
    private String description;
    
    // Construtor padrão
    public CaptureRequest() {}
    
    // Construtor com parâmetros
    public CaptureRequest(BigDecimal amount, String description) {
        this.amount = amount;
        this.description = description;
    }
    
    // Getters e Setters
    public BigDecimal getAmount() {
        return amount;
    }
    
    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    @Override
    public String toString() {
        return "CaptureRequest{" +
                "amount=" + amount +
                ", description='" + description + '\'' +
                '}';
    }
}

