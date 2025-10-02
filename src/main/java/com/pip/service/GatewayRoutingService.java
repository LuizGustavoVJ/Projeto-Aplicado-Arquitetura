package com.pip.service;

import com.pip.model.Gateway;
import com.pip.model.Lojista;
import com.pip.model.LogTransacao;
import com.pip.repository.GatewayRepository;
import com.pip.repository.LogTransacaoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Serviço responsável pelo roteamento inteligente de transações entre gateways
 * 
 * Implementa algoritmo de seleção baseado em múltiplos critérios:
 * - Prioridade configurada
 * - Taxa de sucesso histórica
 * - Tempo médio de resposta
 * - Status de saúde (health check)
 * - Limites de processamento
 * 
 * @author Luiz Gustavo Finotello
 */
@Service
public class GatewayRoutingService {

    private static final Logger logger = LoggerFactory.getLogger(GatewayRoutingService.class);

    @Autowired
    private GatewayRepository gatewayRepository;

    @Autowired
    private LogTransacaoRepository logTransacaoRepository;

    /**
     * Seleciona o melhor gateway para processar uma transação
     * 
     * @param lojista Lojista que está processando a transação
     * @param valor Valor da transação em centavos
     * @return Gateway selecionado
     * @throws RuntimeException se nenhum gateway disponível
     */
    public Gateway selecionarMelhorGateway(Lojista lojista, Long valor) {
        logger.debug("Selecionando gateway para lojista {} e valor {}", lojista.getId(), valor);

        // Buscar todos os gateways ativos
        List<Gateway> gatewaysDisponiveis = buscarGatewaysDisponiveis();

        if (gatewaysDisponiveis.isEmpty()) {
            logger.error("Nenhum gateway disponível no sistema");
            throw new RuntimeException("Nenhum gateway disponível para processar a transação");
        }

        // Filtrar gateways que podem processar o valor
        gatewaysDisponiveis = filtrarPorLimites(gatewaysDisponiveis, valor);

        if (gatewaysDisponiveis.isEmpty()) {
            logger.error("Nenhum gateway com capacidade para processar valor {}", valor);
            throw new RuntimeException("Nenhum gateway com capacidade disponível para o valor solicitado");
        }

        // Aplicar algoritmo de seleção inteligente
        Gateway gatewaySelecionado = aplicarAlgoritmoSelecao(gatewaysDisponiveis, valor);

        logger.info("Gateway selecionado: {} (score: {})", 
            gatewaySelecionado.getCodigo(), 
            calcularScore(gatewaySelecionado));

        // Registrar decisão de roteamento
        registrarDecisaoRoteamento(lojista, gatewaySelecionado, valor, gatewaysDisponiveis.size());

        return gatewaySelecionado;
    }

    /**
     * Seleciona gateway alternativo em caso de falha (fallback)
     * 
     * @param lojista Lojista que está processando a transação
     * @param gatewayFalhou Gateway que falhou
     * @param valor Valor da transação em centavos
     * @return Gateway alternativo ou null se não houver
     */
    public Gateway selecionarGatewayFallback(Lojista lojista, Gateway gatewayFalhou, Long valor) {
        logger.warn("Selecionando gateway fallback. Gateway falho: {}", gatewayFalhou.getCodigo());

        List<Gateway> gatewaysDisponiveis = buscarGatewaysDisponiveis();

        // Remover o gateway que falhou
        gatewaysDisponiveis = gatewaysDisponiveis.stream()
            .filter(g -> !g.getId().equals(gatewayFalhou.getId()))
            .collect(Collectors.toList());

        // Filtrar por limites
        gatewaysDisponiveis = filtrarPorLimites(gatewaysDisponiveis, valor);

        if (gatewaysDisponiveis.isEmpty()) {
            logger.error("Nenhum gateway disponível para fallback");
            return null;
        }

        // Para fallback, priorizar estabilidade (alta taxa de sucesso)
        Gateway gatewayFallback = gatewaysDisponiveis.stream()
            .filter(g -> g.getTaxaSucesso() > 95.0)
            .max(Comparator.comparing(Gateway::getTaxaSucesso))
            .orElse(gatewaysDisponiveis.get(0));

        logger.info("Gateway fallback selecionado: {}", gatewayFallback.getCodigo());

        return gatewayFallback;
    }

    /**
     * Busca gateways disponíveis para processamento
     */
    private List<Gateway> buscarGatewaysDisponiveis() {
        return gatewayRepository.findAll().stream()
            .filter(g -> "ACTIVE".equals(g.getStatus().name()))
            .filter(g -> g.isHealthy())
            .collect(Collectors.toList());
    }

