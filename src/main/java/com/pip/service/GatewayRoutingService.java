package com.pip.service;

import com.pip.model.*;
import com.pip.repository.GatewayRepository;
import com.pip.repository.LogTransacaoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Serviço responsável pelo roteamento inteligente de gateways
 */
@Service
public class GatewayRoutingService {

    @Autowired
    private GatewayRepository gatewayRepository;

    @Autowired
    private LogTransacaoRepository logTransacaoRepository;

    /**
     * Seleciona o melhor gateway para processar uma transação
     */
    public Gateway selecionarMelhorGateway(Lojista lojista, BigDecimal valor, String metodoPagamento) {
        List<Gateway> gatewaysDisponiveis = gatewayRepository.findByLojistaAndAtivoTrueAndStatusOrderByPrioridadeAsc(
            lojista, "ACTIVE");

        if (gatewaysDisponiveis.isEmpty()) {
            throw new RuntimeException("Nenhum gateway disponível para o lojista");
        }

        // Filtrar por método de pagamento suportado
        gatewaysDisponiveis = gatewaysDisponiveis.stream()
            .filter(g -> suportaMetodoPagamento(g, metodoPagamento))
            .collect(Collectors.toList());

        if (gatewaysDisponiveis.isEmpty()) {
            throw new RuntimeException("Nenhum gateway suporta o método de pagamento: " + metodoPagamento);
        }

        // Filtrar por limites de valor
        gatewaysDisponiveis = gatewaysDisponiveis.stream()
            .filter(g -> valor.compareTo(g.getValorMinimo()) >= 0 && 
                        valor.compareTo(g.getValorMaximo()) <= 0)
            .collect(Collectors.toList());

        if (gatewaysDisponiveis.isEmpty()) {
            throw new RuntimeException("Valor fora dos limites dos gateways disponíveis");
        }

        // Algoritmo de seleção inteligente
        return aplicarAlgoritmoSelecao(gatewaysDisponiveis, valor);
    }

    /**
     * Seleciona gateway para fallback em caso de falha
     */
    public Gateway selecionarGatewayFallback(Lojista lojista, Gateway gatewayFalhou, 
                                           BigDecimal valor, String metodoPagamento) {
        List<Gateway> gatewaysDisponiveis = gatewayRepository.findByLojistaAndAtivoTrueAndStatusOrderByPrioridadeAsc(
            lojista, "ACTIVE");

        // Remover o gateway que falhou
        gatewaysDisponiveis = gatewaysDisponiveis.stream()
            .filter(g -> !g.getId().equals(gatewayFalhou.getId()))
            .filter(g -> suportaMetodoPagamento(g, metodoPagamento))
            .filter(g -> valor.compareTo(g.getValorMinimo()) >= 0 && 
                        valor.compareTo(g.getValorMaximo()) <= 0)
            .collect(Collectors.toList());

        if (gatewaysDisponiveis.isEmpty()) {
            return null; // Sem opções de fallback
        }

        // Para fallback, priorizar estabilidade sobre custo
        return gatewaysDisponiveis.stream()
            .filter(g -> g.getTaxaSucesso() > 95.0) // Alta taxa de sucesso
            .min(Comparator.comparing(Gateway::getTempoMedioResposta))
            .orElse(gatewaysDisponiveis.get(0));
    }

    /**
     * Aplica algoritmo de seleção baseado em múltiplos critérios
     */
    private Gateway aplicarAlgoritmoSelecao(List<Gateway> gateways, BigDecimal valor) {
        // Calcular score para cada gateway
        Map<Gateway, Double> scores = new HashMap<>();

        for (Gateway gateway : gateways) {
            double score = calcularScore(gateway, valor);
            scores.put(gateway, score);
        }

        // Retornar gateway com maior score
        return scores.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse(gateways.get(0));
    }

    /**
     * Calcula score do gateway baseado em múltiplos fatores
     */
    private double calcularScore(Gateway gateway, BigDecimal valor) {
        double score = 0.0;

        // Fator 1: Taxa de sucesso (peso 40%)
        score += gateway.getTaxaSucesso() * 0.4;

        // Fator 2: Custo (peso 30%) - menor custo = maior score
        BigDecimal custoTransacao = calcularCustoTransacao(gateway, valor);
        double custoPorcentual = custoTransacao.divide(valor, 4, BigDecimal.ROUND_HALF_UP)
                                              .multiply(BigDecimal.valueOf(100)).doubleValue();
        score += (10.0 - Math.min(custoPorcentual, 10.0)) * 3.0; // Normalizar para 0-30

        // Fator 3: Tempo de resposta (peso 20%) - menor tempo = maior score
        double tempoScore = Math.max(0, 20.0 - (gateway.getTempoMedioResposta() / 100.0));
        score += Math.min(tempoScore, 20.0);

        // Fator 4: Prioridade configurada (peso 10%)
        score += (10.0 - gateway.getPrioridade()) * 1.0;

        return score;
    }

