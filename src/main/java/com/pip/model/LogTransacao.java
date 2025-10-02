package com.pip.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.ZonedDateTime;
import java.util.UUID;

/**
 * Entidade que representa um log detalhado de transação para auditoria
 */
@Entity
@Table(name = "log_transacao", indexes = {
    @Index(name = "idx_log_transacao_id", columnList = "transacao_id"),
    @Index(name = "idx_log_lojista", columnList = "lojista_id"),
    @Index(name = "idx_log_gateway", columnList = "gateway_id"),
    @Index(name = "idx_log_evento", columnList = "evento"),
    @Index(name = "idx_log_timestamp", columnList = "timestamp"),
    @Index(name = "idx_log_nivel", columnList = "nivel")
})
public class LogTransacao {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transacao_id", nullable = false)
    private Transacao transacao;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lojista_id", nullable = false)
    private Lojista lojista;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "gateway_id")
    private Gateway gateway;

    @Column(name = "evento", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private EventoLog evento;

    @Column(name = "nivel", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private NivelLog nivel;

    @Column(name = "mensagem", nullable = false, length = 1000)
    @NotBlank(message = "Mensagem é obrigatória")
    @Size(max = 1000, message = "Mensagem deve ter no máximo 1000 caracteres")
    private String mensagem;

    @Column(name = "detalhes", columnDefinition = "TEXT")
    private String detalhes; // JSON com informações detalhadas

    @Column(name = "request_payload", columnDefinition = "TEXT")
    private String requestPayload; // Payload da requisição (sem dados sensíveis)

    @Column(name = "response_payload", columnDefinition = "TEXT")
    private String responsePayload; // Payload da resposta

    @Column(name = "http_status")
    private Integer httpStatus;

    @Column(name = "tempo_resposta_ms")
    private Long tempoRespostaMs;

    @Column(name = "ip_origem", length = 45)
    @Size(max = 45, message = "IP origem deve ter no máximo 45 caracteres")
    private String ipOrigem;

    @Column(name = "user_agent", length = 500)
    @Size(max = 500, message = "User agent deve ter no máximo 500 caracteres")
    private String userAgent;

    @Column(name = "api_key_id")
    private UUID apiKeyId;

    @Column(name = "correlation_id", length = 100)
    @Size(max = 100, message = "Correlation ID deve ter no máximo 100 caracteres")
    private String correlationId; // Para rastrear requisições relacionadas

    @Column(name = "session_id", length = 100)
    @Size(max = 100, message = "Session ID deve ter no máximo 100 caracteres")
    private String sessionId;

    @Column(name = "erro_codigo", length = 50)
    @Size(max = 50, message = "Código de erro deve ter no máximo 50 caracteres")
    private String erroCodigo;

    @Column(name = "erro_mensagem", length = 500)
    @Size(max = 500, message = "Mensagem de erro deve ter no máximo 500 caracteres")
    private String erroMensagem;

    @Column(name = "stack_trace", columnDefinition = "TEXT")
    private String stackTrace;

    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata; // JSON com metadados adicionais

    @Column(name = "timestamp", nullable = false)
    private ZonedDateTime timestamp;

    @Column(name = "processado", nullable = false)
    private Boolean processado = false;

    @Column(name = "alertado", nullable = false)
    private Boolean alertado = false;

    // Construtores
    public LogTransacao() {
        this.timestamp = ZonedDateTime.now();
        this.processado = false;
        this.alertado = false;
    }

    public LogTransacao(Transacao transacao, Lojista lojista, EventoLog evento, NivelLog nivel, String mensagem) {
        this();
        this.transacao = transacao;
        this.lojista = lojista;
        this.evento = evento;
        this.nivel = nivel;
        this.mensagem = mensagem;
    }

    // Métodos de negócio
    public void marcarProcessado() {
        this.processado = true;
    }

    public void marcarAlertado() {
        this.alertado = true;
    }

    public boolean isErro() {
        return this.nivel == NivelLog.ERROR || this.nivel == NivelLog.FATAL;
    }

    public boolean isWarning() {
        return this.nivel == NivelLog.WARN;
    }

    public boolean precisaAlerta() {
        return (isErro() || isWarning()) && !this.alertado;
    }

    public void adicionarDetalhes(String chave, Object valor) {
        // Implementação simplificada - em produção usar Jackson
        if (this.detalhes == null) {
            this.detalhes = "{}";
        }
        // Adicionar lógica para inserir no JSON
    }

    public void definirErro(String codigo, String mensagem, String stackTrace) {
        this.erroCodigo = codigo;
        this.erroMensagem = mensagem;
        this.stackTrace = stackTrace;
        this.nivel = NivelLog.ERROR;
    }

    public void definirResposta(Integer httpStatus, String responsePayload, Long tempoResposta) {
        this.httpStatus = httpStatus;
        this.responsePayload = responsePayload;
        this.tempoRespostaMs = tempoResposta;
    }

    public void definirRequisicao(String requestPayload, String ipOrigem, String userAgent) {
        this.requestPayload = requestPayload;
        this.ipOrigem = ipOrigem;
        this.userAgent = userAgent;
    }

    public boolean isRequisicaoLenta() {
        return this.tempoRespostaMs != null && this.tempoRespostaMs > 5000; // > 5 segundos
    }

    public boolean isRequisicaoRapida() {
        return this.tempoRespostaMs != null && this.tempoRespostaMs < 200; // < 200ms
    }

    // Getters e Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Transacao getTransacao() {
        return transacao;
    }

    public void setTransacao(Transacao transacao) {
        this.transacao = transacao;
    }

    public Lojista getLojista() {
        return lojista;
    }

    public void setLojista(Lojista lojista) {
        this.lojista = lojista;
    }

    public Gateway getGateway() {
        return gateway;
    }

    public void setGateway(Gateway gateway) {
        this.gateway = gateway;
    }

    public EventoLog getEvento() {
        return evento;
    }

    public void setEvento(EventoLog evento) {
        this.evento = evento;
    }

    public NivelLog getNivel() {
        return nivel;
    }

    public void setNivel(NivelLog nivel) {
        this.nivel = nivel;
    }

    public String getMensagem() {
        return mensagem;
    }

    public void setMensagem(String mensagem) {
        this.mensagem = mensagem;
    }

    public String getDetalhes() {
        return detalhes;
    }

    public void setDetalhes(String detalhes) {
        this.detalhes = detalhes;
    }

    public String getRequestPayload() {
        return requestPayload;
    }

    public void setRequestPayload(String requestPayload) {
        this.requestPayload = requestPayload;
    }

    public String getResponsePayload() {
        return responsePayload;
    }

    public void setResponsePayload(String responsePayload) {
        this.responsePayload = responsePayload;
    }

    public Integer getHttpStatus() {
        return httpStatus;
    }

    public void setHttpStatus(Integer httpStatus) {
        this.httpStatus = httpStatus;
    }

    public Long getTempoRespostaMs() {
        return tempoRespostaMs;
    }

    public void setTempoRespostaMs(Long tempoRespostaMs) {
        this.tempoRespostaMs = tempoRespostaMs;
    }

    public String getIpOrigem() {
        return ipOrigem;
    }

    public void setIpOrigem(String ipOrigem) {
        this.ipOrigem = ipOrigem;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public UUID getApiKeyId() {
        return apiKeyId;
    }

    public void setApiKeyId(UUID apiKeyId) {
        this.apiKeyId = apiKeyId;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getErroCodigo() {
        return erroCodigo;
    }

    public void setErroCodigo(String erroCodigo) {
        this.erroCodigo = erroCodigo;
    }

    public String getErroMensagem() {
        return erroMensagem;
    }

    public void setErroMensagem(String erroMensagem) {
        this.erroMensagem = erroMensagem;
    }

    public String getStackTrace() {
        return stackTrace;
    }

    public void setStackTrace(String stackTrace) {
        this.stackTrace = stackTrace;
    }

    public String getMetadata() {
        return metadata;
    }

    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }

    public ZonedDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(ZonedDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public Boolean getProcessado() {
        return processado;
    }

    public void setProcessado(Boolean processado) {
        this.processado = processado;
    }

    public Boolean getAlertado() {
        return alertado;
    }

    public void setAlertado(Boolean alertado) {
        this.alertado = alertado;
    }
}

/**
 * Enum para eventos de log
 */
enum EventoLog {
    PAYMENT_REQUEST("payment.request", "Requisição de pagamento recebida"),
    PAYMENT_VALIDATION("payment.validation", "Validação de dados de pagamento"),
    GATEWAY_SELECTION("gateway.selection", "Seleção de gateway"),
    GATEWAY_REQUEST("gateway.request", "Requisição enviada ao gateway"),
    GATEWAY_RESPONSE("gateway.response", "Resposta recebida do gateway"),
    PAYMENT_AUTHORIZED("payment.authorized", "Pagamento autorizado"),
    PAYMENT_CAPTURED("payment.captured", "Pagamento capturado"),
    PAYMENT_VOIDED("payment.voided", "Pagamento cancelado"),
    PAYMENT_FAILED("payment.failed", "Pagamento falhou"),
    WEBHOOK_SENT("webhook.sent", "Webhook enviado"),
    WEBHOOK_FAILED("webhook.failed", "Falha no envio de webhook"),
    SECURITY_VIOLATION("security.violation", "Violação de segurança"),
    RATE_LIMIT_EXCEEDED("rate.limit.exceeded", "Limite de taxa excedido"),
    API_KEY_VALIDATION("api.key.validation", "Validação de API Key"),
    SYSTEM_ERROR("system.error", "Erro do sistema");

    private final String code;
    private final String description;

    EventoLog(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }
}

/**
 * Enum para níveis de log
 */
enum NivelLog {
    TRACE("trace", "Rastreamento detalhado"),
    DEBUG("debug", "Informações de debug"),
    INFO("info", "Informações gerais"),
    WARN("warn", "Aviso"),
    ERROR("error", "Erro"),
    FATAL("fatal", "Erro fatal");

    private final String code;
    private final String description;

    NivelLog(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }
}

