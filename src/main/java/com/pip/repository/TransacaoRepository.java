package com.pip.repository;

import com.pip.model.Transacao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * Repositório para operações de persistência da entidade Transacao
 * 
 * @author Luiz Gustavo Finotello
 */
@Repository
public interface TransacaoRepository extends JpaRepository<Transacao, UUID> {
    
    /**
     * Busca transação pelo ID da transação no gateway
     */
    Transacao findByGatewayTransactionId(String gatewayTransactionId);
    
    /**
     * Busca transação pelo ID da transação
     */
    Transacao findByTransactionId(String transactionId);
    
    // Métodos customizados podem ser adicionados aqui conforme necessário
}

