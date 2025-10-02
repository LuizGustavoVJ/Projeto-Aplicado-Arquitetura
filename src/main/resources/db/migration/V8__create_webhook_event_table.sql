-- Migration V8: Criação da tabela webhook_event
-- Autor: Luiz Gustavo Finotello
-- Data: 2025-10-02

CREATE TABLE IF NOT EXISTS webhook_event (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    lojista_id UUID NOT NULL,
    transacao_id UUID NOT NULL,
    evento VARCHAR(50) NOT NULL,
    url VARCHAR(500) NOT NULL,
    payload TEXT NOT NULL,
    signature VARCHAR(255),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    tentativas INTEGER NOT NULL DEFAULT 0,
    max_tentativas INTEGER NOT NULL DEFAULT 5,
    http_status INTEGER,
    response_body TEXT,
    error_message VARCHAR(500),
    proxima_tentativa TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE,
    enviado_at TIMESTAMP WITH TIME ZONE,
    sucesso_at TIMESTAMP WITH TIME ZONE,
    
    CONSTRAINT fk_webhook_event_lojista FOREIGN KEY (lojista_id) REFERENCES lojista(id) ON DELETE CASCADE,
    CONSTRAINT fk_webhook_event_transacao FOREIGN KEY (transacao_id) REFERENCES transacao(id) ON DELETE CASCADE,
    CONSTRAINT chk_webhook_event_status CHECK (status IN ('PENDING', 'SENDING', 'SUCCESS', 'FAILED', 'CANCELLED')),
    CONSTRAINT chk_webhook_event_tentativas CHECK (tentativas >= 0),
    CONSTRAINT chk_webhook_event_max_tentativas CHECK (max_tentativas > 0)
);

-- Índices para otimização de consultas
CREATE INDEX idx_webhook_event_lojista ON webhook_event(lojista_id);
CREATE INDEX idx_webhook_event_transacao ON webhook_event(transacao_id);
CREATE INDEX idx_webhook_event_status ON webhook_event(status);
CREATE INDEX idx_webhook_event_proxima_tentativa ON webhook_event(proxima_tentativa);
CREATE INDEX idx_webhook_event_created_at ON webhook_event(created_at);

-- Comentários da tabela
COMMENT ON TABLE webhook_event IS 'Tabela que armazena eventos individuais de webhook a serem enviados';
COMMENT ON COLUMN webhook_event.id IS 'Identificador único do evento de webhook';
COMMENT ON COLUMN webhook_event.lojista_id IS 'Referência ao lojista destinatário';
COMMENT ON COLUMN webhook_event.transacao_id IS 'Referência à transação que gerou o evento';
COMMENT ON COLUMN webhook_event.evento IS 'Tipo do evento (ex: TRANSACTION_AUTHORIZED, TRANSACTION_CAPTURED)';
COMMENT ON COLUMN webhook_event.url IS 'URL de destino do webhook';
COMMENT ON COLUMN webhook_event.payload IS 'Payload JSON do webhook';
COMMENT ON COLUMN webhook_event.signature IS 'Assinatura HMAC-SHA256 do payload';
COMMENT ON COLUMN webhook_event.status IS 'Status: PENDING, SENDING, SUCCESS, FAILED, CANCELLED';
COMMENT ON COLUMN webhook_event.tentativas IS 'Número de tentativas de envio realizadas';
COMMENT ON COLUMN webhook_event.max_tentativas IS 'Número máximo de tentativas permitidas';
COMMENT ON COLUMN webhook_event.proxima_tentativa IS 'Data/hora da próxima tentativa de envio (com backoff exponencial)';
