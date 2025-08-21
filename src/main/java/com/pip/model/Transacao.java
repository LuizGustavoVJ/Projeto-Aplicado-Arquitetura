package com.pip.model;

import jakarta.persistence.*;
import java.time.ZonedDateTime;
import java.util.UUID;

/**
 * Entidade que representa uma transação de pagamento
 * 
 * @author Luiz Gustavo Finotello
 */
@Entity
@Table(name = "transacao")
public class Transacao {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "lojista_id", nullable = false)
    private UUID lojistaId;

    @Column(name = "valor", nullable = false)
    private Long valor;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "card_token", nullable = false)
    private String cardToken;

    @Column(name = "gateway_id")
    private String gatewayId;

    @Column(name = "created_at", nullable = false)
    private ZonedDateTime createdAt;

    @Column(name = "updated_at")
    private ZonedDateTime updatedAt;

    // Construtores
    public Transacao() {
        this.createdAt = ZonedDateTime.now();
    }

    public Transacao(UUID lojistaId, Long valor, String status, String cardToken) {
        this();
        this.lojistaId = lojistaId;
        this.valor = valor;
        this.status = status;
        this.cardToken = cardToken;
    }

    // Getters e Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getLojistaId() {
        return lojistaId;
    }

    public void setLojistaId(UUID lojistaId) {
        this.lojistaId = lojistaId;
    }

    public Long getValor() {
        return valor;
    }

    public void setValor(Long valor) {
        this.valor = valor;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
        this.updatedAt = ZonedDateTime.now();
    }

    public String getCardToken() {
        return cardToken;
    }

    public void setCardToken(String cardToken) {
        this.cardToken = cardToken;
    }

    public String getGatewayId() {
        return gatewayId;
    }

    public void setGatewayId(String gatewayId) {
        this.gatewayId = gatewayId;
    }

    public ZonedDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(ZonedDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public ZonedDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(ZonedDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}

