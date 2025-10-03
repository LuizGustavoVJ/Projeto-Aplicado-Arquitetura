-- Otimização de índices para melhor performance
-- Payment Integration Platform - Database Optimization

-- Índices para tabela de transações
CREATE INDEX IF NOT EXISTS idx_transacao_status ON transacao(status);
CREATE INDEX IF NOT EXISTS idx_transacao_gateway ON transacao(gateway_id);
CREATE INDEX IF NOT EXISTS idx_transacao_lojista ON transacao(lojista_id);
CREATE INDEX IF NOT EXISTS idx_transacao_created_at ON transacao(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_transacao_status_created ON transacao(status, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_transacao_gateway_status ON transacao(gateway_id, status);

-- Índice composto para queries de busca por período e status
CREATE INDEX IF NOT EXISTS idx_transacao_search ON transacao(lojista_id, status, created_at DESC);

-- Índice para busca por transaction_id (usado em callbacks de gateway)
CREATE INDEX IF NOT EXISTS idx_transacao_transaction_id ON transacao(transaction_id);

-- Índices para tabela de webhooks
CREATE INDEX IF NOT EXISTS idx_webhook_lojista ON webhook(lojista_id);
CREATE INDEX IF NOT EXISTS idx_webhook_event_type ON webhook_event(event_type);
CREATE INDEX IF NOT EXISTS idx_webhook_event_status ON webhook_event(status);
CREATE INDEX IF NOT EXISTS idx_webhook_event_created ON webhook_event(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_webhook_event_next_retry ON webhook_event(next_retry_at) WHERE status = 'PENDING';

-- Índices para tabela de API keys
CREATE INDEX IF NOT EXISTS idx_api_key_lojista ON api_key(lojista_id);
CREATE INDEX IF NOT EXISTS idx_api_key_active ON api_key(active) WHERE active = true;
CREATE INDEX IF NOT EXISTS idx_api_key_key_hash ON api_key(key_hash);

-- Índices para tabela de logs de transação
CREATE INDEX IF NOT EXISTS idx_log_transacao_transacao_id ON log_transacao(transacao_id);
CREATE INDEX IF NOT EXISTS idx_log_transacao_created ON log_transacao(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_log_transacao_level ON log_transacao(log_level);

-- Índices para tabela de gateways
CREATE INDEX IF NOT EXISTS idx_gateway_active ON gateway(active) WHERE active = true;
CREATE INDEX IF NOT EXISTS idx_gateway_priority ON gateway(priority DESC);

-- Índices para tabela de lojistas
CREATE INDEX IF NOT EXISTS idx_lojista_active ON lojista(active) WHERE active = true;
CREATE INDEX IF NOT EXISTS idx_lojista_cnpj ON lojista(cnpj);

-- Estatísticas para o otimizador de queries
ANALYZE transacao;
ANALYZE webhook;
ANALYZE webhook_event;
ANALYZE api_key;
ANALYZE log_transacao;
ANALYZE gateway;
ANALYZE lojista;

-- Comentários para documentação
COMMENT ON INDEX idx_transacao_search IS 'Índice otimizado para queries de busca de transações por lojista, status e período';
COMMENT ON INDEX idx_webhook_event_next_retry IS 'Índice parcial para otimizar busca de webhooks pendentes de retry';
COMMENT ON INDEX idx_api_key_active IS 'Índice parcial para busca rápida de API keys ativas';
