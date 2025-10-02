-- Migration V4: Criação da tabela transacao
-- Autor: Luiz Gustavo Finotello
-- Data: 2025-10-02

CREATE TABLE IF NOT EXISTS transacao (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    lojista_id UUID NOT NULL,
    gateway_id UUID,
    transaction_id VARCHAR(100) NOT NULL UNIQUE,
    gateway_transaction_id VARCHAR(100),
    tipo VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    valor BIGINT NOT NULL,
    moeda VARCHAR(3) NOT NULL DEFAULT 'BRL',
    parcelas INTEGER NOT NULL DEFAULT 1,
    card_token VARCHAR(100),
    card_brand VARCHAR(50),
    card_last_digits VARCHAR(4),
    customer_name VARCHAR(200),
    customer_email VARCHAR(100),
    customer_document VARCHAR(20),
    description VARCHAR(500),
    metadata TEXT,
    authorization_code VARCHAR(50),
    nsu VARCHAR(50),
    tid VARCHAR(50),
    error_code VARCHAR(50),
    error_message VARCHAR(500),
    gateway_response TEXT,
    ip_address VARCHAR(45),
    user_agent VARCHAR(500),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE,
    authorized_at TIMESTAMP WITH TIME ZONE,
    captured_at TIMESTAMP WITH TIME ZONE,
    voided_at TIMESTAMP WITH TIME ZONE,
    
    CONSTRAINT fk_transacao_lojista FOREIGN KEY (lojista_id) REFERENCES lojista(id) ON DELETE CASCADE,
    CONSTRAINT fk_transacao_gateway FOREIGN KEY (gateway_id) REFERENCES gateway(id) ON DELETE SET NULL,
    CONSTRAINT chk_transacao_tipo CHECK (tipo IN ('AUTHORIZATION', 'CAPTURE', 'VOID', 'REFUND')),
    CONSTRAINT chk_transacao_status CHECK (status IN ('PENDING', 'AUTHORIZED', 'CAPTURED', 'VOIDED', 'REFUNDED', 'FAILED', 'EXPIRED')),
    CONSTRAINT chk_transacao_valor CHECK (valor > 0),
    CONSTRAINT chk_transacao_moeda CHECK (moeda IN ('BRL', 'USD', 'EUR')),
    CONSTRAINT chk_transacao_parcelas CHECK (parcelas >= 1)
);

-- Índices para otimização de consultas
CREATE INDEX idx_transacao_transaction_id ON transacao(transaction_id);
CREATE INDEX idx_transacao_lojista ON transacao(lojista_id);
CREATE INDEX idx_transacao_gateway ON transacao(gateway_id);
CREATE INDEX idx_transacao_status ON transacao(status);
CREATE INDEX idx_transacao_tipo ON transacao(tipo);
CREATE INDEX idx_transacao_created_at ON transacao(created_at);
CREATE INDEX idx_transacao_gateway_transaction_id ON transacao(gateway_transaction_id);
CREATE INDEX idx_transacao_card_token ON transacao(card_token);

-- Comentários da tabela
COMMENT ON TABLE transacao IS 'Tabela que armazena todas as transações de pagamento';
COMMENT ON COLUMN transacao.id IS 'Identificador único da transação';
COMMENT ON COLUMN transacao.lojista_id IS 'Referência ao lojista proprietário da transação';
COMMENT ON COLUMN transacao.gateway_id IS 'Referência ao gateway que processou a transação';
COMMENT ON COLUMN transacao.transaction_id IS 'ID único da transação no PIP';
COMMENT ON COLUMN transacao.gateway_transaction_id IS 'ID da transação no gateway';
COMMENT ON COLUMN transacao.tipo IS 'Tipo da transação: AUTHORIZATION, CAPTURE, VOID, REFUND';
COMMENT ON COLUMN transacao.status IS 'Status: PENDING, AUTHORIZED, CAPTURED, VOIDED, REFUNDED, FAILED, EXPIRED';
COMMENT ON COLUMN transacao.valor IS 'Valor da transação em centavos';
COMMENT ON COLUMN transacao.moeda IS 'Código da moeda (ISO 4217)';
COMMENT ON COLUMN transacao.parcelas IS 'Número de parcelas';
COMMENT ON COLUMN transacao.card_token IS 'Token do cartão tokenizado';
COMMENT ON COLUMN transacao.authorization_code IS 'Código de autorização do gateway';
COMMENT ON COLUMN transacao.nsu IS 'Número Sequencial Único';
COMMENT ON COLUMN transacao.tid IS 'Transaction ID do adquirente';
