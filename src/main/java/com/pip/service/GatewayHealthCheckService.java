package com.pip.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Serviço de health check periódico dos gateways
 * 
 * Executa verificações agendadas para monitorar a saúde dos gateways
 * e atualizar seus status automaticamente
 * 
 * @author Luiz Gustavo Finotello
 */
@Service
public class GatewayHealthCheckService {

    private static final Logger logger = LoggerFactory.getLogger(GatewayHealthCheckService.class);

    @Autowired
    private GatewayRoutingService routingService;

    /**
     * Verifica saúde dos gateways a cada 1 minuto
     */
    @Scheduled(fixedRate = 60000) // 60 segundos
    public void verificarSaudeGateways() {
        logger.debug("Executando health check dos gateways");
        
        try {
            routingService.verificarSaudeGateways();
            logger.debug("Health check concluído com sucesso");
        } catch (Exception e) {
            logger.error("Erro ao executar health check: {}", e.getMessage(), e);
        }
    }

    /**
     * Rebalanceia prioridades dos gateways a cada 1 hora
     */
    @Scheduled(fixedRate = 3600000) // 1 hora
    public void rebalancearGateways() {
        logger.info("Executando rebalanceamento de gateways");
        
        try {
            routingService.rebalancearGateways();
            logger.info("Rebalanceamento concluído com sucesso");
        } catch (Exception e) {
            logger.error("Erro ao executar rebalanceamento: {}", e.getMessage(), e);
        }
    }

    /**
     * Reseta volumes processados diariamente à meia-noite
     */
    @Scheduled(cron = "0 0 0 * * *") // Todos os dias à meia-noite
    public void resetarVolumesProcessados() {
        logger.info("Executando reset de volumes processados");
        
        try {
            routingService.resetarVolumesProcessados();
            logger.info("Reset de volumes concluído com sucesso");
        } catch (Exception e) {
            logger.error("Erro ao executar reset de volumes: {}", e.getMessage(), e);
        }
    }
}
