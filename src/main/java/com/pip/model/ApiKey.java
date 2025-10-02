package com.pip.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.ZonedDateTime;
import java.util.UUID;

/**
 * Entidade que representa uma chave de API para autenticação
 */
@Entity
@Table(name = "api_key", indexes = {
    @Index(name = "idx_api_key_key_hash", columnList = "key_hash"),
    @Index(name = "idx_api_key_lojista", columnList = "lojista_id"),
    @Index(name = "idx_api_key_status", columnList = "status")
})
public class ApiKey {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lojista_id", nullable = false)
    private Lojista lojista;

    @Column(name = "nome", nullable = false, length = 100)
    @NotBlank(message = "Nome da API Key é obrigatório")
    @Size(max = 100, message = "Nome deve ter no máximo 100 caracteres")
    private String nome;

    @Column(name = "key_hash", nullable = false, unique = true, length = 255)
    private String keyHash; // SHA-256 da chave

    @Column(name = "key_prefix", nullable = false, length = 20)
    private String keyPrefix; // Primeiros caracteres visíveis (ex: pip_live_abc...)

    @Column(name = "ambiente", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private AmbienteApiKey ambiente;

    @Column(name = "status", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private StatusApiKey status;

    @Column(name = "escopo", length = 500)
    private String escopo; // Permissões separadas por vírgula

    @Column(name = "ip_whitelist", length = 1000)
    private String ipWhitelist; // IPs permitidos separados por vírgula

    @Column(name = "rate_limit_per_minute", nullable = false)
    private Integer rateLimitPerMinute = 100;

    @Column(name = "total_requests", nullable = false)
    private Long totalRequests = 0L;

    @Column(name = "last_used_at")
    private ZonedDateTime lastUsedAt;

    @Column(name = "last_used_ip", length = 45)
    private String lastUsedIp;

    @Column(name = "expires_at")
    private ZonedDateTime expiresAt;

    @Column(name = "created_at", nullable = false)
    private ZonedDateTime createdAt;

    @Column(name = "updated_at")
    private ZonedDateTime updatedAt;

    @Column(name = "rotated_at")
    private ZonedDateTime rotatedAt;

    // Construtores
    public ApiKey() {
        this.createdAt = ZonedDateTime.now();
        this.status = StatusApiKey.ACTIVE;
        this.totalRequests = 0L;
    }

    public ApiKey(Lojista lojista, String nome, AmbienteApiKey ambiente) {
        this();
        this.lojista = lojista;
        this.nome = nome;
        this.ambiente = ambiente;
        // Definir expiração padrão para 1 ano
        this.expiresAt = ZonedDateTime.now().plusYears(1);
    }

    // Métodos de negócio
    public void revogar() {
        this.status = StatusApiKey.REVOKED;
        this.updatedAt = ZonedDateTime.now();
    }

    public void suspender() {
        this.status = StatusApiKey.SUSPENDED;
        this.updatedAt = ZonedDateTime.now();
    }

    public void reativar() {
        if (this.status == StatusApiKey.REVOKED) {
            throw new IllegalStateException("API Key revogada não pode ser reativada");
        }
        this.status = StatusApiKey.ACTIVE;
        this.updatedAt = ZonedDateTime.now();
    }

    public boolean isValida() {
        if (this.status != StatusApiKey.ACTIVE) {
            return false;
        }
        if (this.expiresAt != null && ZonedDateTime.now().isAfter(this.expiresAt)) {
            return false;
        }
        return true;
    }

    public boolean isIpPermitido(String ip) {
        if (this.ipWhitelist == null || this.ipWhitelist.trim().isEmpty()) {
            return true; // Sem restrição de IP
        }
        String[] ipsPermitidos = this.ipWhitelist.split(",");
        for (String ipPermitido : ipsPermitidos) {
            if (ipPermitido.trim().equals(ip)) {
                return true;
            }
        }
        return false;
    }

    public boolean temPermissao(String permissao) {
        if (this.escopo == null || this.escopo.trim().isEmpty()) {
            return true; // Sem restrição de escopo
        }
        String[] permissoes = this.escopo.split(",");
        for (String perm : permissoes) {
            if (perm.trim().equals(permissao) || perm.trim().equals("*")) {
                return true;
            }
        }
        return false;
    }

    public void registrarUso(String ip) {
        this.totalRequests++;
        this.lastUsedAt = ZonedDateTime.now();
        this.lastUsedIp = ip;
        this.updatedAt = ZonedDateTime.now();
    }

    public void rotacionar(String novoKeyHash, String novoKeyPrefix) {
        this.keyHash = novoKeyHash;
        this.keyPrefix = novoKeyPrefix;
        this.rotatedAt = ZonedDateTime.now();
        this.updatedAt = ZonedDateTime.now();
        // Estender expiração por mais 1 ano
        this.expiresAt = ZonedDateTime.now().plusYears(1);
    }

    public boolean precisaRotacao() {
        if (this.rotatedAt == null) {
            // Se nunca foi rotacionada, verificar se tem mais de 90 dias
            return ZonedDateTime.now().isAfter(this.createdAt.plusDays(90));
        }
        // Se já foi rotacionada, verificar se tem mais de 90 dias desde a última rotação
        return ZonedDateTime.now().isAfter(this.rotatedAt.plusDays(90));
    }

    public long getDiasParaExpiracao() {
        if (this.expiresAt == null) {
            return Long.MAX_VALUE;
        }
        return ZonedDateTime.now().until(this.expiresAt, java.time.temporal.ChronoUnit.DAYS);
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

    public String getKeyHash() {
        return keyHash;
    }

    public void setKeyHash(String keyHash) {
        this.keyHash = keyHash;
        this.updatedAt = ZonedDateTime.now();
    }

    public String getKeyPrefix() {
        return keyPrefix;
    }

    public void setKeyPrefix(String keyPrefix) {
        this.keyPrefix = keyPrefix;
        this.updatedAt = ZonedDateTime.now();
    }

    public AmbienteApiKey getAmbiente() {
        return ambiente;
    }

    public void setAmbiente(AmbienteApiKey ambiente) {
        this.ambiente = ambiente;
        this.updatedAt = ZonedDateTime.now();
    }

    public StatusApiKey getStatus() {
        return status;
    }

    public void setStatus(StatusApiKey status) {
        this.status = status;
        this.updatedAt = ZonedDateTime.now();
    }

    public String getEscopo() {
        return escopo;
    }

    public void setEscopo(String escopo) {
        this.escopo = escopo;
        this.updatedAt = ZonedDateTime.now();
    }

    public String getIpWhitelist() {
        return ipWhitelist;
    }

    public void setIpWhitelist(String ipWhitelist) {
        this.ipWhitelist = ipWhitelist;
        this.updatedAt = ZonedDateTime.now();
    }

    public Integer getRateLimitPerMinute() {
        return rateLimitPerMinute;
    }

    public void setRateLimitPerMinute(Integer rateLimitPerMinute) {
        this.rateLimitPerMinute = rateLimitPerMinute;
        this.updatedAt = ZonedDateTime.now();
    }

    public Long getTotalRequests() {
        return totalRequests;
    }

    public void setTotalRequests(Long totalRequests) {
        this.totalRequests = totalRequests;
        this.updatedAt = ZonedDateTime.now();
    }

    public ZonedDateTime getLastUsedAt() {
        return lastUsedAt;
    }

    public void setLastUsedAt(ZonedDateTime lastUsedAt) {
        this.lastUsedAt = lastUsedAt;
    }

    public String getLastUsedIp() {
        return lastUsedIp;
    }

    public void setLastUsedIp(String lastUsedIp) {
        this.lastUsedIp = lastUsedIp;
    }

    public ZonedDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(ZonedDateTime expiresAt) {
        this.expiresAt = expiresAt;
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

    public ZonedDateTime getRotatedAt() {
        return rotatedAt;
    }

    public void setRotatedAt(ZonedDateTime rotatedAt) {
        this.rotatedAt = rotatedAt;
    }
}

/**
 * Enum para ambiente da API Key
 */
enum AmbienteApiKey {
    SANDBOX("sandbox", "Ambiente de testes"),
    PRODUCTION("production", "Ambiente de produção");

    private final String code;
    private final String description;

    AmbienteApiKey(String code, String description) {
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
 * Enum para status da API Key
 */
enum StatusApiKey {
    ACTIVE("active", "Ativa"),
    SUSPENDED("suspended", "Suspensa"),
    REVOKED("revoked", "Revogada");

    private final String code;
    private final String description;

    StatusApiKey(String code, String description) {
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

