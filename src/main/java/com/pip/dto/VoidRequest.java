package com.pip.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO para requisição de cancelamento (void) de pagamento
 */
public class VoidRequest {
    
    @NotBlank(message = "O motivo do cancelamento é obrigatório")
    @Size(max = 500, message = "O motivo não pode exceder 500 caracteres")
    private String reason;
    
    @Size(max = 1000, message = "As observações não podem exceder 1000 caracteres")
    private String notes;
    
    // Construtor padrão
    public VoidRequest() {}
    
    // Construtor com parâmetros
    public VoidRequest(String reason, String notes) {
        this.reason = reason;
        this.notes = notes;
    }
    
    // Getters e Setters
    public String getReason() {
        return reason;
    }
    
    public void setReason(String reason) {
        this.reason = reason;
    }
    
    public String getNotes() {
        return notes;
    }
    
    public void setNotes(String notes) {
        this.notes = notes;
    }
    
    @Override
    public String toString() {
        return "VoidRequest{" +
                "reason='" + reason + '\'' +
                ", notes='" + notes + '\'' +
                '}';
    }
}

