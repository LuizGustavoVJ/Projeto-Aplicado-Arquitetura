package com.pip.repository;

import com.pip.model.ApiKey;
import com.pip.model.Lojista;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository para operações de persistência da entidade ApiKey
 */
@Repository
public interface ApiKeyRepository extends JpaRepository<ApiKey, UUID> {

    Optional<ApiKey> findByKeyHash(String keyHash);
    
    List<ApiKey> findByLojistaAndStatus(Lojista lojista, String status);
    
    List<ApiKey> findByLojistaOrderByCreatedAtDesc(Lojista lojista);
    
    @Query("SELECT a FROM ApiKey a WHERE a.status = 'ACTIVE' AND (a.expiresAt IS NULL OR a.expiresAt > CURRENT_TIMESTAMP)")
    List<ApiKey> findAllActive();
    
    @Query("SELECT a FROM ApiKey a WHERE a.expiresAt IS NOT NULL AND a.expiresAt BETWEEN CURRENT_TIMESTAMP AND :dataLimite")
    List<ApiKey> findExpiringBefore(@Param("dataLimite") ZonedDateTime dataLimite);
    
    @Query("SELECT a FROM ApiKey a WHERE a.rotatedAt IS NULL AND a.createdAt < :dataLimite")
    List<ApiKey> findNeverRotatedBefore(@Param("dataLimite") ZonedDateTime dataLimite);
    
    @Query("SELECT a FROM ApiKey a WHERE a.rotatedAt IS NOT NULL AND a.rotatedAt < :dataLimite")
    List<ApiKey> findNotRotatedSince(@Param("dataLimite") ZonedDateTime dataLimite);
}

