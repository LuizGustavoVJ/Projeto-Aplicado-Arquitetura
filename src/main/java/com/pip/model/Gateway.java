package com.pip.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import java.time.ZonedDateTime;
import java.util.UUID;

/**
 * Entidade que representa um gateway de pagamento configurado
 * 
 * @author Luiz Gustavo Finotello
 */
@Entity
@Table(name = "gateway", indexes = {
    @Index(name = "idx_gateway_codigo", columnList = "codigo"),
    @Index(name = "idx_gateway_status", columnList = "status"),
    @Index(name = "idx_gateway_prioridade", columnList = "prioridade")
})
public class Gateway {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "codigo", nullable = false, unique = true, length = 50)
    @NotBlank(message = "Código do gateway é obrigatório")
    @Size(max = 50, message = "Código deve ter no máximo 50 caracteres")
    private String codigo;

    @Column(name = "nome", nullable = false, length = 100)
    @NotBlank(message = "Nome do gateway é obrigatório")
    @Size(max = 100, message = "Nome deve ter no máximo 100 caracteres")
    private String nome;

    @Column(name = "descricao", length = 500)
    @Size(max = 500, message = "Descrição deve ter no máximo 500 caracteres")
    private String descricao;

    @Column(name = "tipo", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private TipoGateway tipo;

    @Column(name = "status", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private StatusGateway status;

    @Column(name = "url_base", nullable = false, length = 500)
    @NotBlank(message = "URL base é obrigatória")
    @Size(max = 500, message = "URL base deve ter no máximo 500 caracteres")
    private String urlBase;

    @Column(name = "url_sandbox", length = 500)
    @Size(max = 500, message = "URL sandbox deve ter no máximo 500 caracteres")
    private String urlSandbox;

    @Column(name = "api_key", length = 500)
    @Size(max = 500, message = "API Key deve ter no máximo 500 caracteres")
    private String apiKey;

    @Column(name = "api_secret", length = 500)
    @Size(max = 500, message = "API Secret deve ter no máximo 500 caracteres")
    private String apiSecret;

    @Column(name = "merchant_id", length = 100)
    @Size(max = 100, message = "Merchant ID deve ter no máximo 100 caracteres")
    private String merchantId;

    @Column(name = "prioridade", nullable = false)
    @Min(value = 1, message = "Prioridade deve ser no mínimo 1")
    @Max(value = 100, message = "Prioridade deve ser no máximo 100")
    private Integer prioridade;

    @Column(name = "peso_roteamento", nullable = false)
    @Min(value = 0, message = "Peso deve ser no mínimo 0")
    @Max(value = 100, message = "Peso deve ser no máximo 100")
    private Integer pesoRoteamento;

    @Column(name = "taxa_sucesso", nullable = false)
    private Double taxaSucesso = 0.0; // Percentual de sucesso (0-100)

    @Column(name = "tempo_resposta_medio", nullable = false)
    private Long tempoRespostaMedio = 0L; // Em milissegundos

    @Column(name = "limite_diario", nullable = false)
    private Long limiteDiario; // Em centavos

    @Column(name = "volume_processado_hoje", nullable = false)
    private Long volumeProcessadoHoje = 0L; // Em centavos

    @Column(name = "total_transacoes", nullable = false)
    private Long totalTransacoes = 0L;

    @Column(name = "total_sucesso", nullable = false)
    private Long totalSucesso = 0L;

    @Column(name = "total_falhas", nullable = false)
    private Long totalFalhas = 0L;

    @Column(name = "suporta_captura", nullable = false)
    private Boolean suportaCaptura = true;

    @Column(name = "suporta_cancelamento", nullable = false)
    private Boolean suportaCancelamento = true;

    @Column(name = "suporta_estorno", nullable = false)
    private Boolean suportaEstorno = true;

    @Column(name = "suporta_parcelamento", nullable = false)
    private Boolean suportaParcelamento = false;

    @Column(name = "max_parcelas", nullable = false)
    private Integer maxParcelas = 1;

    @Column(name = "timeout_conexao", nullable = false)
    private Integer timeoutConexao = 30000; // Em milissegundos

    @Column(name = "timeout_leitura", nullable = false)
    private Integer timeoutLeitura = 60000; // Em milissegundos

    @Column(name = "max_tentativas", nullable = false)
    private Integer maxTentativas = 3;

    @Column(name = "intervalo_retry", nullable = false)
    private Integer intervaloRetry = 1000; // Em milissegundos

    @Column(name = "ambiente", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private AmbienteGateway ambiente;

    @Column(name = "created_at", nullable = false)
    private ZonedDateTime createdAt;

    @Column(name = "updated_at")
    private ZonedDateTime updatedAt;

    @Column(name = "last_health_check")
    private ZonedDateTime lastHealthCheck;

    @Column(name = "health_status", length = 20)
    @Enumerated(EnumType.STRING)
    private HealthStatus healthStatus;

    // Construtores
    public Gateway() {
        this.createdAt = ZonedDateTime.now();
        this.status = StatusGateway.INACTIVE;
        this.ambiente = AmbienteGateway.SANDBOX;
        this.healthStatus = HealthStatus.UNKNOWN;
        this.volumeProcessadoHoje = 0L;
        this.totalTransacoes = 0L;
        this.totalSucesso = 0L;
        this.totalFalhas = 0L;
        this.taxaSucesso = 0.0;
        this.tempoRespostaMedio = 0L;
    }

    public Gateway(String codigo, String nome, TipoGateway tipo, String urlBase) {
        this();
        this.codigo = codigo;
        this.nome = nome;
        this.tipo = tipo;
        this.urlBase = urlBase;
    }

    // Métodos de negócio
    public void ativar() {
        if (this.apiKey == null || this.apiKey.trim().isEmpty()) {
            throw new IllegalStateException("API Key é obrigatória para ativar o gateway");
        }
        this.status = StatusGateway.ACTIVE;
        this.updatedAt = ZonedDateTime.now();
    }

    public void desativar() {
        this.status = StatusGateway.INACTIVE;
        this.updatedAt = ZonedDateTime.now();
    }

    public void marcarManutencao() {
        this.status = StatusGateway.MAINTENANCE;
        this.updatedAt = ZonedDateTime.now();
    }

    public boolean podeProcessarTransacao(Long valor) {
        if (this.status != StatusGateway.ACTIVE) {
            return false;
        }
        if (this.healthStatus == HealthStatus.DOWN) {
            return false;
        }
        return (this.volumeProcessadoHoje + valor) <= this.limiteDiario;
    }

    public void registrarTransacao(boolean sucesso, Long valor, Long tempoResposta) {
        this.totalTransacoes++;
        this.volumeProcessadoHoje += valor;
        
        if (sucesso) {
            this.totalSucesso++;
        } else {
            this.totalFalhas++;
        }
        
        // Atualizar taxa de sucesso
        this.taxaSucesso = (this.totalSucesso.doubleValue() / this.totalTransacoes.doubleValue()) * 100.0;
        
        // Atualizar tempo médio de resposta (média móvel simples)
        this.tempoRespostaMedio = (this.tempoRespostaMedio + tempoResposta) / 2;
        
        this.updatedAt = ZonedDateTime.now();
    }

    public void resetarVolumeProcessadoHoje() {
        this.volumeProcessadoHoje = 0L;
        this.updatedAt = ZonedDateTime.now();
    }

    public void atualizarHealthCheck(HealthStatus novoStatus) {
        this.healthStatus = novoStatus;
        this.lastHealthCheck = ZonedDateTime.now();
        this.updatedAt = ZonedDateTime.now();
    }

    public String getUrlAtiva() {
        return this.ambiente == AmbienteGateway.PRODUCTION ? this.urlBase : this.urlSandbox;
    }

    public boolean isHealthy() {
        return this.healthStatus == HealthStatus.UP;
    }

    public double getPercentualLimiteUtilizado() {
        if (this.limiteDiario == 0) return 0.0;
        return (this.volumeProcessadoHoje.doubleValue() / this.limiteDiario.doubleValue()) * 100.0;
    }

    // Getters e Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
        this.updatedAt = ZonedDateTime.now();
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
        this.updatedAt = ZonedDateTime.now();
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
        this.updatedAt = ZonedDateTime.now();
    }

    public TipoGateway getTipo() {
        return tipo;
    }

    public void setTipo(TipoGateway tipo) {
        this.tipo = tipo;
        this.updatedAt = ZonedDateTime.now();
    }

    public StatusGateway getStatus() {
        return status;
    }

    public void setStatus(StatusGateway status) {
        this.status = status;
        this.updatedAt = ZonedDateTime.now();
    }

    public String getUrlBase() {
        return urlBase;
    }

    public void setUrlBase(String urlBase) {
        this.urlBase = urlBase;
        this.updatedAt = ZonedDateTime.now();
    }

    public String getUrlSandbox() {
        return urlSandbox;
    }

    public void setUrlSandbox(String urlSandbox) {
        this.urlSandbox = urlSandbox;
        this.updatedAt = ZonedDateTime.now();
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
        this.updatedAt = ZonedDateTime.now();
    }

    public String getApiSecret() {
        return apiSecret;
    }

    public void setApiSecret(String apiSecret) {
        this.apiSecret = apiSecret;
        this.updatedAt = ZonedDateTime.now();
    }

    public String getMerchantId() {
        return merchantId;
    }

    public void setMerchantId(String merchantId) {
        this.merchantId = merchantId;
        this.updatedAt = ZonedDateTime.now();
    }

    public Integer getPrioridade() {
        return prioridade;
    }

    public void setPrioridade(Integer prioridade) {
        this.prioridade = prioridade;
        this.updatedAt = ZonedDateTime.now();
    }

    public Integer getPesoRoteamento() {
        return pesoRoteamento;
    }

    public void setPesoRoteamento(Integer pesoRoteamento) {
        this.pesoRoteamento = pesoRoteamento;
        this.updatedAt = ZonedDateTime.now();
    }

    public Double getTaxaSucesso() {
        return taxaSucesso;
    }

    public void setTaxaSucesso(Double taxaSucesso) {
        this.taxaSucesso = taxaSucesso;
        this.updatedAt = ZonedDateTime.now();
    }

    public Long getTempoRespostaMedio() {
        return tempoRespostaMedio;
    }

    public void setTempoRespostaMedio(Long tempoRespostaMedio) {
        this.tempoRespostaMedio = tempoRespostaMedio;
        this.updatedAt = ZonedDateTime.now();
    }

    public Long getLimiteDiario() {
        return limiteDiario;
    }

    public void setLimiteDiario(Long limiteDiario) {
        this.limiteDiario = limiteDiario;
        this.updatedAt = ZonedDateTime.now();
    }

    public Long getVolumeProcessadoHoje() {
        return volumeProcessadoHoje;
    }

    public void setVolumeProcessadoHoje(Long volumeProcessadoHoje) {
        this.volumeProcessadoHoje = volumeProcessadoHoje;
        this.updatedAt = ZonedDateTime.now();
    }

    public Long getTotalTransacoes() {
        return totalTransacoes;
    }

    public void setTotalTransacoes(Long totalTransacoes) {
        this.totalTransacoes = totalTransacoes;
        this.updatedAt = ZonedDateTime.now();
    }

    public Long getTotalSucesso() {
        return totalSucesso;
    }

    public void setTotalSucesso(Long totalSucesso) {
        this.totalSucesso = totalSucesso;
        this.updatedAt = ZonedDateTime.now();
    }

    public Long getTotalFalhas() {
        return totalFalhas;
    }

    public void setTotalFalhas(Long totalFalhas) {
        this.totalFalhas = totalFalhas;
        this.updatedAt = ZonedDateTime.now();
    }

    public Boolean getSuportaCaptura() {
        return suportaCaptura;
    }

    public void setSuportaCaptura(Boolean suportaCaptura) {
        this.suportaCaptura = suportaCaptura;
        this.updatedAt = ZonedDateTime.now();
    }

    public Boolean getSuportaCancelamento() {
        return suportaCancelamento;
    }

    public void setSuportaCancelamento(Boolean suportaCancelamento) {
        this.suportaCancelamento = suportaCancelamento;
        this.updatedAt = ZonedDateTime.now();
    }

    public Boolean getSuportaEstorno() {
        return suportaEstorno;
    }

    public void setSuportaEstorno(Boolean suportaEstorno) {
        this.suportaEstorno = suportaEstorno;
        this.updatedAt = ZonedDateTime.now();
    }

    public Boolean getSuportaParcelamento() {
        return suportaParcelamento;
    }

    public void setSuportaParcelamento(Boolean suportaParcelamento) {
        this.suportaParcelamento = suportaParcelamento;
        this.updatedAt = ZonedDateTime.now();
    }

    public Integer getMaxParcelas() {
        return maxParcelas;
    }

    public void setMaxParcelas(Integer maxParcelas) {
        this.maxParcelas = maxParcelas;
        this.updatedAt = ZonedDateTime.now();
    }

    public Integer getTimeoutConexao() {
        return timeoutConexao;
    }

    public void setTimeoutConexao(Integer timeoutConexao) {
        this.timeoutConexao = timeoutConexao;
        this.updatedAt = ZonedDateTime.now();
    }

    public Integer getTimeoutLeitura() {
        return timeoutLeitura;
    }

    public void setTimeoutLeitura(Integer timeoutLeitura) {
        this.timeoutLeitura = timeoutLeitura;
        this.updatedAt = ZonedDateTime.now();
    }

    public Integer getMaxTentativas() {
        return maxTentativas;
    }

    public void setMaxTentativas(Integer maxTentativas) {
        this.maxTentativas = maxTentativas;
        this.updatedAt = ZonedDateTime.now();
    }

    public Integer getIntervaloRetry() {
        return intervaloRetry;
    }

    public void setIntervaloRetry(Integer intervaloRetry) {
        this.intervaloRetry = intervaloRetry;
        this.updatedAt = ZonedDateTime.now();
    }

    public AmbienteGateway getAmbiente() {
        return ambiente;
    }

    public void setAmbiente(AmbienteGateway ambiente) {
        this.ambiente = ambiente;
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

    public ZonedDateTime getLastHealthCheck() {
        return lastHealthCheck;
    }

    public void setLastHealthCheck(ZonedDateTime lastHealthCheck) {
        this.lastHealthCheck = lastHealthCheck;
    }

    public HealthStatus getHealthStatus() {
        return healthStatus;
    }

    public void setHealthStatus(HealthStatus healthStatus) {
        this.healthStatus = healthStatus;
        this.updatedAt = ZonedDateTime.now();
    }
}

/**
 * Enum para tipos de gateway
 */
enum TipoGateway {
    ACQUIRER("acquirer", "Adquirente"),
    SUBACQUIRER("subacquirer", "Subadquirente"),
    FACILITATOR("facilitator", "Facilitador de Pagamento"),
    WALLET("wallet", "Carteira Digital");

    private final String code;
    private final String description;

    TipoGateway(String code, String description) {
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
 * Enum para status do gateway
 */
enum StatusGateway {
    ACTIVE("active", "Ativo"),
    INACTIVE("inactive", "Inativo"),
    MAINTENANCE("maintenance", "Em manutenção");

    private final String code;
    private final String description;

    StatusGateway(String code, String description) {
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
 * Enum para ambiente do gateway
 */
enum AmbienteGateway {
    SANDBOX("sandbox", "Ambiente de testes"),
    PRODUCTION("production", "Ambiente de produção");

    private final String code;
    private final String description;

    AmbienteGateway(String code, String description) {
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
 * Enum para status de saúde do gateway
 */
enum HealthStatus {
    UP("up", "Funcionando"),
    DOWN("down", "Fora do ar"),
    DEGRADED("degraded", "Funcionamento degradado"),
    UNKNOWN("unknown", "Status desconhecido");

    private final String code;
    private final String description;

    HealthStatus(String code, String description) {
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

