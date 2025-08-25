package com.pip.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

/**
 * DTO para requisição de tokenização
 */
public class TokenizationRequest {
    
    @NotBlank(message = "PAN is required")
    @Pattern(regexp = "^[0-9]{13,19}$", message = "Invalid PAN format")
    private String pan;
    
    @NotBlank(message = "CVV is required")
    @Pattern(regexp = "^[0-9]{3,4}$", message = "Invalid CVV format")
    private String cvv;
    
    @NotBlank(message = "Expiry date is required")
    @Pattern(regexp = "^(0[1-9]|1[0-2])/[0-9]{2}$", message = "Invalid expiry date format (MM/YY)")
    private String expiryDate;
    
    @Size(max = 100, message = "Cardholder name too long")
    private String cardholderName;
    
    @NotBlank(message = "Merchant ID is required")
    private String merchantId;
    
    private boolean testMode = false;
    
    // Constructors
    public TokenizationRequest() {}
    
    public TokenizationRequest(String pan, String cvv, String expiryDate, String cardholderName, String merchantId) {
        this.pan = pan;
        this.cvv = cvv;
        this.expiryDate = expiryDate;
        this.cardholderName = cardholderName;
        this.merchantId = merchantId;
    }
    
    // Getters and Setters
    public String getPan() {
        return pan;
    }
    
    public void setPan(String pan) {
        this.pan = pan;
    }
    
    public String getCvv() {
        return cvv;
    }
    
    public void setCvv(String cvv) {
        this.cvv = cvv;
    }
    
    public String getExpiryDate() {
        return expiryDate;
    }
    
    public void setExpiryDate(String expiryDate) {
        this.expiryDate = expiryDate;
    }
    
    public String getCardholderName() {
        return cardholderName;
    }
    
    public void setCardholderName(String cardholderName) {
        this.cardholderName = cardholderName;
    }
    
    public String getMerchantId() {
        return merchantId;
    }
    
    public void setMerchantId(String merchantId) {
        this.merchantId = merchantId;
    }
    
    public boolean isTestMode() {
        return testMode;
    }
    
    public void setTestMode(boolean testMode) {
        this.testMode = testMode;
    }
}

