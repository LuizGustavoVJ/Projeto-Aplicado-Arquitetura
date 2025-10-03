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

    @Column(name = "transaction_id")
    private String transactionId;

    @Column(name = "gateway_transaction_id")
    private String gatewayTransactionId;

    @Column(name = "customer_name")
    private String customerName;

    @Column(name = "customer_email")
    private String customerEmail;

    @Column(name = "customer_document")
    private String customerDocument;

    @Column(name = "card_brand")
    private String cardBrand;

    @Column(name = "card_last_digits")
    private String cardLastDigits;

    @Column(name = "moeda")
    private String moeda;

    @Column(name = "parcelas")
    private Integer parcelas;

    @Column(name = "authorization_code")
    private String authorizationCode;

    @Column(name = "nsu")
    private String nsu;

    @Column(name = "tid")
    private String tid;

    @Column(name = "authorized_at")
    private ZonedDateTime authorizedAt;

    @Column(name = "captured_at")
    private ZonedDateTime capturedAt;

    @Column(name = "voided_at")
    private ZonedDateTime voidedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lojista_id", insertable = false, updatable = false)
    private Lojista lojista;

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

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public String getGatewayTransactionId() {
        return gatewayTransactionId;
    }

    public void setGatewayTransactionId(String gatewayTransactionId) {
        this.gatewayTransactionId = gatewayTransactionId;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getCustomerEmail() {
        return customerEmail;
    }

    public void setCustomerEmail(String customerEmail) {
        this.customerEmail = customerEmail;
    }

    public String getCustomerDocument() {
        return customerDocument;
    }

    public void setCustomerDocument(String customerDocument) {
        this.customerDocument = customerDocument;
    }

    public String getCardBrand() {
        return cardBrand;
    }

    public void setCardBrand(String cardBrand) {
        this.cardBrand = cardBrand;
    }

    public String getCardLastDigits() {
        return cardLastDigits;
    }

    public void setCardLastDigits(String cardLastDigits) {
        this.cardLastDigits = cardLastDigits;
    }

    public String getMoeda() {
        return moeda;
    }

    public void setMoeda(String moeda) {
        this.moeda = moeda;
    }

    public Integer getParcelas() {
        return parcelas;
    }

    public void setParcelas(Integer parcelas) {
        this.parcelas = parcelas;
    }

    public String getAuthorizationCode() {
        return authorizationCode;
    }

    public void setAuthorizationCode(String authorizationCode) {
        this.authorizationCode = authorizationCode;
    }

    public String getNsu() {
        return nsu;
    }

    public void setNsu(String nsu) {
        this.nsu = nsu;
    }

    public String getTid() {
        return tid;
    }

    public void setTid(String tid) {
        this.tid = tid;
    }

    public ZonedDateTime getAuthorizedAt() {
        return authorizedAt;
    }

    public void setAuthorizedAt(ZonedDateTime authorizedAt) {
        this.authorizedAt = authorizedAt;
    }

    public ZonedDateTime getCapturedAt() {
        return capturedAt;
    }

    public void setCapturedAt(ZonedDateTime capturedAt) {
        this.capturedAt = capturedAt;
    }

    public ZonedDateTime getVoidedAt() {
        return voidedAt;
    }

    public void setVoidedAt(ZonedDateTime voidedAt) {
        this.voidedAt = voidedAt;
    }

    public Lojista getLojista() {
        return lojista;
    }

    public void setLojista(Lojista lojista) {
        this.lojista = lojista;
    }
}

