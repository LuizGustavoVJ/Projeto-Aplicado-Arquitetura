package com.pip.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Serviço de Rate Limiting usando Redis
 * 
 * Implementa controle de taxa de requisições por API Key usando algoritmo de janela deslizante.
 * 
 * Limites padrão por plano:
 * - FREE: 100 requisições/minuto
 * - STARTER: 500 requisições/minuto
 * - BUSINESS: 2000 requisições/minuto
 * - ENTERPRISE: 10000 requisições/minuto
 * 
 * @author Luiz Gustavo Finotello
 */
@Service
public class RateLimitService {

    private static final Logger logger = LoggerFactory.getLogger(RateLimitService.class);
    
    private static final String RATE_LIMIT_KEY_PREFIX = "rate_limit:";
    private static final long WINDOW_SIZE_SECONDS = 60; // 1 minuto

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * Verifica se a requisição está dentro do limite de taxa
     * 
     * @param apiKey Chave da API
     * @param maxRequests Número máximo de requisições permitidas
     * @return true se dentro do limite, false se excedeu
     */
    public boolean isAllowed(String apiKey, int maxRequests) {
        String key = RATE_LIMIT_KEY_PREFIX + apiKey;
        
        try {
            // Incrementar contador
            Long currentCount = redisTemplate.opsForValue().increment(key);
            
            if (currentCount == null) {
                logger.warn("Falha ao incrementar contador para API Key: {}", apiKey);
                return true; // Em caso de erro, permitir requisição
            }
            
            // Se é a primeira requisição, definir expiração
            if (currentCount == 1) {
                redisTemplate.expire(key, WINDOW_SIZE_SECONDS, TimeUnit.SECONDS);
            }
            
            // Verificar se excedeu o limite
            boolean allowed = currentCount <= maxRequests;
            
            if (!allowed) {
                logger.warn("Rate limit excedido para API Key: {}. Requisições: {}/{}", 
                    apiKey, currentCount, maxRequests);
            }
            
            return allowed;
            
        } catch (Exception e) {
            logger.error("Erro ao verificar rate limit para API Key {}: {}", apiKey, e.getMessage());
            return true; // Em caso de erro, permitir requisição
        }
    }

    /**
     * Obtém o número de requisições restantes
     * 
     * @param apiKey Chave da API
     * @param maxRequests Número máximo de requisições permitidas
     * @return Número de requisições restantes
     */
    public long getRemainingRequests(String apiKey, int maxRequests) {
        String key = RATE_LIMIT_KEY_PREFIX + apiKey;
        
        try {
            Object value = redisTemplate.opsForValue().get(key);
            
            if (value == null) {
                return maxRequests;
            }
            
            long currentCount = Long.parseLong(value.toString());
            long remaining = maxRequests - currentCount;
            
            return Math.max(0, remaining);
            
        } catch (Exception e) {
            logger.error("Erro ao obter requisições restantes para API Key {}: {}", apiKey, e.getMessage());
            return maxRequests;
        }
    }

    /**
     * Obtém o tempo até o reset do limite (em segundos)
     * 
     * @param apiKey Chave da API
     * @return Tempo em segundos até o reset
     */
    public long getResetTime(String apiKey) {
        String key = RATE_LIMIT_KEY_PREFIX + apiKey;
        
        try {
            Long ttl = redisTemplate.getExpire(key, TimeUnit.SECONDS);
            
            if (ttl == null || ttl < 0) {
                return 0;
            }
            
            return ttl;
            
        } catch (Exception e) {
            logger.error("Erro ao obter tempo de reset para API Key {}: {}", apiKey, e.getMessage());
            return 0;
        }
    }

    /**
     * Reseta o contador de rate limit para uma API Key
     * 
     * @param apiKey Chave da API
     */
    public void reset(String apiKey) {
        String key = RATE_LIMIT_KEY_PREFIX + apiKey;
        
        try {
            redisTemplate.delete(key);
            logger.info("Rate limit resetado para API Key: {}", apiKey);
            
        } catch (Exception e) {
            logger.error("Erro ao resetar rate limit para API Key {}: {}", apiKey, e.getMessage());
        }
    }

    /**
     * Obtém o limite de requisições baseado no plano do lojista
     * 
     * @param plano Plano do lojista
     * @return Número máximo de requisições por minuto
     */
    public int getLimitByPlan(String plano) {
        return switch (plano.toUpperCase()) {
            case "FREE" -> 100;
            case "STARTER" -> 500;
            case "BUSINESS" -> 2000;
            case "ENTERPRISE" -> 10000;
            default -> 100; // Padrão: FREE
        };
    }
}
