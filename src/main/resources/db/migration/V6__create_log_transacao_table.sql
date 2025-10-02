-- Migration V6: Criação da tabela log_transacao
-- Autor: Luiz Gustavo Finotello
-- Data: 2025-10-02

CREATE TABLE IF NOT EXISTS log_transacao (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    transacao_id UUID NOT NULL,
    gateway_id UUID,
    acao VARCHAR(50) NOT NULL,
    status_anterior VARCHAR(20),
    status_novo VARCHAR(20) NOT NULL,
    request_payload TEXT,
    response_payload TEXT,
    http_status INTEGER,
    tempo_resposta BIGINT,
    error_code VARCHAR(50),
    error_message VARCHAR(500),
    ip_address VARCHAR(45),
    user_agent VARCHAR(500),
    metadata TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_log_transacao_transacao FOREIGN KEY (transacao_id) REFERENCES transacao(id) ON DELETE CASCADE,
    CONSTRAINT fk_log_transacao_gateway FOREIGN KEY (gateway_id) REFERENCES gateway(id) ON DELETE SET NULL,
    CONSTRAINT chk_log_transacao_acao CHECK (acao IN ('AUTHORIZATION', 'CAPTURE', 'VOID', 'REFUND', 'QUERY', 'WEBHOOK_SENT', 'ROUTING_DECISION')),
    CONSTRAINT chk_log_transacao_tempo_resposta CHECK (tempo_resposta >= 0)
);

-- Índices para otimização de consultas
CREATE INDEX idx_log_transacao_transacao ON log_transacao(transacao_id);
CREATE INDEX idx_log_transacao_gateway ON log_transacao(gateway_id);
CREATE INDEX idx_log_transacao_acao ON log_transacao(acao);
CREATE INDEX idx_log_transacao_created_at ON log_transacao(created_at);
CREATE INDEX idx_log_transacao_status_novo ON log_transacao(status_novo);

-- Comentários da tabela
COMMENT ON TABLE log_transacao IS 'Tabela que armazena logs detalhados de todas as operações de transação';
COMMENT ON COLUMN log_transacao.id IS 'Identificador único do log';
COMMENT ON COLUMN log_transacao.transacao_id IS 'Referência à transação relacionada';
COMMENT ON COLUMN log_transacao.gateway_id IS 'Referência ao gateway utilizado';
COMMENT ON COLUMN log_transacao.acao IS 'Ação realizada: AUTHORIZATION, CAPTURE, VOID, REFUND, QUERY, WEBHOOK_SENT, ROUTING_DECISION';
COMMENT ON COLUMN log_transacao.status_anterior IS 'Status da transação antes da ação';
COMMENT ON COLUMN log_transacao.status_novo IS 'Status da transação após a ação';
COMMENT ON COLUMN log_transacao.request_payload IS 'Payload da requisição enviada ao gateway';
COMMENT ON COLUMN log_transacao.response_payload IS 'Payload da resposta recebida do gateway';
COMMENT ON COLUMN log_transacao.tempo_resposta IS 'Tempo de resposta em milissegundos';
COMMENT ON COLUMN log_transacao.metadata IS 'Metadados adicionais em formato JSON';
