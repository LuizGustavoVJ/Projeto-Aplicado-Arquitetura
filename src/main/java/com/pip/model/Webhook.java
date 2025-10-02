package com.pip.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import java.time.ZonedDateTime;
import java.util.UUID;

/**
 * Entidade que representa uma configuração de webhook
 */
@Entity
@Table(name = "webhook", indexes = {
    @Index(name = "idx_webhook_lojista", columnList = "lojista_id"),
    @Index(name = "idx_webhook_status", columnList = "status"),
    @Index(name = "idx_webhook_evento", columnList = "evento")
})
public class Webhook {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lojista_id", nullable = false)
    private Lojista lojista;

    @Column(name = "nome", nullable = false, length = 100)
    @NotBlank(message = "Nome do webhook é obrigatório")
    @Size(max = 100, message = "Nome deve ter no máximo 100 caracteres")
    private String nome;

    @Column(name = "url", nullable = false, length = 500)
    @NotBlank(message = "URL do webhook é obrigatória")
    @Size(max = 500, message = "URL deve ter no máximo 500 caracteres")
    private String url;

    @Column(name = "evento", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private EventoWebhook evento;

    @Column(name = "status", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private StatusWebhook status;

    @Column(name = "secret", nullable = false, length = 100)
    @Size(max = 100, message = "Secret deve ter no máximo 100 caracteres")
    private String secret; // Para assinatura HMAC

    @Column(name = "timeout_segundos", nullable = false)
    @Min(value = 1, message = "Timeout deve ser no mínimo 1 segundo")
    @Max(value = 300, message = "Timeout deve ser no máximo 300 segundos")
    private Integer timeoutSegundos = 30;

    @Column(name = "max_tentativas", nullable = false)
    @Min(value = 1, message = "Máximo de tentativas deve ser no mínimo 1")
    @Max(value = 10, message = "Máximo de tentativas deve ser no máximo 10")
    private Integer maxTentativas = 3;

    @Column(name = "intervalo_retry_segundos", nullable = false)
    @Min(value = 1, message = "Intervalo de retry deve ser no mínimo 1 segundo")
    private Integer intervaloRetrySegundos = 60;

    @Column(name = "total_envios", nullable = false)
    private Long totalEnvios = 0L;

    @Column(name = "total_sucessos", nullable = false)
    private Long totalSucessos = 0L;

    @Column(name = "total_falhas", nullable = false)
    private Long totalFalhas = 0L;

    @Column(name = "taxa_sucesso", nullable = false)
    private Double taxaSucesso = 0.0;

    @Column(name = "ultimo_envio_at")
    private ZonedDateTime ultimoEnvioAt;

    @Column(name = "ultimo_sucesso_at")
    private ZonedDateTime ultimoSucessoAt;

    @Column(name = "ultima_falha_at")
    private ZonedDateTime ultimaFalhaAt;

    @Column(name = "ultima_falha_motivo", length = 500)
    private String ultimaFalhaMotivo;

    @Column(name = "headers_customizados", length = 1000)
    private String headersCustomizados; // JSON string

    @Column(name = "filtros", length = 500)
    private String filtros; // Condições para envio (JSON)

    @Column(name = "ativo", nullable = false)
    private Boolean ativo = true;

    @Column(name = "created_at", nullable = false)
    private ZonedDateTime createdAt;

    @Column(name = "updated_at")
    private ZonedDateTime updatedAt;

    @Column(name = "last_test_at")
    private ZonedDateTime lastTestAt;

    @Column(name = "last_test_success")
    private Boolean lastTestSuccess;

    // Construtores
    public Webhook() {
        this.createdAt = ZonedDateTime.now();
        this.status = StatusWebhook.ACTIVE;
        this.ativo = true;
        this.totalEnvios = 0L;
        this.totalSucessos = 0L;
        this.totalFalhas = 0L;
        this.taxaSucesso = 0.0;
    }

    public Webhook(Lojista lojista, String nome, String url, EventoWebhook evento) {
        this();
        this.lojista = lojista;
        this.nome = nome;
        this.url = url;
        this.evento = evento;
    }

    // Métodos de negócio
    public void ativar() {
        this.status = StatusWebhook.ACTIVE;
        this.ativo = true;
        this.updatedAt = ZonedDateTime.now();
    }

    public void desativar() {
        this.status = StatusWebhook.INACTIVE;
        this.ativo = false;
        this.updatedAt = ZonedDateTime.now();
    }

    public void pausar() {
        this.status = StatusWebhook.PAUSED;
        this.ativo = false;
        this.updatedAt = ZonedDateTime.now();
    }

    public void marcarFalha(String motivo) {
        this.status = StatusWebhook.FAILED;
        this.ultimaFalhaAt = ZonedDateTime.now();
        this.ultimaFalhaMotivo = motivo;
        this.updatedAt = ZonedDateTime.now();
        
        // Se muitas falhas consecutivas, pausar automaticamente
        if (this.totalFalhas > 0 && this.totalFalhas % 10 == 0) {
            this.pausar();
        }
    }

    public void registrarEnvio(boolean sucesso, String motivoFalha) {
        this.totalEnvios++;
        this.ultimoEnvioAt = ZonedDateTime.now();
        
        if (sucesso) {
            this.totalSucessos++;
            this.ultimoSucessoAt = ZonedDateTime.now();
            // Se estava com falha, reativar
            if (this.status == StatusWebhook.FAILED) {
                this.status = StatusWebhook.ACTIVE;
            }
        } else {
            this.totalFalhas++;
            this.ultimaFalhaAt = ZonedDateTime.now();
            this.ultimaFalhaMotivo = motivoFalha;
        }
        
        // Atualizar taxa de sucesso
        this.taxaSucesso = (this.totalSucessos.doubleValue() / this.totalEnvios.doubleValue()) * 100.0;
        this.updatedAt = ZonedDateTime.now();
    }

    public void registrarTeste(boolean sucesso) {
        this.lastTestAt = ZonedDateTime.now();
        this.lastTestSuccess = sucesso;
        this.updatedAt = ZonedDateTime.now();
    }

    public boolean podeEnviar() {
        return this.ativo && 
               this.status == StatusWebhook.ACTIVE && 
               this.url != null && 
               !this.url.trim().isEmpty();
    }

    public boolean precisaRetry() {
        return this.status == StatusWebhook.FAILED && 
               this.ultimaFalhaAt != null &&
               ZonedDateTime.now().isAfter(this.ultimaFalhaAt.plusSeconds(this.intervaloRetrySegundos));
    }

    public String gerarAssinatura(String payload) {
        try {
            javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
            javax.crypto.spec.SecretKeySpec secretKeySpec = new javax.crypto.spec.SecretKeySpec(
                this.secret.getBytes(), "HmacSHA256");
            mac.init(secretKeySpec);
            byte[] hash = mac.doFinal(payload.getBytes());
            return "sha256=" + bytesToHex(hash);
        } catch (Exception e) {
            throw new RuntimeException("Erro ao gerar assinatura HMAC", e);
        }
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }

    public boolean isHealthy() {
        // Considera saudável se taxa de sucesso > 80% nos últimos envios
        return this.taxaSucesso >= 80.0;
    }

    public long getMinutosDesdeUltimaFalha() {
        if (this.ultimaFalhaAt == null) {
            return Long.MAX_VALUE;
        }
        return this.ultimaFalhaAt.until(ZonedDateTime.now(), java.time.temporal.ChronoUnit.MINUTES);
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
        this.updatedAt = ZonedDateTime.now();
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
        this.updatedAt = ZonedDateTime.now();
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
        this.updatedAt = ZonedDateTime.now();
    }

    public EventoWebhook getEvento() {
        return evento;
    }

    public void setEvento(EventoWebhook evento) {
        this.evento = evento;
        this.updatedAt = ZonedDateTime.now();
    }

    public StatusWebhook getStatus() {
        return status;
    }

    public void setStatus(StatusWebhook status) {
        this.status = status;
        this.updatedAt = ZonedDateTime.now();
    }

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
        this.updatedAt = ZonedDateTime.now();
    }

    public Integer getTimeoutSegundos() {
        return timeoutSegundos;
    }

    public void setTimeoutSegundos(Integer timeoutSegundos) {
        this.timeoutSegundos = timeoutSegundos;
        this.updatedAt = ZonedDateTime.now();
    }

    public Integer getMaxTentativas() {
        return maxTentativas;
    }

    public void setMaxTentativas(Integer maxTentativas) {
        this.maxTentativas = maxTentativas;
        this.updatedAt = ZonedDateTime.now();
    }

    public Integer getIntervaloRetrySegundos() {
        return intervaloRetrySegundos;
    }

    public void setIntervaloRetrySegundos(Integer intervaloRetrySegundos) {
        this.intervaloRetrySegundos = intervaloRetrySegundos;
        this.updatedAt = ZonedDateTime.now();
    }

    public Long getTotalEnvios() {
        return totalEnvios;
    }

    public void setTotalEnvios(Long totalEnvios) {
        this.totalEnvios = totalEnvios;
        this.updatedAt = ZonedDateTime.now();
    }

    public Long getTotalSucessos() {
        return totalSucessos;
    }

    public void setTotalSucessos(Long totalSucessos) {
        this.totalSucessos = totalSucessos;
        this.updatedAt = ZonedDateTime.now();
    }

    public Long getTotalFalhas() {
        return totalFalhas;
    }

    public void setTotalFalhas(Long totalFalhas) {
        this.totalFalhas = totalFalhas;
        this.updatedAt = ZonedDateTime.now();
    }

    public Double getTaxaSucesso() {
        return taxaSucesso;
    }

    public void setTaxaSucesso(Double taxaSucesso) {
        this.taxaSucesso = taxaSucesso;
        this.updatedAt = ZonedDateTime.now();
    }

    public ZonedDateTime getUltimoEnvioAt() {
        return ultimoEnvioAt;
    }

    public void setUltimoEnvioAt(ZonedDateTime ultimoEnvioAt) {
        this.ultimoEnvioAt = ultimoEnvioAt;
    }

    public ZonedDateTime getUltimoSucessoAt() {
        return ultimoSucessoAt;
    }

    public void setUltimoSucessoAt(ZonedDateTime ultimoSucessoAt) {
        this.ultimoSucessoAt = ultimoSucessoAt;
    }

    public ZonedDateTime getUltimaFalhaAt() {
        return ultimaFalhaAt;
    }

    public void setUltimaFalhaAt(ZonedDateTime ultimaFalhaAt) {
        this.ultimaFalhaAt = ultimaFalhaAt;
    }

    public String getUltimaFalhaMotivo() {
        return ultimaFalhaMotivo;
    }

    public void setUltimaFalhaMotivo(String ultimaFalhaMotivo) {
        this.ultimaFalhaMotivo = ultimaFalhaMotivo;
    }

    public String getHeadersCustomizados() {
        return headersCustomizados;
    }

    public void setHeadersCustomizados(String headersCustomizados) {
        this.headersCustomizados = headersCustomizados;
        this.updatedAt = ZonedDateTime.now();
    }

    public String getFiltros() {
        return filtros;
    }

    public void setFiltros(String filtros) {
        this.filtros = filtros;
        this.updatedAt = ZonedDateTime.now();
    }

    public Boolean getAtivo() {
        return ativo;
    }

    public void setAtivo(Boolean ativo) {
        this.ativo = ativo;
        this.updatedAt = ZonedDateTime.now();
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

    public ZonedDateTime getLastTestAt() {
        return lastTestAt;
    }

    public void setLastTestAt(ZonedDateTime lastTestAt) {
        this.lastTestAt = lastTestAt;
    }

    public Boolean getLastTestSuccess() {
        return lastTestSuccess;
    }

    public void setLastTestSuccess(Boolean lastTestSuccess) {
        this.lastTestSuccess = lastTestSuccess;
    }
}

/**
 * Enum para eventos de webhook
 */
enum EventoWebhook {
    PAYMENT_AUTHORIZED("payment.authorized", "Pagamento autorizado"),
    PAYMENT_CAPTURED("payment.captured", "Pagamento capturado"),
    PAYMENT_VOIDED("payment.voided", "Pagamento cancelado"),
    PAYMENT_FAILED("payment.failed", "Pagamento falhou"),
    PAYMENT_REFUNDED("payment.refunded", "Pagamento estornado"),
    ALL("*", "Todos os eventos");

    private final String code;
    private final String description;

    EventoWebhook(String code, String description) {
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
 * Enum para status do webhook
 */
enum StatusWebhook {
    ACTIVE("active", "Ativo"),
    INACTIVE("inactive", "Inativo"),
    PAUSED("paused", "Pausado"),
    FAILED("failed", "Com falha");

    private final String code;
    private final String description;

    StatusWebhook(String code, String description) {
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

