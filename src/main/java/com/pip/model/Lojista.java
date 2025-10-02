package com.pip.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.ZonedDateTime;
import java.util.UUID;

/**
 * Entidade que representa um lojista (cliente da plataforma PIP)
 * 
 * @author Luiz Gustavo Finotello
 */
@Entity
@Table(name = "lojista", indexes = {
    @Index(name = "idx_lojista_email", columnList = "email"),
    @Index(name = "idx_lojista_cnpj", columnList = "cnpj"),
    @Index(name = "idx_lojista_status", columnList = "status")
})
public class Lojista {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "nome_fantasia", nullable = false, length = 200)
    @NotBlank(message = "Nome fantasia é obrigatório")
    @Size(max = 200, message = "Nome fantasia deve ter no máximo 200 caracteres")
    private String nomeFantasia;

    @Column(name = "razao_social", nullable = false, length = 200)
    @NotBlank(message = "Razão social é obrigatória")
    @Size(max = 200, message = "Razão social deve ter no máximo 200 caracteres")
    private String razaoSocial;

    @Column(name = "cnpj", nullable = false, unique = true, length = 14)
    @NotBlank(message = "CNPJ é obrigatório")
    @Size(min = 14, max = 14, message = "CNPJ deve ter exatamente 14 dígitos")
    private String cnpj;

    @Column(name = "email", nullable = false, unique = true, length = 100)
    @NotBlank(message = "Email é obrigatório")
    @Email(message = "Email deve ter formato válido")
    @Size(max = 100, message = "Email deve ter no máximo 100 caracteres")
    private String email;

    @Column(name = "telefone", length = 20)
    @Size(max = 20, message = "Telefone deve ter no máximo 20 caracteres")
    private String telefone;

    @Column(name = "endereco", length = 500)
    @Size(max = 500, message = "Endereço deve ter no máximo 500 caracteres")
    private String endereco;

    @Column(name = "status", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private LojistaStatus status;

    @Column(name = "plano", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private PlanoLojista plano;

    @Column(name = "webhook_url", length = 500)
    @Size(max = 500, message = "URL do webhook deve ter no máximo 500 caracteres")
    private String webhookUrl;

    @Column(name = "webhook_secret", length = 100)
    @Size(max = 100, message = "Secret do webhook deve ter no máximo 100 caracteres")
    private String webhookSecret;

    @Column(name = "limite_mensal", nullable = false)
    private Long limiteMensal; // Em centavos

    @Column(name = "volume_processado", nullable = false)
    private Long volumeProcessado = 0L; // Em centavos

    @Column(name = "taxa_percentual", nullable = false)
    private Integer taxaPercentual; // Em basis points (ex: 250 = 2.5%)

    @Column(name = "taxa_fixa", nullable = false)
    private Long taxaFixa; // Em centavos

    @Column(name = "created_at", nullable = false)
    private ZonedDateTime createdAt;

    @Column(name = "updated_at")
    private ZonedDateTime updatedAt;

    @Column(name = "activated_at")
    private ZonedDateTime activatedAt;

    // Construtores
    public Lojista() {
        this.createdAt = ZonedDateTime.now();
        this.status = LojistaStatus.PENDING;
        this.volumeProcessado = 0L;
    }

    public Lojista(String nomeFantasia, String razaoSocial, String cnpj, String email, PlanoLojista plano) {
        this();
        this.nomeFantasia = nomeFantasia;
        this.razaoSocial = razaoSocial;
        this.cnpj = cnpj;
        this.email = email;
        this.plano = plano;
        this.limiteMensal = plano.getLimitePadrao();
        this.taxaPercentual = plano.getTaxaPercentual();
        this.taxaFixa = plano.getTaxaFixa();
    }

    // Métodos de negócio
    public void ativar() {
        if (this.status != LojistaStatus.PENDING) {
            throw new IllegalStateException("Lojista só pode ser ativado se estiver pendente");
        }
        this.status = LojistaStatus.ACTIVE;
        this.activatedAt = ZonedDateTime.now();
        this.updatedAt = ZonedDateTime.now();
    }

    public void suspender() {
        if (this.status != LojistaStatus.ACTIVE) {
            throw new IllegalStateException("Lojista só pode ser suspenso se estiver ativo");
        }
        this.status = LojistaStatus.SUSPENDED;
        this.updatedAt = ZonedDateTime.now();
    }

    public void bloquear() {
        this.status = LojistaStatus.BLOCKED;
        this.updatedAt = ZonedDateTime.now();
    }

    public boolean podeProcessarTransacao(Long valor) {
        if (this.status != LojistaStatus.ACTIVE) {
            return false;
        }
        return (this.volumeProcessado + valor) <= this.limiteMensal;
    }

    public void adicionarVolumeProcessado(Long valor) {
        this.volumeProcessado += valor;
        this.updatedAt = ZonedDateTime.now();
    }

    public void resetarVolumeProcessado() {
        this.volumeProcessado = 0L;
        this.updatedAt = ZonedDateTime.now();
    }

    // Getters e Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getNomeFantasia() {
        return nomeFantasia;
    }

    public void setNomeFantasia(String nomeFantasia) {
        this.nomeFantasia = nomeFantasia;
        this.updatedAt = ZonedDateTime.now();
    }

    public String getRazaoSocial() {
        return razaoSocial;
    }

    public void setRazaoSocial(String razaoSocial) {
        this.razaoSocial = razaoSocial;
        this.updatedAt = ZonedDateTime.now();
    }

    public String getCnpj() {
        return cnpj;
    }

    public void setCnpj(String cnpj) {
        this.cnpj = cnpj;
        this.updatedAt = ZonedDateTime.now();
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
        this.updatedAt = ZonedDateTime.now();
    }

    public String getTelefone() {
        return telefone;
    }

    public void setTelefone(String telefone) {
        this.telefone = telefone;
        this.updatedAt = ZonedDateTime.now();
    }

    public String getEndereco() {
        return endereco;
    }

    public void setEndereco(String endereco) {
        this.endereco = endereco;
        this.updatedAt = ZonedDateTime.now();
    }

    public LojistaStatus getStatus() {
        return status;
    }

    public void setStatus(LojistaStatus status) {
        this.status = status;
        this.updatedAt = ZonedDateTime.now();
    }

    public PlanoLojista getPlano() {
        return plano;
    }

    public void setPlano(PlanoLojista plano) {
        this.plano = plano;
        this.limiteMensal = plano.getLimitePadrao();
        this.taxaPercentual = plano.getTaxaPercentual();
        this.taxaFixa = plano.getTaxaFixa();
        this.updatedAt = ZonedDateTime.now();
    }

    public String getWebhookUrl() {
        return webhookUrl;
    }

    public void setWebhookUrl(String webhookUrl) {
        this.webhookUrl = webhookUrl;
        this.updatedAt = ZonedDateTime.now();
    }

    public String getWebhookSecret() {
        return webhookSecret;
    }

    public void setWebhookSecret(String webhookSecret) {
        this.webhookSecret = webhookSecret;
        this.updatedAt = ZonedDateTime.now();
    }

    public Long getLimiteMensal() {
        return limiteMensal;
    }

    public void setLimiteMensal(Long limiteMensal) {
        this.limiteMensal = limiteMensal;
        this.updatedAt = ZonedDateTime.now();
    }

    public Long getVolumeProcessado() {
        return volumeProcessado;
    }

    public void setVolumeProcessado(Long volumeProcessado) {
        this.volumeProcessado = volumeProcessado;
        this.updatedAt = ZonedDateTime.now();
    }

    public Integer getTaxaPercentual() {
        return taxaPercentual;
    }

    public void setTaxaPercentual(Integer taxaPercentual) {
        this.taxaPercentual = taxaPercentual;
        this.updatedAt = ZonedDateTime.now();
    }

    public Long getTaxaFixa() {
        return taxaFixa;
    }

    public void setTaxaFixa(Long taxaFixa) {
        this.taxaFixa = taxaFixa;
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

    public ZonedDateTime getActivatedAt() {
        return activatedAt;
    }

    public void setActivatedAt(ZonedDateTime activatedAt) {
        this.activatedAt = activatedAt;
    }
}

/**
 * Enum para status do lojista
 */
enum LojistaStatus {
    PENDING("pending", "Aguardando aprovação"),
    ACTIVE("active", "Ativo"),
    SUSPENDED("suspended", "Suspenso temporariamente"),
    BLOCKED("blocked", "Bloqueado permanentemente");

    private final String code;
    private final String description;

    LojistaStatus(String code, String description) {
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
 * Enum para planos de lojista
 */
enum PlanoLojista {
    STARTER("starter", "Plano Starter", 10000000L, 290, 39L), // R$ 100k, 2.9%, R$ 0.39
    BUSINESS("business", "Plano Business", 50000000L, 250, 29L), // R$ 500k, 2.5%, R$ 0.29
    ENTERPRISE("enterprise", "Plano Enterprise", 200000000L, 190, 19L); // R$ 2M, 1.9%, R$ 0.19

    private final String code;
    private final String nome;
    private final Long limitePadrao; // Em centavos
    private final Integer taxaPercentual; // Em basis points
    private final Long taxaFixa; // Em centavos

    PlanoLojista(String code, String nome, Long limitePadrao, Integer taxaPercentual, Long taxaFixa) {
        this.code = code;
        this.nome = nome;
        this.limitePadrao = limitePadrao;
        this.taxaPercentual = taxaPercentual;
        this.taxaFixa = taxaFixa;
    }

    public String getCode() {
        return code;
    }

    public String getNome() {
        return nome;
    }

    public Long getLimitePadrao() {
        return limitePadrao;
    }

    public Integer getTaxaPercentual() {
        return taxaPercentual;
    }

    public Long getTaxaFixa() {
        return taxaFixa;
    }
}

