package com.pip.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.NotBlank;

/**
 * DTO para requisição de autorização de pagamento
 * 
 * @author Luiz Gustavo Finotello
 */
public class AuthorizationRequest {

    @NotNull(message = "O valor é obrigatório")
    @Positive(message = "O valor deve ser positivo")
    private Long amount;

    @NotBlank(message = "A moeda é obrigatória")
    private String currency;

    @NotBlank(message = "O token do cartão é obrigatório")
    private String cardToken;

    private Boolean capture = true;

    // Construtores
    public AuthorizationRequest() {}

    public AuthorizationRequest(Long amount, String currency, String cardToken, Boolean capture) {
        this.amount = amount;
        this.currency = currency;
        this.cardToken = cardToken;
        this.capture = capture;
    }

    // Getters e Setters
    public Long getAmount() {
        return amount;
    }

    public void setAmount(Long amount) {
        this.amount = amount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getCardToken() {
        return cardToken;
    }

    public void setCardToken(String cardToken) {
        this.cardToken = cardToken;
    }

    public Boolean getCapture() {
        return capture;
    }

    public void setCapture(Boolean capture) {
        this.capture = capture;
    }
}

