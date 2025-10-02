package com.pip.repository;

import com.pip.model.WebhookEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Repository para operações de persistência da entidade WebhookEvent
 * 
 * @author Luiz Gustavo Finotello
 */
@Repository
public interface WebhookEventRepository extends JpaRepository<WebhookEvent, UUID> {

    /**
     * Busca eventos pendentes prontos para envio
     */
    @Query("SELECT w FROM WebhookEvent w WHERE w.status = :status AND (w.proximaTentativa IS NULL OR w.proximaTentativa <= :agora)")
    List<WebhookEvent> findByStatusAndProximaTentativaBefore(@Param("status") String status, @Param("agora") ZonedDateTime agora);

    /**
     * Busca eventos por status
     */
    List<WebhookEvent> findByStatus(String status);

    /**
     * Busca eventos por status criados após determinada data
     */
    List<WebhookEvent> findByStatusAndCreatedAtAfter(String status, ZonedDateTime createdAt);

    /**
     * Busca eventos por status criados antes de determinada data
     */
    List<WebhookEvent> findByStatusAndCreatedAtBefore(String status, ZonedDateTime createdAt);

    /**
     * Busca eventos de uma transação específica
     */
    List<WebhookEvent> findByTransacaoId(UUID transacaoId);

    /**
     * Busca eventos de um lojista específico
     */
    List<WebhookEvent> findByLojistaId(UUID lojistaId);

    /**
     * Conta eventos por status
     */
    @Query("SELECT w.status, COUNT(w) FROM WebhookEvent w GROUP BY w.status")
    List<Object[]> countByStatus();

    /**
     * Busca eventos falhados que ainda podem ser retentados
     */
    @Query("SELECT w FROM WebhookEvent w WHERE w.status = 'FAILED' AND w.tentativas < w.maxTentativas AND w.proximaTentativa <= :agora")
    List<WebhookEvent> findFailedEventsForRetry(@Param("agora") ZonedDateTime agora);
}
