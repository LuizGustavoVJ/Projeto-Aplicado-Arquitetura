package com.pip.service;

import com.pip.model.Lojista;
import com.pip.model.PlanoLojista;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Testes unitários para RateLimitService
 * 
 * @author Luiz Gustavo Finotello
 */
class RateLimitServiceTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    private RateLimitService rateLimitService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        
        rateLimitService = new RateLimitService(redisTemplate);
    }

    @Test
    void testIsAllowed_FirstRequest() {
        // Configurar mock
        when(valueOperations.get(anyString())).thenReturn(null);
        when(valueOperations.increment(anyString())).thenReturn(1L);

        // Criar lojista com plano FREE (100 req/min)
        Lojista lojista = new Lojista();
        lojista.setPlano(PlanoLojista.FREE);

        // Verificar se primeira requisição é permitida
        assertTrue(rateLimitService.isAllowed(lojista));

        // Verificar se Redis foi chamado
        verify(valueOperations).get(anyString());
        verify(valueOperations).increment(anyString());
        verify(redisTemplate).expire(anyString(), eq(60L), eq(TimeUnit.SECONDS));
    }

    @Test
    void testIsAllowed_WithinLimit() {
        // Configurar mock - 50 requisições de 100 permitidas
        when(valueOperations.get(anyString())).thenReturn(50L);
        when(valueOperations.increment(anyString())).thenReturn(51L);

        // Criar lojista com plano FREE (100 req/min)
        Lojista lojista = new Lojista();
        lojista.setPlano(PlanoLojista.FREE);

        // Verificar se requisição é permitida
        assertTrue(rateLimitService.isAllowed(lojista));
    }

    @Test
    void testIsAllowed_ExceedsLimit() {
        // Configurar mock - 100 requisições (limite atingido)
        when(valueOperations.get(anyString())).thenReturn(100L);

        // Criar lojista com plano FREE (100 req/min)
        Lojista lojista = new Lojista();
        lojista.setPlano(PlanoLojista.FREE);

        // Verificar se requisição é bloqueada
        assertFalse(rateLimitService.isAllowed(lojista));

        // Verificar que increment não foi chamado
        verify(valueOperations, never()).increment(anyString());
    }

    @Test
    void testGetRemainingRequests() {
        // Configurar mock - 30 requisições de 100
        when(valueOperations.get(anyString())).thenReturn(30L);

        // Criar lojista com plano FREE (100 req/min)
        Lojista lojista = new Lojista();
        lojista.setPlano(PlanoLojista.FREE);

        // Verificar requisições restantes
        long remaining = rateLimitService.getRemainingRequests(lojista);
        assertEquals(70L, remaining);
    }

    @Test
    void testGetRemainingRequests_NoRequests() {
        // Configurar mock - nenhuma requisição ainda
        when(valueOperations.get(anyString())).thenReturn(null);

        // Criar lojista com plano FREE (100 req/min)
        Lojista lojista = new Lojista();
        lojista.setPlano(PlanoLojista.FREE);

        // Verificar requisições restantes
        long remaining = rateLimitService.getRemainingRequests(lojista);
        assertEquals(100L, remaining);
    }

    @Test
    void testGetResetTime() {
        // Configurar mock - 30 segundos restantes
        when(redisTemplate.getExpire(anyString(), eq(TimeUnit.SECONDS))).thenReturn(30L);

        // Criar lojista
        Lojista lojista = new Lojista();
        lojista.setPlano(PlanoLojista.FREE);

        // Verificar tempo de reset
        long resetTime = rateLimitService.getResetTime(lojista);
        assertEquals(30L, resetTime);
    }

    @Test
    void testGetResetTime_NoExpiration() {
        // Configurar mock - sem expiração definida
        when(redisTemplate.getExpire(anyString(), eq(TimeUnit.SECONDS))).thenReturn(-1L);

        // Criar lojista
        Lojista lojista = new Lojista();
        lojista.setPlano(PlanoLojista.FREE);

        // Verificar tempo de reset (deve retornar 60 segundos por padrão)
        long resetTime = rateLimitService.getResetTime(lojista);
        assertEquals(60L, resetTime);
    }

    @Test
    void testDifferentPlans() {
        // Configurar mock
        when(valueOperations.get(anyString())).thenReturn(null);
        when(valueOperations.increment(anyString())).thenReturn(1L);

        // Testar plano STARTER (500 req/min)
        Lojista lojistaStarter = new Lojista();
        lojistaStarter.setPlano(PlanoLojista.STARTER);
        assertTrue(rateLimitService.isAllowed(lojistaStarter));

        // Testar plano BUSINESS (2000 req/min)
        Lojista lojistaBusiness = new Lojista();
        lojistaBusiness.setPlano(PlanoLojista.BUSINESS);
        assertTrue(rateLimitService.isAllowed(lojistaBusiness));

        // Testar plano ENTERPRISE (10000 req/min)
        Lojista lojistaEnterprise = new Lojista();
        lojistaEnterprise.setPlano(PlanoLojista.ENTERPRISE);
        assertTrue(rateLimitService.isAllowed(lojistaEnterprise));
    }
}
