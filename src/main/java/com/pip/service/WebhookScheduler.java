package com.pip.service;

import com.pip.model.WebhookEvent;
import com.pip.repository.WebhookEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

/**
 * Scheduler para processamento de webhooks pendentes e reenvio
 * 
 * Executa verificações periódicas para:
 * - Enviar webhooks pendentes
 * - Reenviar webhooks falhados que estão agendados para nova tentativa
 * - Limpar webhooks antigos
 * 
 * @author Luiz Gustavo Finotello
 */
@Service
public class WebhookScheduler {

    private static final Logger logger = LoggerFactory.getLogger(WebhookScheduler.class);

    @Autowired
    private WebhookEventRepository webhookEventRepository;

    @Autowired
    private WebhookService webhookService;

    /**
     * Processa webhooks pendentes a cada 30 segundos
     */
    @Scheduled(fixedRate = 30000) // 30 segundos
    public void processarWebhooksPendentes() {
        logger.debug("Verificando webhooks pendentes para envio");

        try {
            // Buscar webhooks pendentes que estão prontos para envio
            List<WebhookEvent> webhooksPendentes = webhookEventRepository.findByStatusAndProximaTentativaBefore(
                "PENDING",
                ZonedDateTime.now()
            );

            if (webhooksPendentes.isEmpty()) {
                logger.debug("Nenhum webhook pendente para processar");
                return;
            }

            logger.info("Encontrados {} webhooks pendentes para processar", webhooksPendentes.size());

            int sucessos = 0;
            int falhas = 0;

            for (WebhookEvent webhook : webhooksPendentes) {
                try {
                    boolean enviado = webhookService.enviarWebhook(webhook);
                    if (enviado) {
                        sucessos++;
                    } else {
                        falhas++;
                    }
                } catch (Exception e) {
                    logger.error("Erro ao processar webhook {}: {}", webhook.getId(), e.getMessage());
                    falhas++;
                }
            }

            logger.info("Processamento concluído. Sucessos: {}, Falhas: {}", sucessos, falhas);

        } catch (Exception e) {
            logger.error("Erro ao processar webhooks pendentes: {}", e.getMessage(), e);
        }
    }

    /**
     * Processa webhooks que falharam e estão agendados para retry
     */
    @Scheduled(fixedRate = 60000) // 1 minuto
    public void processarWebhooksParaRetry() {
        logger.debug("Verificando webhooks para retry");

        try {
            // Buscar webhooks falhados que estão agendados para nova tentativa
            List<WebhookEvent> webhooksParaRetry = webhookEventRepository.findByStatusAndProximaTentativaBefore(
                "FAILED",
                ZonedDateTime.now()
            );

            if (webhooksParaRetry.isEmpty()) {
                logger.debug("Nenhum webhook para retry");
                return;
            }

            logger.info("Encontrados {} webhooks para retry", webhooksParaRetry.size());

            for (WebhookEvent webhook : webhooksParaRetry) {
                try {
                    // Verificar se ainda não atingiu número máximo de tentativas
                    if (webhook.getTentativas() < webhook.getMaxTentativas()) {
                        webhookService.enviarWebhook(webhook);
                    } else {
                        // Marcar como falha definitiva
                        webhook.setStatus("FAILED");
                        webhook.setProximaTentativa(null);
                        webhook.setErrorMessage("Número máximo de tentativas excedido");
                        webhook.setUpdatedAt(ZonedDateTime.now());
                        webhookEventRepository.save(webhook);
                        
                        logger.warn("Webhook {} falhou definitivamente após {} tentativas",
                            webhook.getId(), webhook.getTentativas());
                    }
                } catch (Exception e) {
                    logger.error("Erro ao processar retry do webhook {}: {}", 
                        webhook.getId(), e.getMessage());
                }
            }

        } catch (Exception e) {
            logger.error("Erro ao processar webhooks para retry: {}", e.getMessage(), e);
        }
    }

    /**
     * Limpa webhooks antigos (mais de 30 dias) a cada dia
     */
    @Scheduled(cron = "0 0 2 * * *") // Todos os dias às 2h da manhã
    public void limparWebhooksAntigos() {
        logger.info("Iniciando limpeza de webhooks antigos");

        try {
            ZonedDateTime dataLimite = ZonedDateTime.now().minusDays(30);
            
            // Buscar webhooks antigos com sucesso
            List<WebhookEvent> webhooksAntigos = webhookEventRepository.findByStatusAndCreatedAtBefore(
                "SUCCESS",
                dataLimite
            );

            if (webhooksAntigos.isEmpty()) {
                logger.info("Nenhum webhook antigo para limpar");
                return;
            }

            logger.info("Encontrados {} webhooks antigos para limpar", webhooksAntigos.size());

            // Deletar webhooks antigos
            webhookEventRepository.deleteAll(webhooksAntigos);

            logger.info("Limpeza concluída. {} webhooks removidos", webhooksAntigos.size());

        } catch (Exception e) {
            logger.error("Erro ao limpar webhooks antigos: {}", e.getMessage(), e);
        }
    }

    /**
     * Gera relatório de webhooks falhados a cada hora
     */
    @Scheduled(cron = "0 0 * * * *") // A cada hora
    public void gerarRelatorioWebhooksFalhados() {
        logger.debug("Gerando relatório de webhooks falhados");

        try {
            // Buscar webhooks falhados nas últimas 24 horas
            ZonedDateTime ultimasDias = ZonedDateTime.now().minusHours(24);
            List<WebhookEvent> webhooksFalhados = webhookEventRepository.findByStatusAndCreatedAtAfter(
                "FAILED",
                ultimasDias
            );

            if (webhooksFalhados.isEmpty()) {
                logger.info("Nenhum webhook falhado nas últimas 24 horas");
                return;
            }

            logger.warn("RELATÓRIO: {} webhooks falhados nas últimas 24 horas", webhooksFalhados.size());

            // Agrupar por lojista
            Map<String, Long> falhasPorLojista = webhooksFalhados.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                    w -> w.getLojista().getNomeFantasia(),
                    java.util.stream.Collectors.counting()
                ));

            falhasPorLojista.forEach((lojista, count) -> 
                logger.warn("  - Lojista {}: {} webhooks falhados", lojista, count)
            );

        } catch (Exception e) {
            logger.error("Erro ao gerar relatório de webhooks falhados: {}", e.getMessage(), e);
        }
    }
}
