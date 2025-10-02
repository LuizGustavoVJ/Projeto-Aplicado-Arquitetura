package com.pip.service;

import com.pip.model.Gateway;
import com.pip.model.Lojista;
import com.pip.repository.GatewayRepository;
import com.pip.repository.LogTransacaoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Testes unitários para GatewayRoutingService
 * 
 * @author Luiz Gustavo Finotello
 */
@ExtendWith(MockitoExtension.class)
class GatewayRoutingServiceTest {

    @Mock
    private GatewayRepository gatewayRepository;

    @Mock
    private LogTransacaoRepository logTransacaoRepository;

    @InjectMocks
    private GatewayRoutingService gatewayRoutingService;

    private Lojista lojista;
    private Gateway gateway1;
    private Gateway gateway2;

    @BeforeEach
    void setUp() {
        // Criar lojista de teste
        lojista = new Lojista();
        lojista.setNomeFantasia("Loja Teste");
        lojista.setRazaoSocial("Loja Teste LTDA");
        lojista.setCnpj("12345678901234");
        lojista.setEmail("teste@loja.com");
        lojista.setPlano(com.pip.model.PlanoLojista.BUSINESS);

        // Criar gateways de teste
        gateway1 = new Gateway();
        gateway1.setCodigo("GATEWAY1");
        gateway1.setNome("Gateway 1");
        gateway1.setTipo(com.pip.model.TipoGateway.ACQUIRER);
        gateway1.setUrlBase("http://gateway1.com");
        gateway1.setPrioridade(1);
        gateway1.setPesoRoteamento(50);
        gateway1.setTaxaSucesso(95.0);
        gateway1.setTempoRespostaMedio(1000L);
        gateway1.setLimiteDiario(10000000L);
        gateway1.setVolumeProcessadoHoje(0L);
        gateway1.ativar();

        gateway2 = new Gateway();
        gateway2.setCodigo("GATEWAY2");
        gateway2.setNome("Gateway 2");
        gateway2.setTipo(com.pip.model.TipoGateway.SUBACQUIRER);
        gateway2.setUrlBase("http://gateway2.com");
        gateway2.setPrioridade(2);
        gateway2.setPesoRoteamento(30);
        gateway2.setTaxaSucesso(90.0);
        gateway2.setTempoRespostaMedio(2000L);
        gateway2.setLimiteDiario(5000000L);
        gateway2.setVolumeProcessadoHoje(0L);
        gateway2.ativar();
    }

    @Test
    void testSelecionarMelhorGateway_ComGatewaysDisponiveis() {
        // Arrange
        List<Gateway> gateways = Arrays.asList(gateway1, gateway2);
        when(gatewayRepository.findAll()).thenReturn(gateways);

        // Act
        Gateway selecionado = gatewayRoutingService.selecionarMelhorGateway(lojista, 100000L);

        // Assert
        assertNotNull(selecionado);
        assertEquals("GATEWAY1", selecionado.getCodigo());
        verify(gatewayRepository, times(1)).findAll();
    }

    @Test
    void testSelecionarMelhorGateway_SemGatewaysDisponiveis() {
        // Arrange
        when(gatewayRepository.findAll()).thenReturn(Arrays.asList());

        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            gatewayRoutingService.selecionarMelhorGateway(lojista, 100000L);
        });
    }

    @Test
    void testSelecionarGatewayFallback() {
        // Arrange
        List<Gateway> gateways = Arrays.asList(gateway1, gateway2);
        when(gatewayRepository.findAll()).thenReturn(gateways);

        // Act
        Gateway fallback = gatewayRoutingService.selecionarGatewayFallback(lojista, gateway1, 100000L);

        // Assert
        assertNotNull(fallback);
        assertEquals("GATEWAY2", fallback.getCodigo());
    }

    @Test
    void testVerificarSaudeGateways() {
        // Arrange
        List<Gateway> gateways = Arrays.asList(gateway1, gateway2);
        when(gatewayRepository.findAll()).thenReturn(gateways);

        // Act
        gatewayRoutingService.verificarSaudeGateways();

        // Assert
        verify(gatewayRepository, times(1)).findAll();
        verify(gatewayRepository, atLeast(2)).save(any(Gateway.class));
    }

    @Test
    void testObterEstatisticasRoteamento() {
        // Arrange
        List<Gateway> gateways = Arrays.asList(gateway1, gateway2);
        when(gatewayRepository.findAll()).thenReturn(gateways);

        // Act
        var stats = gatewayRoutingService.obterEstatisticasRoteamento();

        // Assert
        assertNotNull(stats);
        assertTrue(stats.containsKey("GATEWAY1"));
        assertTrue(stats.containsKey("GATEWAY2"));
    }

    @Test
    void testRebalancearGateways() {
        // Arrange
        List<Gateway> gateways = Arrays.asList(gateway1, gateway2);
        when(gatewayRepository.findAll()).thenReturn(gateways);

        // Act
        gatewayRoutingService.rebalancearGateways();

        // Assert
        verify(gatewayRepository, times(1)).findAll();
    }
}
