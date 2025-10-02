package com.pip.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * Configuração do RestTemplate para comunicação com gateways
 * 
 * Define timeouts e configurações de conexão para garantir resiliência
 * 
 * @author Luiz Gustavo Finotello
 */
@Configuration
public class RestTemplateConfig {

    /**
     * Bean do RestTemplate com configurações de timeout
     */
    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
            .setConnectTimeout(Duration.ofSeconds(10))  // Timeout de conexão
            .setReadTimeout(Duration.ofSeconds(30))     // Timeout de leitura
            .requestFactory(this::clientHttpRequestFactory)
            .build();
    }

    /**
     * Factory para configurar detalhes da requisição HTTP
     */
    private ClientHttpRequestFactory clientHttpRequestFactory() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10000);  // 10 segundos
        factory.setReadTimeout(30000);     // 30 segundos
        return factory;
    }
}
