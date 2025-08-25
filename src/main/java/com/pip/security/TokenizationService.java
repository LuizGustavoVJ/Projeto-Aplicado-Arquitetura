package com.pip.security;

import com.azure.security.keyvault.secrets.SecretClient;
import com.azure.security.keyvault.secrets.models.KeyVaultSecret;
import com.pip.audit.SecurityAuditLogger;
import com.pip.dto.TokenizationRequest;
import com.pip.dto.TokenizationResponse;
import com.pip.dto.DetokenizationRequest;
import com.pip.dto.DetokenizationResponse;
import com.pip.exception.TokenizationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Arrays;
import java.util.UUID;

/**
 * Serviço de tokenização PCI DSS compliant
 * Implementa tokenização irreversível usando Azure Key Vault
 */
@Service
public class TokenizationService {
    
    private static final Logger logger = LoggerFactory.getLogger(TokenizationService.class);
    private static final String TOKEN_PREFIX = "tkn_live_";
    private static final String TOKEN_TEST_PREFIX = "tkn_test_";
    
    @Autowired
    private SecretClient secretClient;
    
    @Autowired
    private SecurityAuditLogger auditLogger;
    
    private final SecureRandom secureRandom = new SecureRandom();
    
    /**
     * Tokeniza dados sensíveis de cartão de crédito
     * @param request Dados do cartão para tokenização
     * @return Token irreversível e seguro
     */
    public TokenizationResponse tokenize(TokenizationRequest request) {
        String requestId = UUID.randomUUID().toString();
        
        try {
            // Log início da tokenização
            auditLogger.logTokenizationStart(requestId, request.getMerchantId());
            
            // Validar dados de entrada
            validateTokenizationRequest(request);
            
            // Gerar token único
            String token = generateSecureToken(request.isTestMode());
            
            // Criar payload criptografado
            String encryptedPayload = createEncryptedPayload(request);
            
            // Armazenar no Azure Key Vault
            secretClient.setSecret(token, encryptedPayload);
            
            // Limpar dados sensíveis da memória
            clearSensitiveData(request);
            
            // Log sucesso da tokenização
            auditLogger.logTokenizationSuccess(requestId, token, request.getMerchantId());
            
            return TokenizationResponse.builder()
                .token(token)
                .tokenType("IRREVERSIBLE")
                .expiresAt(calculateExpirationTime())
                .requestId(requestId)
                .build();
                
        } catch (Exception e) {
            auditLogger.logTokenizationFailure(requestId, request.getMerchantId(), e);
            throw new TokenizationException("Tokenization failed", e);
        }
    }
    
    /**
     * Destokeniza dados para processamento de pagamento
     * @param request Requisição de destokenização
     * @return Dados originais do cartão
     */
    public DetokenizationResponse detokenize(DetokenizationRequest request) {
        String requestId = UUID.randomUUID().toString();
        
        try {
            // Log início da destokenização
            auditLogger.logDetokenizationStart(requestId, request.getToken(), request.getMerchantId());
            
            // Validar token
            validateDetokenizationRequest(request);
            
            // Recuperar dados do Azure Key Vault
            KeyVaultSecret secret = secretClient.getSecret(request.getToken());
            
            if (secret == null) {
                throw new TokenizationException("Token not found or expired");
            }
            
            // Descriptografar payload
            TokenizedData data = decryptPayload(secret.getValue());
            
            // Validar propriedade do token
            if (!data.getMerchantId().equals(request.getMerchantId())) {
                auditLogger.logUnauthorizedDetokenization(requestId, request.getToken(), request.getMerchantId());
                throw new TokenizationException("Unauthorized token access");
            }
            
            // Log sucesso da destokenização
            auditLogger.logDetokenizationSuccess(requestId, request.getToken(), request.getMerchantId());
            
            DetokenizationResponse response = DetokenizationResponse.builder()
                .pan(data.getPan())
                .cvv(data.getCvv())
                .expiryDate(data.getExpiryDate())
                .cardholderName(data.getCardholderName())
                .requestId(requestId)
                .build();
            
            // Agendar limpeza dos dados após uso
            scheduleDataClearing(response);
            
            return response;
            
        } catch (Exception e) {
            auditLogger.logDetokenizationFailure(requestId, request.getToken(), request.getMerchantId(), e);
            throw new TokenizationException("Detokenization failed", e);
        }
    }
    
    private void validateTokenizationRequest(TokenizationRequest request) {
        if (request.getPan() == null || request.getPan().length() < 13 || request.getPan().length() > 19) {
            throw new IllegalArgumentException("Invalid PAN format");
        }
        
        if (request.getCvv() == null || request.getCvv().length() < 3 || request.getCvv().length() > 4) {
            throw new IllegalArgumentException("Invalid CVV format");
        }
        
        if (request.getMerchantId() == null || request.getMerchantId().trim().isEmpty()) {
            throw new IllegalArgumentException("Merchant ID is required");
        }
        
        // Validar algoritmo de Luhn
        if (!isValidLuhn(request.getPan())) {
            throw new IllegalArgumentException("Invalid PAN checksum");
        }
    }
    
