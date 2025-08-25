package com.pip.config;

import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.security.keyvault.secrets.SecretClient;
import com.azure.security.keyvault.secrets.SecretClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuração do Azure Key Vault para tokenização segura
 * Implementa conformidade PCI DSS para armazenamento de dados sensíveis
 */
@Configuration
public class AzureKeyVaultConfig {
    
    @Value("${azure.keyvault.uri:https://pip-keyvault.vault.azure.net/}")
    private String keyVaultUri;
    
    @Bean
    public SecretClient secretClient() {
        return new SecretClientBuilder()
            .vaultUrl(keyVaultUri)
            .credential(new DefaultAzureCredentialBuilder().build())
            .buildClient();
    }
}

