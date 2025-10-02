package com.pip.model;

import jakarta.persistence.*;
import java.time.ZonedDateTime;
import java.util.UUID;

/**
 * Entidade que representa um evento de webhook individual a ser enviado
 * 
 * Diferente de Webhook (que é uma configuração), WebhookEvent representa
 * cada notificação específica que precisa ser enviada
 * 
 * @author Luiz Gustavo Finotello
 */
@Entity
@Table(name = "webhook_event", indexes = {
    @Index(name = "idx_webhook_event_lojista", columnList = "lojista_id"),
    @Index(name = "idx_webhook_event_transacao", columnList = "transacao_id"),
    @Index(name = "idx_webhook_event_status", columnList = "status"),
    @Index(name = "idx_webhook_event_proxima_tentativa", columnList = "proxima_tentativa")
})
public class WebhookEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lojista_id", nullable = false)
    private Lojista lojista;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transacao_id", nullable = false)
    private Transacao transacao;

    @Column(name = "evento", nullable = false, length = 50)
    private String evento;

    @Column(name = "url", nullable = false, length = 500)
    private String url;

    @Column(name = "payload", nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Column(name = "signature", length = 255)
    private String signature;

    @Column(name = "status", nullable = false, length = 20)
    private String status; // PENDING, SENDING, SUCCESS, FAILED, CANCELLED

    @Column(name = "tentativas", nullable = false)
    private Integer tentativas = 0;

    @Column(name = "max_tentativas", nullable = false)
    private Integer maxTentativas = 5;

    @Column(name = "http_status")
    private Integer httpStatus;

    @Column(name = "response_body", columnDefinition = "TEXT")
    private String responseBody;

    @Column(name = "error_message", length = 500)
    private String errorMessage;

    @Column(name = "proxima_tentativa")
    private ZonedDateTime proximaTentativa;

    @Column(name = "created_at", nullable = false)
    private ZonedDateTime createdAt;

    @Column(name = "updated_at")
    private ZonedDateTime updatedAt;

    @Column(name = "enviado_at")
    private ZonedDateTime enviadoAt;

    @Column(name = "sucesso_at")
    private ZonedDateTime sucessoAt;

    // Construtores
    public WebhookEvent() {
        this.createdAt = ZonedDateTime.now();
        this.status = "PENDING";
        this.tentativas = 0;
        this.maxTentativas = 5;
    }

    public WebhookEvent(Lojista lojista, Transacao transacao, String evento, String url, String payload) {
        this();
        this.lojista = lojista;
        this.transacao = transacao;
        this.evento = evento;
        this.url = url;
        this.payload = payload;
    }

    // Getters e Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Lojista getLojista() {
        return lojista;
    }

    public void setLojista(Lojista lojista) {
        this.lojista = lojista;
    }

    public Transacao getTransacao() {
        return transacao;
    }

    public void setTransacao(Transacao transacao) {
        this.transacao = transacao;
    }

    public String getEvento() {
        return evento;
    }

    public void setEvento(String evento) {
        this.evento = evento;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getTentativas() {
        return tentativas;
    }

    public void setTentativas(Integer tentativas) {
        this.tentativas = tentativas;
    }

    public Integer getMaxTentativas() {
        return maxTentativas;
    }

    public void setMaxTentativas(Integer maxTentativas) {
        this.maxTentativas = maxTentativas;
    }

    public Integer getHttpStatus() {
        return httpStatus;
    }

    public void setHttpStatus(Integer httpStatus) {
        this.httpStatus = httpStatus;
    }

    public String getResponseBody() {
        return responseBody;
    }

    public void setResponseBody(String responseBody) {
        this.responseBody = responseBody;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public ZonedDateTime getProximaTentativa() {
        return proximaTentativa;
    }

    public void setProximaTentativa(ZonedDateTime proximaTentativa) {
        this.proximaTentativa = proximaTentativa;
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

    public ZonedDateTime getEnviadoAt() {
        return enviadoAt;
    }

    public void setEnviadoAt(ZonedDateTime enviadoAt) {
        this.enviadoAt = enviadoAt;
    }

    public ZonedDateTime getSucessoAt() {
        return sucessoAt;
    }

    public void setSucessoAt(ZonedDateTime sucessoAt) {
        this.sucessoAt = sucessoAt;
    }
}