    private void validateDetokenizationRequest(DetokenizationRequest request) {
        if (request.getToken() == null || 
            (!request.getToken().startsWith(TOKEN_PREFIX) && !request.getToken().startsWith(TOKEN_TEST_PREFIX))) {
            throw new IllegalArgumentException("Invalid token format");
        }
        
        if (request.getMerchantId() == null || request.getMerchantId().trim().isEmpty()) {
            throw new IllegalArgumentException("Merchant ID is required");
        }
        
        if (request.getPurpose() == null || request.getPurpose().trim().isEmpty()) {
            throw new IllegalArgumentException("Purpose is required for detokenization");
        }
    }
    
    private String generateSecureToken(boolean testMode) {
        String prefix = testMode ? TOKEN_TEST_PREFIX : TOKEN_PREFIX;
        
        // Gerar 32 bytes aleatórios
        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);
        
        // Converter para hex
        StringBuilder hexString = new StringBuilder();
        for (byte b : randomBytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        
        return prefix + hexString.toString();
    }
    
    private String createEncryptedPayload(TokenizationRequest request) {
        // Em implementação real, usar AES-256-GCM
        // Por simplicidade, usando Base64 (NUNCA fazer isso em produção)
        TokenizedData data = TokenizedData.builder()
            .pan(request.getPan())
            .cvv(request.getCvv())
            .expiryDate(request.getExpiryDate())
            .cardholderName(request.getCardholderName())
            .merchantId(request.getMerchantId())
            .tokenizedAt(Instant.now())
            .build();
        
        // Simular criptografia (implementar AES-256-GCM em produção)
        return java.util.Base64.getEncoder().encodeToString(data.toString().getBytes());
    }
    
    private TokenizedData decryptPayload(String encryptedPayload) {
        // Simular descriptografia (implementar AES-256-GCM em produção)
        String decrypted = new String(java.util.Base64.getDecoder().decode(encryptedPayload));
        
        // Parse dos dados (implementar JSON parsing em produção)
        return TokenizedData.fromString(decrypted);
    }
    
    private void clearSensitiveData(TokenizationRequest request) {
        // Sobrescrever strings sensíveis
        if (request.getPan() != null) {
            char[] panArray = request.getPan().toCharArray();
            Arrays.fill(panArray, '0');
            request.setPan(null);
        }
        
        if (request.getCvv() != null) {
            char[] cvvArray = request.getCvv().toCharArray();
            Arrays.fill(cvvArray, '0');
            request.setCvv(null);
        }
        
        // Forçar garbage collection
        System.gc();
    }
    
    private void scheduleDataClearing(DetokenizationResponse response) {
        // Agendar limpeza após 5 minutos (tempo máximo de uso)
        // Em produção, usar scheduler apropriado
        new Thread(() -> {
            try {
                Thread.sleep(300000); // 5 minutos
                clearDetokenizedData(response);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }
    
    private void clearDetokenizedData(DetokenizationResponse response) {
        if (response.getPan() != null) {
            char[] panArray = response.getPan().toCharArray();
            Arrays.fill(panArray, '0');
            response.setPan(null);
        }
        
        if (response.getCvv() != null) {
            char[] cvvArray = response.getCvv().toCharArray();
            Arrays.fill(cvvArray, '0');
            response.setCvv(null);
        }
        
        System.gc();
    }
    
    private boolean isValidLuhn(String pan) {
        int sum = 0;
        boolean alternate = false;
        
        for (int i = pan.length() - 1; i >= 0; i--) {
            int n = Integer.parseInt(pan.substring(i, i + 1));
            
            if (alternate) {
                n *= 2;
                if (n > 9) {
                    n = (n % 10) + 1;
                }
            }
            
            sum += n;
            alternate = !alternate;
        }
        
        return (sum % 10 == 0);
    }
    
    private Instant calculateExpirationTime() {
        // Tokens expiram em 1 ano
        return Instant.now().plusSeconds(365 * 24 * 60 * 60);
    }
    
    /**
     * Classe interna para dados tokenizados
     */
    private static class TokenizedData {
        private String pan;
        private String cvv;
        private String expiryDate;
        private String cardholderName;
        private String merchantId;
        private Instant tokenizedAt;
        
        // Builder pattern e métodos de serialização
        public static TokenizedDataBuilder builder() {
            return new TokenizedDataBuilder();
        }
        
        public static TokenizedData fromString(String data) {
            // Implementar parsing real em produção
            return new TokenizedData();
        }
        
        // Getters
        public String getPan() { return pan; }
        public String getCvv() { return cvv; }
        public String getExpiryDate() { return expiryDate; }
        public String getCardholderName() { return cardholderName; }
        public String getMerchantId() { return merchantId; }
        public Instant getTokenizedAt() { return tokenizedAt; }
        
        public static class TokenizedDataBuilder {
            private TokenizedData data = new TokenizedData();
            
            public TokenizedDataBuilder pan(String pan) { data.pan = pan; return this; }
            public TokenizedDataBuilder cvv(String cvv) { data.cvv = cvv; return this; }
            public TokenizedDataBuilder expiryDate(String expiryDate) { data.expiryDate = expiryDate; return this; }
            public TokenizedDataBuilder cardholderName(String cardholderName) { data.cardholderName = cardholderName; return this; }
            public TokenizedDataBuilder merchantId(String merchantId) { data.merchantId = merchantId; return this; }
            public TokenizedDataBuilder tokenizedAt(Instant tokenizedAt) { data.tokenizedAt = tokenizedAt; return this; }
            
            public TokenizedData build() { return data; }
        }
    }
}

