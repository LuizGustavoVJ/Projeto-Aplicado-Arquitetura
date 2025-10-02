package com.pip.repository;

import com.pip.model.Lojista;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository para operações de persistência da entidade Lojista
 * 
 * @author Luiz Gustavo Finotello
 */
@Repository
public interface LojistaRepository extends JpaRepository<Lojista, UUID> {

    /**
     * Busca lojista por email
     */
    Optional<Lojista> findByEmail(String email);

    /**
     * Busca lojista por CNPJ
     */
    Optional<Lojista> findByCnpj(String cnpj);

    /**
     * Verifica se existe lojista com o email
     */
    boolean existsByEmail(String email);

    /**
     * Verifica se existe lojista com o CNPJ
     */
    boolean existsByCnpj(String cnpj);

    /**
     * Busca lojistas por status
     */
    List<Lojista> findByStatus(String status);

    /**
     * Busca lojistas por plano
     */
    List<Lojista> findByPlano(String plano);

    /**
     * Busca lojistas ativos
     */
    @Query("SELECT l FROM Lojista l WHERE l.status = 'ACTIVE'")
    List<Lojista> findAllActive();

    /**
     * Busca lojistas pendentes de aprovação
     */
    @Query("SELECT l FROM Lojista l WHERE l.status = 'PENDING' ORDER BY l.createdAt ASC")
    List<Lojista> findPendingApproval();

    /**
     * Busca lojistas por nome fantasia (busca parcial)
     */
    @Query("SELECT l FROM Lojista l WHERE LOWER(l.nomeFantasia) LIKE LOWER(CONCAT('%', :nome, '%'))")
    Page<Lojista> findByNomeFantasiaContainingIgnoreCase(@Param("nome") String nome, Pageable pageable);

    /**
     * Busca lojistas criados em um período
     */
    @Query("SELECT l FROM Lojista l WHERE l.createdAt BETWEEN :inicio AND :fim ORDER BY l.createdAt DESC")
    List<Lojista> findByCreatedAtBetween(@Param("inicio") ZonedDateTime inicio, @Param("fim") ZonedDateTime fim);

    /**
     * Busca lojistas que ultrapassaram o limite mensal
     */
    @Query("SELECT l FROM Lojista l WHERE l.volumeProcessado > l.limiteMensal AND l.status = 'ACTIVE'")
    List<Lojista> findOverLimit();

    /**
     * Busca lojistas próximos do limite (90% ou mais)
     */
    @Query("SELECT l FROM Lojista l WHERE l.volumeProcessado >= (l.limiteMensal * 0.9) AND l.status = 'ACTIVE'")
    List<Lojista> findNearLimit();

    /**
     * Conta lojistas por status
     */
    @Query("SELECT l.status, COUNT(l) FROM Lojista l GROUP BY l.status")
    List<Object[]> countByStatus();

    /**
     * Conta lojistas por plano
     */
    @Query("SELECT l.plano, COUNT(l) FROM Lojista l GROUP BY l.plano")
    List<Object[]> countByPlano();

    /**
     * Soma volume processado por período
     */
    @Query("SELECT SUM(l.volumeProcessado) FROM Lojista l WHERE l.createdAt BETWEEN :inicio AND :fim")
    Long sumVolumeProcessadoByPeriod(@Param("inicio") ZonedDateTime inicio, @Param("fim") ZonedDateTime fim);

    /**
     * Busca lojistas com webhook configurado
     */
    @Query("SELECT l FROM Lojista l WHERE l.webhookUrl IS NOT NULL AND l.webhookUrl != ''")
    List<Lojista> findWithWebhook();

    /**
     * Busca lojistas sem webhook configurado
     */
    @Query("SELECT l FROM Lojista l WHERE l.webhookUrl IS NULL OR l.webhookUrl = ''")
    List<Lojista> findWithoutWebhook();

    /**
     * Atualiza volume processado para zero (reset mensal)
     */
    @Query("UPDATE Lojista l SET l.volumeProcessado = 0, l.updatedAt = CURRENT_TIMESTAMP")
    int resetAllVolumeProcessado();

    /**
     * Busca lojistas para reset de volume (primeiro dia do mês)
     */
    @Query("SELECT l FROM Lojista l WHERE l.volumeProcessado > 0")
    List<Lojista> findForVolumeReset();
}

