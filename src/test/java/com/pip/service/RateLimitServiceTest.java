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
        
        rateLimitService = new RateLimitService();
        // Inject mocked RedisTemplate using reflection
        try {
            java.lang.reflect.Field field = RateLimitService.class.getDeclaredField("redisTemplate");
            field.setAccessible(true);
            field.set(rateLimitService, redisTemplate);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void testIsAllowed_FirstRequest() {
        // Configurar mock - primeira requisição retorna 1
        when(valueOperations.increment(anyString())).thenReturn(1L);

        // Verificar se primeira requisição é permitida
        assertTrue(rateLimitService.isAllowed("test-api-key", 100));

        // Verificar se Redis foi chamado
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
        lojista.setPlano(PlanoLojista.STARTER);

        // Verificar se requisição é permitida
        assertTrue(rateLimitService.isAllowed("test-api-key", 100));
    }

    @Test
    void testIsAllowed_ExceedsLimit() {
        // Configurar mock - 101 requisições (excedeu o limite de 100)
        when(valueOperations.increment(anyString())).thenReturn(101L);

        // Verificar se requisição é bloqueada
        assertFalse(rateLimitService.isAllowed("test-api-key", 100));

        // Verificar que increment foi chamado
        verify(valueOperations).increment(anyString());
    }

    @Test
    void testGetRemainingRequests() {
        // Configurar mock - 30 requisições de 100
        when(valueOperations.get(anyString())).thenReturn(30L);

        // Criar lojista com plano FREE (100 req/min)
        Lojista lojista = new Lojista();
        lojista.setPlano(PlanoLojista.STARTER);

        // Verificar requisições restantes
        long remaining = rateLimitService.getRemainingRequests("test-api-key", 100);
        assertEquals(70L, remaining);
    }

    @Test
    void testGetRemainingRequests_NoRequests() {
        // Configurar mock - nenhuma requisição ainda
        when(valueOperations.get(anyString())).thenReturn(null);

        // Criar lojista com plano FREE (100 req/min)
        Lojista lojista = new Lojista();
        lojista.setPlano(PlanoLojista.STARTER);

        // Verificar requisições restantes
        long remaining = rateLimitService.getRemainingRequests("test-api-key", 100);
        assertEquals(100L, remaining);
    }

    @Test
    void testGetResetTime() {
        // Configurar mock - 30 segundos restantes
        when(redisTemplate.getExpire(anyString(), eq(TimeUnit.SECONDS))).thenReturn(30L);

        // Criar lojista
        Lojista lojista = new Lojista();
        lojista.setPlano(PlanoLojista.STARTER);

        // Verificar tempo de reset
        long resetTime = rateLimitService.getResetTime("test-api-key");
        assertEquals(30L, resetTime);
    }

    @Test
    void testGetResetTime_NoExpiration() {
        // Configurar mock para retornar 60 segundos de TTL
        when(redisTemplate.getExpire(anyString(), eq(TimeUnit.SECONDS))).thenReturn(60L);

        // Verificar tempo de reset (deve retornar 60 segundos)
        long resetTime = rateLimitService.getResetTime("test-api-key");
        assertEquals(60L, resetTime);
    }

    @Test
    void testDifferentPlans() {
        // Configurar mock
        when(valueOperations.get(anyString())).thenReturn(null);
        when(valueOperations.increment(anyString())).thenReturn(1L);

        // Testar plano STARTER (500 req/min)
        assertTrue(rateLimitService.isAllowed("test-api-key-starter", 500));

        // Testar plano BUSINESS (2000 req/min)
        assertTrue(rateLimitService.isAllowed("test-api-key-business", 2000));

        // Testar plano ENTERPRISE (10000 req/min)
        assertTrue(rateLimitService.isAllowed("test-api-key-enterprise", 10000));
    }
}
