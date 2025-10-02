package com.pip.repository;

import com.pip.model.Webhook;
import com.pip.model.Lojista;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Repository para operações de persistência da entidade Webhook
 */
@Repository
public interface WebhookRepository extends JpaRepository<Webhook, UUID> {

    List<Webhook> findByLojistaAndAtivoTrue(Lojista lojista);
    
    List<Webhook> findByEventoAndAtivoTrue(String evento);
    
    @Query("SELECT w FROM Webhook w WHERE w.ativo = true AND w.status = 'ACTIVE'")
    List<Webhook> findAllActiveWebhooks();
    
    @Query("SELECT w FROM Webhook w WHERE w.status = 'FAILED' AND w.ultimaFalhaAt < :dataLimite")
    List<Webhook> findFailedWebhooksForRetry(@Param("dataLimite") ZonedDateTime dataLimite);
    
    @Query("SELECT w FROM Webhook w WHERE w.taxaSucesso < :taxaMinima AND w.totalEnvios > 10")
    List<Webhook> findUnhealthyWebhooks(@Param("taxaMinima") Double taxaMinima);
    
    List<Webhook> findByLojistaOrderByCreatedAtDesc(Lojista lojista);
}

