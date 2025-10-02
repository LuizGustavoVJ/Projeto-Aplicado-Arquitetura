-- Migration V3: Criação da tabela api_key
-- Autor: Luiz Gustavo Finotello
-- Data: 2025-10-02

CREATE TABLE IF NOT EXISTS api_key (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    lojista_id UUID NOT NULL,
    nome VARCHAR(100) NOT NULL,
    key_hash VARCHAR(255) NOT NULL UNIQUE,
    key_prefix VARCHAR(20) NOT NULL,
    ambiente VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    escopo VARCHAR(500),
    ip_whitelist VARCHAR(1000),
    rate_limit_per_minute INTEGER NOT NULL DEFAULT 100,
    total_requests BIGINT NOT NULL DEFAULT 0,
    last_used_at TIMESTAMP WITH TIME ZONE,
    last_used_ip VARCHAR(45),
    expires_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE,
    rotated_at TIMESTAMP WITH TIME ZONE,
    
    CONSTRAINT fk_api_key_lojista FOREIGN KEY (lojista_id) REFERENCES lojista(id) ON DELETE CASCADE,
    CONSTRAINT chk_api_key_ambiente CHECK (ambiente IN ('SANDBOX', 'PRODUCTION')),
    CONSTRAINT chk_api_key_status CHECK (status IN ('ACTIVE', 'SUSPENDED', 'REVOKED')),
    CONSTRAINT chk_api_key_rate_limit CHECK (rate_limit_per_minute > 0),
    CONSTRAINT chk_api_key_total_requests CHECK (total_requests >= 0)
);

-- Índices para otimização de consultas
CREATE INDEX idx_api_key_key_hash ON api_key(key_hash);
CREATE INDEX idx_api_key_lojista ON api_key(lojista_id);
CREATE INDEX idx_api_key_status ON api_key(status);
CREATE INDEX idx_api_key_ambiente ON api_key(ambiente);
CREATE INDEX idx_api_key_expires_at ON api_key(expires_at);
CREATE INDEX idx_api_key_last_used_at ON api_key(last_used_at);

-- Comentários da tabela
COMMENT ON TABLE api_key IS 'Tabela que armazena as chaves de API para autenticação dos lojistas';
COMMENT ON COLUMN api_key.id IS 'Identificador único da API Key';
COMMENT ON COLUMN api_key.lojista_id IS 'Referência ao lojista proprietário da chave';
COMMENT ON COLUMN api_key.nome IS 'Nome descritivo da API Key';
COMMENT ON COLUMN api_key.key_hash IS 'Hash SHA-256 da chave para validação';
COMMENT ON COLUMN api_key.key_prefix IS 'Prefixo visível da chave (ex: pip_live_abc...)';
COMMENT ON COLUMN api_key.ambiente IS 'Ambiente da chave: SANDBOX ou PRODUCTION';
COMMENT ON COLUMN api_key.status IS 'Status da chave: ACTIVE, SUSPENDED, REVOKED';
COMMENT ON COLUMN api_key.escopo IS 'Permissões da chave separadas por vírgula';
COMMENT ON COLUMN api_key.ip_whitelist IS 'IPs permitidos separados por vírgula';
COMMENT ON COLUMN api_key.rate_limit_per_minute IS 'Limite de requisições por minuto';
COMMENT ON COLUMN api_key.total_requests IS 'Total de requisições feitas com esta chave';
COMMENT ON COLUMN api_key.expires_at IS 'Data de expiração da chave';
COMMENT ON COLUMN api_key.rotated_at IS 'Data da última rotação da chave';
