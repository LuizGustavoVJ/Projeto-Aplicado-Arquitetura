package com.pip.repository;

import com.pip.model.LogTransacao;
import com.pip.model.Transacao;
import com.pip.model.Lojista;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Repository para operações de persistência da entidade LogTransacao
 */
@Repository
public interface LogTransacaoRepository extends JpaRepository<LogTransacao, UUID> {

    List<LogTransacao> findByTransacaoOrderByTimestampAsc(Transacao transacao);
    
    List<LogTransacao> findByLojistaAndTimestampBetweenOrderByTimestampDesc(
        Lojista lojista, ZonedDateTime inicio, ZonedDateTime fim);
    
    Page<LogTransacao> findByLojistaOrderByTimestampDesc(Lojista lojista, Pageable pageable);
    
    @Query("SELECT l FROM LogTransacao l WHERE l.nivel IN ('ERROR', 'FATAL') AND l.alertado = false")
    List<LogTransacao> findErrorsNotAlerted();
    
    @Query("SELECT l FROM LogTransacao l WHERE l.nivel = 'WARN' AND l.alertado = false AND l.timestamp > :dataLimite")
    List<LogTransacao> findWarningsNotAlerted(@Param("dataLimite") ZonedDateTime dataLimite);
    
    @Query("SELECT l FROM LogTransacao l WHERE l.tempoRespostaMs > :tempoLimite ORDER BY l.tempoRespostaMs DESC")
    List<LogTransacao> findSlowRequests(@Param("tempoLimite") Long tempoLimite);
    
    @Query("SELECT l.evento, COUNT(l) FROM LogTransacao l WHERE l.timestamp BETWEEN :inicio AND :fim GROUP BY l.evento")
    List<Object[]> countEventsByPeriod(@Param("inicio") ZonedDateTime inicio, @Param("fim") ZonedDateTime fim);
    
    @Query("SELECT l FROM LogTransacao l WHERE l.correlationId = :correlationId ORDER BY l.timestamp ASC")
    List<LogTransacao> findByCorrelationId(@Param("correlationId") String correlationId);
    
    @Query("SELECT COUNT(l) FROM LogTransacao l WHERE l.lojista = :lojista AND l.timestamp BETWEEN :inicio AND :fim")
    Long countByLojistaAndPeriod(@Param("lojista") Lojista lojista, @Param("inicio") ZonedDateTime inicio, @Param("fim") ZonedDateTime fim);
}

