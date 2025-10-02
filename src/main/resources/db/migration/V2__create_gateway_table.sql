-- Migration V2: Criação da tabela gateway
-- Autor: Luiz Gustavo Finotello
-- Data: 2025-10-02

CREATE TABLE IF NOT EXISTS gateway (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    codigo VARCHAR(50) NOT NULL UNIQUE,
    nome VARCHAR(100) NOT NULL,
    descricao VARCHAR(500),
    tipo VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'INACTIVE',
    url_base VARCHAR(500) NOT NULL,
    url_sandbox VARCHAR(500),
    api_key VARCHAR(500),
    api_secret VARCHAR(500),
    merchant_id VARCHAR(100),
    prioridade INTEGER NOT NULL,
    peso_roteamento INTEGER NOT NULL,
    taxa_sucesso DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    tempo_resposta_medio BIGINT NOT NULL DEFAULT 0,
    limite_diario BIGINT NOT NULL,
    volume_processado_hoje BIGINT NOT NULL DEFAULT 0,
    total_transacoes BIGINT NOT NULL DEFAULT 0,
    total_sucesso BIGINT NOT NULL DEFAULT 0,
    total_falhas BIGINT NOT NULL DEFAULT 0,
    suporta_captura BOOLEAN NOT NULL DEFAULT TRUE,
    suporta_cancelamento BOOLEAN NOT NULL DEFAULT TRUE,
    suporta_estorno BOOLEAN NOT NULL DEFAULT TRUE,
    suporta_parcelamento BOOLEAN NOT NULL DEFAULT FALSE,
    max_parcelas INTEGER NOT NULL DEFAULT 1,
    timeout_conexao INTEGER NOT NULL DEFAULT 30000,
    timeout_leitura INTEGER NOT NULL DEFAULT 60000,
    max_tentativas INTEGER NOT NULL DEFAULT 3,
    intervalo_retry INTEGER NOT NULL DEFAULT 1000,
    ambiente VARCHAR(20) NOT NULL DEFAULT 'SANDBOX',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE,
    last_health_check TIMESTAMP WITH TIME ZONE,
    health_status VARCHAR(20) DEFAULT 'UNKNOWN',
    
    CONSTRAINT chk_gateway_tipo CHECK (tipo IN ('ACQUIRER', 'SUBACQUIRER', 'FACILITATOR', 'WALLET')),
    CONSTRAINT chk_gateway_status CHECK (status IN ('ACTIVE', 'INACTIVE', 'MAINTENANCE')),
    CONSTRAINT chk_gateway_ambiente CHECK (ambiente IN ('SANDBOX', 'PRODUCTION')),
    CONSTRAINT chk_gateway_health_status CHECK (health_status IN ('UP', 'DOWN', 'DEGRADED', 'UNKNOWN')),
    CONSTRAINT chk_gateway_prioridade CHECK (prioridade BETWEEN 1 AND 100),
    CONSTRAINT chk_gateway_peso_roteamento CHECK (peso_roteamento BETWEEN 0 AND 100),
    CONSTRAINT chk_gateway_taxa_sucesso CHECK (taxa_sucesso BETWEEN 0 AND 100),
    CONSTRAINT chk_gateway_limite_diario CHECK (limite_diario > 0),
    CONSTRAINT chk_gateway_volume_processado_hoje CHECK (volume_processado_hoje >= 0),
    CONSTRAINT chk_gateway_max_parcelas CHECK (max_parcelas >= 1),
    CONSTRAINT chk_gateway_timeout_conexao CHECK (timeout_conexao > 0),
    CONSTRAINT chk_gateway_timeout_leitura CHECK (timeout_leitura > 0),
    CONSTRAINT chk_gateway_max_tentativas CHECK (max_tentativas >= 1),
    CONSTRAINT chk_gateway_intervalo_retry CHECK (intervalo_retry >= 0)
);

-- Índices para otimização de consultas
CREATE INDEX idx_gateway_codigo ON gateway(codigo);
CREATE INDEX idx_gateway_status ON gateway(status);
CREATE INDEX idx_gateway_prioridade ON gateway(prioridade);
CREATE INDEX idx_gateway_tipo ON gateway(tipo);
CREATE INDEX idx_gateway_ambiente ON gateway(ambiente);
CREATE INDEX idx_gateway_health_status ON gateway(health_status);
CREATE INDEX idx_gateway_taxa_sucesso ON gateway(taxa_sucesso);

-- Comentários da tabela
COMMENT ON TABLE gateway IS 'Tabela que armazena configurações dos gateways de pagamento';
COMMENT ON COLUMN gateway.id IS 'Identificador único do gateway';
COMMENT ON COLUMN gateway.codigo IS 'Código único do gateway (ex: CIELO, REDE, STONE)';
COMMENT ON COLUMN gateway.tipo IS 'Tipo do gateway: ACQUIRER, SUBACQUIRER, FACILITATOR, WALLET';
COMMENT ON COLUMN gateway.status IS 'Status operacional: ACTIVE, INACTIVE, MAINTENANCE';
COMMENT ON COLUMN gateway.prioridade IS 'Prioridade no roteamento (1-100, menor = maior prioridade)';
COMMENT ON COLUMN gateway.peso_roteamento IS 'Peso para balanceamento de carga (0-100)';
COMMENT ON COLUMN gateway.taxa_sucesso IS 'Taxa de sucesso histórica em percentual (0-100)';
COMMENT ON COLUMN gateway.tempo_resposta_medio IS 'Tempo médio de resposta em milissegundos';
COMMENT ON COLUMN gateway.limite_diario IS 'Limite diário de processamento em centavos';
COMMENT ON COLUMN gateway.volume_processado_hoje IS 'Volume processado hoje em centavos';
COMMENT ON COLUMN gateway.health_status IS 'Status de saúde: UP, DOWN, DEGRADED, UNKNOWN';
COMMENT ON COLUMN gateway.ambiente IS 'Ambiente de operação: SANDBOX ou PRODUCTION';