    /**
     * Calcula custo da transação para um gateway específico
     */
    private BigDecimal calcularCustoTransacao(Gateway gateway, BigDecimal valor) {
        BigDecimal custoFixo = gateway.getTaxaFixa() != null ? gateway.getTaxaFixa() : BigDecimal.ZERO;
        BigDecimal custoPercentual = valor.multiply(gateway.getTaxaPercentual().divide(BigDecimal.valueOf(100)));
        return custoFixo.add(custoPercentual);
    }

    /**
     * Verifica se gateway suporta método de pagamento
     */
    private boolean suportaMetodoPagamento(Gateway gateway, String metodoPagamento) {
        if (gateway.getMetodosPagamento() == null) {
            return true; // Se não especificado, assume que suporta todos
        }
        
        // Assumindo que métodos são armazenados como JSON array string
        return gateway.getMetodosPagamento().toLowerCase().contains(metodoPagamento.toLowerCase());
    }

    /**
     * Verifica saúde dos gateways e atualiza status
     */
    public void verificarSaudeGateways() {
        List<Gateway> gateways = gatewayRepository.findAll();
        
        for (Gateway gateway : gateways) {
            boolean saudavel = avaliarSaudeGateway(gateway);
            
            if (!saudavel && gateway.getStatus().equals("ACTIVE")) {
                gateway.marcarIndisponivel("Health check failed");
                gatewayRepository.save(gateway);
            } else if (saudavel && gateway.getStatus().equals("INACTIVE")) {
                gateway.marcarDisponivel();
                gatewayRepository.save(gateway);
            }
        }
    }

    /**
     * Avalia saúde de um gateway específico
     */
    private boolean avaliarSaudeGateway(Gateway gateway) {
        ZonedDateTime ultimaHora = ZonedDateTime.now().minusHours(1);
        
        // Critérios de saúde:
        // 1. Taxa de sucesso > 90% na última hora
        // 2. Tempo médio de resposta < 5 segundos
        // 3. Sem falhas críticas nos últimos 15 minutos
        
        return gateway.getTaxaSucesso() > 90.0 && 
               gateway.getTempoMedioResposta() < 5000 &&
               !temFalhasCriticasRecentes(gateway, ZonedDateTime.now().minusMinutes(15));
    }

    /**
     * Verifica se há falhas críticas recentes
     */
    private boolean temFalhasCriticasRecentes(Gateway gateway, ZonedDateTime desde) {
        // Implementação simplificada - em produção consultar logs específicos
        return false;
    }

    /**
     * Obtém estatísticas de roteamento
     */
    public Map<String, Object> obterEstatisticasRoteamento(Lojista lojista, ZonedDateTime inicio, ZonedDateTime fim) {
        Map<String, Object> stats = new HashMap<>();
        
        List<Gateway> gateways = gatewayRepository.findByLojista(lojista);
        
        for (Gateway gateway : gateways) {
            Map<String, Object> gatewayStats = new HashMap<>();
            gatewayStats.put("totalTransacoes", gateway.getTotalTransacoes());
            gatewayStats.put("taxaSucesso", gateway.getTaxaSucesso());
            gatewayStats.put("tempoMedioResposta", gateway.getTempoMedioResposta());
            gatewayStats.put("status", gateway.getStatus());
            
            stats.put(gateway.getNome(), gatewayStats);
        }
        
        return stats;
    }

    /**
     * Força rebalanceamento de carga entre gateways
     */
    public void rebalancearGateways(Lojista lojista) {
        List<Gateway> gateways = gatewayRepository.findByLojistaAndAtivoTrue(lojista);
        
        // Algoritmo simples de rebalanceamento baseado em carga atual
        for (Gateway gateway : gateways) {
            // Ajustar prioridade baseado na performance recente
            if (gateway.getTaxaSucesso() > 98.0 && gateway.getTempoMedioResposta() < 1000) {
                // Gateway performático - aumentar prioridade
                gateway.setPrioridade(Math.max(1, gateway.getPrioridade() - 1));
            } else if (gateway.getTaxaSucesso() < 95.0 || gateway.getTempoMedioResposta() > 3000) {
                // Gateway com problemas - diminuir prioridade
                gateway.setPrioridade(Math.min(10, gateway.getPrioridade() + 1));
            }
            
            gatewayRepository.save(gateway);
        }
    }

    /**
     * Simula processamento para teste de carga
     */
    public Gateway selecionarGatewayParaTeste(Lojista lojista) {
        List<Gateway> gateways = gatewayRepository.findByLojistaAndAtivoTrueAndStatus(lojista, "ACTIVE");
        
        if (gateways.isEmpty()) {
            return null;
        }
        
        // Para testes, usar round-robin simples
        Random random = new Random();
        return gateways.get(random.nextInt(gateways.size()));
    }
}

