package com.pip.gateway;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * Testes unitários para GatewayAdapterFactory
 * 
 * @author Luiz Gustavo Finotello
 */
class GatewayAdapterFactoryTest {

    @Mock
    private GatewayAdapter cieloAdapter;

    @Mock
    private GatewayAdapter redeAdapter;

    @Mock
    private GatewayAdapter pixAdapter;

    private GatewayAdapterFactory factory;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        // Configurar mocks
        when(cieloAdapter.getGatewayCode()).thenReturn("CIELO");
        when(redeAdapter.getGatewayCode()).thenReturn("REDE");
        when(pixAdapter.getGatewayCode()).thenReturn("PIX");

        // Criar factory com adaptadores mockados
        List<GatewayAdapter> adapters = Arrays.asList(cieloAdapter, redeAdapter, pixAdapter);
        factory = new GatewayAdapterFactory(adapters);
    }

    @Test
    void testGetAdapter_Success() {
        // Buscar adaptador existente
        GatewayAdapter adapter = factory.getAdapter("CIELO");
        
        assertNotNull(adapter);
        assertEquals("CIELO", adapter.getGatewayCode());
    }

    @Test
    void testGetAdapter_CaseInsensitive() {
        // Buscar adaptador com código em minúsculas
        GatewayAdapter adapter = factory.getAdapter("cielo");
        
        assertNotNull(adapter);
        assertEquals("CIELO", adapter.getGatewayCode());
    }

    @Test
    void testGetAdapter_NotFound() {
        // Tentar buscar adaptador inexistente
        assertThrows(IllegalArgumentException.class, () -> {
            factory.getAdapter("INEXISTENTE");
        });
    }

    @Test
    void testGetAdapter_NullCode() {
        // Tentar buscar com código nulo
        assertThrows(IllegalArgumentException.class, () -> {
            factory.getAdapter(null);
        });
    }

    @Test
    void testGetAdapter_EmptyCode() {
        // Tentar buscar com código vazio
        assertThrows(IllegalArgumentException.class, () -> {
            factory.getAdapter("");
        });
    }

    @Test
    void testHasAdapter_Exists() {
        // Verificar adaptador existente
        assertTrue(factory.hasAdapter("CIELO"));
        assertTrue(factory.hasAdapter("REDE"));
        assertTrue(factory.hasAdapter("PIX"));
    }

    @Test
    void testHasAdapter_NotExists() {
        // Verificar adaptador inexistente
        assertFalse(factory.hasAdapter("INEXISTENTE"));
    }

    @Test
    void testHasAdapter_NullCode() {
        // Verificar com código nulo
        assertFalse(factory.hasAdapter(null));
    }

    @Test
    void testGetSupportedGateways() {
        // Obter lista de gateways suportados
        List<String> supported = factory.getSupportedGateways();
        
        assertNotNull(supported);
        assertEquals(3, supported.size());
        assertTrue(supported.contains("CIELO"));
        assertTrue(supported.contains("REDE"));
        assertTrue(supported.contains("PIX"));
    }

    @Test
    void testGetAdapterCount() {
        // Verificar contagem de adaptadores
        assertEquals(3, factory.getAdapterCount());
    }
}