    /**
     * Filtra gateways que podem processar o valor solicitado
     */
    private List<Gateway> filtrarPorLimites(List<Gateway> gateways, Long valor) {
        return gateways.stream()
            .filter(g -> g.podeProcessarTransacao(valor))
            .collect(Collectors.toList());
    }

    /**
     * Aplica algoritmo de seleção baseado em score
     */
    private Gateway aplicarAlgoritmoSelecao(List<Gateway> gateways, Long valor) {
        Map<Gateway, Double> scores = new HashMap<>();

        for (Gateway gateway : gateways) {
            double score = calcularScore(gateway);
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
     * 
     * Fatores considerados:
     * - Taxa de sucesso (40%)
     * - Tempo de resposta (30%)
     * - Prioridade configurada (20%)
     * - Capacidade disponível (10%)
     */
    private double calcularScore(Gateway gateway) {
        double score = 0.0;

        // Fator 1: Taxa de sucesso (peso 40%)
        // Taxa de 100% = 40 pontos, 0% = 0 pontos
        score += (gateway.getTaxaSucesso() / 100.0) * 40.0;

        // Fator 2: Tempo de resposta (peso 30%)
        // Tempo < 500ms = 30 pontos, > 5000ms = 0 pontos
        double tempoScore = Math.max(0, 30.0 - (gateway.getTempoRespostaMedio() / 1000.0) * 6.0);
        score += Math.min(tempoScore, 30.0);

        // Fator 3: Prioridade configurada (peso 20%)
        // Prioridade 1 = 20 pontos, prioridade 100 = 0 pontos
        score += (100.0 - gateway.getPrioridade()) / 100.0 * 20.0;

        // Fator 4: Capacidade disponível (peso 10%)
        // Quanto mais capacidade livre, melhor
        double percentualUtilizado = gateway.getPercentualLimiteUtilizado();
        score += (100.0 - percentualUtilizado) / 100.0 * 10.0;

        return score;
    }

    /**
     * Registra decisão de roteamento no log
     */
    private void registrarDecisaoRoteamento(Lojista lojista, Gateway gateway, Long valor, int gatewaysAvaliados) {
        LogTransacao log = new LogTransacao();
        log.setGateway(gateway);
        log.setAcao("ROUTING_DECISION");
        log.setStatusNovo("ROUTED");
        log.setMetadata(String.format(
            "{\"lojista_id\":\"%s\",\"gateway_selecionado\":\"%s\",\"valor\":%d,\"gateways_avaliados\":%d,\"score\":%.2f}",
            lojista.getId(),
            gateway.getCodigo(),
            valor,
            gatewaysAvaliados,
            calcularScore(gateway)
        ));
        log.setCreatedAt(ZonedDateTime.now());

        logTransacaoRepository.save(log);
    }

    /**
     * Verifica saúde de todos os gateways e atualiza status
     * 
     * Este método deve ser executado periodicamente (ex: a cada 1 minuto)
     */
    public void verificarSaudeGateways() {
        logger.debug("Iniciando verificação de saúde dos gateways");

        List<Gateway> gateways = gatewayRepository.findAll();

        for (Gateway gateway : gateways) {
            try {
                boolean saudavel = avaliarSaudeGateway(gateway);

                if (saudavel) {
                    gateway.atualizarHealthCheck(com.pip.model.HealthStatus.UP);
                } else {
                    gateway.atualizarHealthCheck(com.pip.model.HealthStatus.DOWN);
                }

                gatewayRepository.save(gateway);

                logger.debug("Gateway {} - Status: {}", gateway.getCodigo(), gateway.getHealthStatus());

            } catch (Exception e) {
                logger.error("Erro ao verificar saúde do gateway {}: {}", gateway.getCodigo(), e.getMessage());
                gateway.atualizarHealthCheck(com.pip.model.HealthStatus.UNKNOWN);
                gatewayRepository.save(gateway);
            }
        }

        logger.info("Verificação de saúde concluída. Total de gateways: {}", gateways.size());
    }

    /**
     * Avalia saúde de um gateway específico
     * 
     * Critérios:
     * - Taxa de sucesso > 90%
     * - Tempo médio de resposta < 5 segundos
     * - Última verificação há menos de 5 minutos
     */
    private boolean avaliarSaudeGateway(Gateway gateway) {
        // Critério 1: Taxa de sucesso
        if (gateway.getTaxaSucesso() < 90.0) {
            logger.warn("Gateway {} com taxa de sucesso baixa: {}%", 
                gateway.getCodigo(), gateway.getTaxaSucesso());
            return false;
        }

        // Critério 2: Tempo de resposta
        if (gateway.getTempoRespostaMedio() > 5000) {
            logger.warn("Gateway {} com tempo de resposta alto: {}ms", 
                gateway.getCodigo(), gateway.getTempoRespostaMedio());
            return false;
        }

        // Critério 3: Última verificação (se houver)
        if (gateway.getLastHealthCheck() != null) {
            ZonedDateTime limiteVerificacao = ZonedDateTime.now().minusMinutes(5);
            if (gateway.getLastHealthCheck().isBefore(limiteVerificacao)) {
                logger.warn("Gateway {} sem verificação recente", gateway.getCodigo());
                return false;
            }
        }

        return true;
    }

    /**
     * Obtém estatísticas de roteamento
     */
    public Map<String, Object> obterEstatisticasRoteamento() {
        Map<String, Object> stats = new HashMap<>();

        List<Gateway> gateways = gatewayRepository.findAll();

        for (Gateway gateway : gateways) {
            Map<String, Object> gatewayStats = new HashMap<>();
            gatewayStats.put("codigo", gateway.getCodigo());
            gatewayStats.put("status", gateway.getStatus().name());
            gatewayStats.put("healthStatus", gateway.getHealthStatus().name());
            gatewayStats.put("totalTransacoes", gateway.getTotalTransacoes());
            gatewayStats.put("totalSucesso", gateway.getTotalSucesso());
            gatewayStats.put("totalFalhas", gateway.getTotalFalhas());
            gatewayStats.put("taxaSucesso", gateway.getTaxaSucesso());
            gatewayStats.put("tempoRespostaMedio", gateway.getTempoRespostaMedio());
            gatewayStats.put("volumeProcessadoHoje", gateway.getVolumeProcessadoHoje());
            gatewayStats.put("limiteDiario", gateway.getLimiteDiario());
            gatewayStats.put("percentualUtilizado", gateway.getPercentualLimiteUtilizado());
            gatewayStats.put("score", calcularScore(gateway));

            stats.put(gateway.getCodigo(), gatewayStats);
        }

        return stats;
    }

    /**
     * Rebalanceia prioridades dos gateways baseado em performance
     * 
     * Este método deve ser executado periodicamente (ex: a cada 1 hora)
     */
    public void rebalancearGateways() {
        logger.info("Iniciando rebalanceamento de gateways");

        List<Gateway> gateways = gatewayRepository.findAll();

        for (Gateway gateway : gateways) {
            int prioridadeAtual = gateway.getPrioridade();
            int novaPrioridade = calcularNovaPrioridade(gateway);

            if (prioridadeAtual != novaPrioridade) {
                logger.info("Ajustando prioridade do gateway {} de {} para {}", 
                    gateway.getCodigo(), prioridadeAtual, novaPrioridade);
                
                gateway.setPrioridade(novaPrioridade);
                gatewayRepository.save(gateway);
            }
        }

        logger.info("Rebalanceamento concluído");
    }

    /**
     * Calcula nova prioridade baseada em performance
     */
    private int calcularNovaPrioridade(Gateway gateway) {
        int prioridade = gateway.getPrioridade();

        // Gateway com excelente performance: diminuir prioridade (número menor = maior prioridade)
        if (gateway.getTaxaSucesso() > 98.0 && gateway.getTempoRespostaMedio() < 1000) {
            prioridade = Math.max(1, prioridade - 5);
        }
        // Gateway com boa performance: manter ou melhorar levemente
        else if (gateway.getTaxaSucesso() > 95.0 && gateway.getTempoRespostaMedio() < 2000) {
            prioridade = Math.max(1, prioridade - 1);
        }
        // Gateway com performance ruim: aumentar prioridade (número maior = menor prioridade)
        else if (gateway.getTaxaSucesso() < 90.0 || gateway.getTempoRespostaMedio() > 3000) {
            prioridade = Math.min(100, prioridade + 5);
        }

        return prioridade;
    }

    /**
     * Reseta volumes processados diariamente
     * 
     * Este método deve ser executado diariamente à meia-noite
     */
    public void resetarVolumesProcessados() {
        logger.info("Resetando volumes processados dos gateways");

        List<Gateway> gateways = gatewayRepository.findAll();

        for (Gateway gateway : gateways) {
            gateway.resetarVolumeProcessadoHoje();
            gatewayRepository.save(gateway);
        }

        logger.info("Volumes resetados. Total de gateways: {}", gateways.size());
    }
}
