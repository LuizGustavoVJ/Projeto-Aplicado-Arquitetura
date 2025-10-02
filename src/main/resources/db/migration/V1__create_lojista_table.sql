-- Migration V1: Criação da tabela lojista
-- Autor: Luiz Gustavo Finotello
-- Data: 2025-10-02

CREATE TABLE IF NOT EXISTS lojista (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nome_fantasia VARCHAR(200) NOT NULL,
    razao_social VARCHAR(200) NOT NULL,
    cnpj VARCHAR(14) NOT NULL UNIQUE,
    email VARCHAR(100) NOT NULL UNIQUE,
    telefone VARCHAR(20),
    endereco VARCHAR(500),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    plano VARCHAR(20) NOT NULL,
    webhook_url VARCHAR(500),
    webhook_secret VARCHAR(100),
    limite_mensal BIGINT NOT NULL,
    volume_processado BIGINT NOT NULL DEFAULT 0,
    taxa_percentual INTEGER NOT NULL,
    taxa_fixa BIGINT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE,
    activated_at TIMESTAMP WITH TIME ZONE,
    
    CONSTRAINT chk_lojista_status CHECK (status IN ('PENDING', 'ACTIVE', 'SUSPENDED', 'BLOCKED')),
    CONSTRAINT chk_lojista_plano CHECK (plano IN ('STARTER', 'BUSINESS', 'ENTERPRISE')),
    CONSTRAINT chk_lojista_cnpj_length CHECK (LENGTH(cnpj) = 14),
    CONSTRAINT chk_lojista_limite_mensal CHECK (limite_mensal > 0),
    CONSTRAINT chk_lojista_volume_processado CHECK (volume_processado >= 0),
    CONSTRAINT chk_lojista_taxa_percentual CHECK (taxa_percentual >= 0),
    CONSTRAINT chk_lojista_taxa_fixa CHECK (taxa_fixa >= 0)
);

-- Índices para otimização de consultas
CREATE INDEX idx_lojista_email ON lojista(email);
CREATE INDEX idx_lojista_cnpj ON lojista(cnpj);
CREATE INDEX idx_lojista_status ON lojista(status);
CREATE INDEX idx_lojista_plano ON lojista(plano);
CREATE INDEX idx_lojista_created_at ON lojista(created_at);

-- Comentários da tabela
COMMENT ON TABLE lojista IS 'Tabela que armazena informações dos lojistas (clientes da plataforma PIP)';
COMMENT ON COLUMN lojista.id IS 'Identificador único do lojista';
COMMENT ON COLUMN lojista.nome_fantasia IS 'Nome fantasia da empresa';
COMMENT ON COLUMN lojista.razao_social IS 'Razão social da empresa';
COMMENT ON COLUMN lojista.cnpj IS 'CNPJ da empresa (14 dígitos)';
COMMENT ON COLUMN lojista.email IS 'Email de contato do lojista';
COMMENT ON COLUMN lojista.status IS 'Status do lojista: PENDING, ACTIVE, SUSPENDED, BLOCKED';
COMMENT ON COLUMN lojista.plano IS 'Plano contratado: STARTER, BUSINESS, ENTERPRISE';
COMMENT ON COLUMN lojista.limite_mensal IS 'Limite mensal de processamento em centavos';
COMMENT ON COLUMN lojista.volume_processado IS 'Volume processado no mês atual em centavos';
COMMENT ON COLUMN lojista.taxa_percentual IS 'Taxa percentual em basis points (ex: 250 = 2.5%)';
COMMENT ON COLUMN lojista.taxa_fixa IS 'Taxa fixa por transação em centavos';
