package com.pip.repository;

import com.pip.model.Gateway;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository para operações de persistência da entidade Gateway
 */
@Repository
public interface GatewayRepository extends JpaRepository<Gateway, UUID> {

    Optional<Gateway> findByCodigo(String codigo);
    
    List<Gateway> findByStatusOrderByPrioridadeAsc(String status);
    
    @Query("SELECT g FROM Gateway g WHERE g.status = 'ACTIVE' AND g.healthStatus = 'UP' ORDER BY g.prioridade ASC, g.taxaSucesso DESC")
    List<Gateway> findActiveAndHealthyOrderByPriorityAndSuccessRate();
    
    @Query("SELECT g FROM Gateway g WHERE g.volumeProcessadoHoje < g.limiteDiario AND g.status = 'ACTIVE'")
    List<Gateway> findAvailableForProcessing();
    
    @Query("SELECT g FROM Gateway g WHERE g.volumeProcessadoHoje >= (g.limiteDiario * 0.9)")
    List<Gateway> findNearDailyLimit();
    
    @Query("UPDATE Gateway g SET g.volumeProcessadoHoje = 0")
    int resetAllDailyVolume();
}

