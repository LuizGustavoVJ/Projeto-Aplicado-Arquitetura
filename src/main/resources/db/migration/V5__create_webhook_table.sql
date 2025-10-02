-- Migration V5: Criação da tabela webhook
-- Autor: Luiz Gustavo Finotello
-- Data: 2025-10-02

CREATE TABLE IF NOT EXISTS webhook (
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
    
    CONSTRAINT fk_webhook_lojista FOREIGN KEY (lojista_id) REFERENCES lojista(id) ON DELETE CASCADE,
    CONSTRAINT fk_webhook_transacao FOREIGN KEY (transacao_id) REFERENCES transacao(id) ON DELETE CASCADE,
    CONSTRAINT chk_webhook_evento CHECK (evento IN ('TRANSACTION_AUTHORIZED', 'TRANSACTION_CAPTURED', 'TRANSACTION_VOIDED', 'TRANSACTION_REFUNDED', 'TRANSACTION_FAILED')),
    CONSTRAINT chk_webhook_status CHECK (status IN ('PENDING', 'SENDING', 'SUCCESS', 'FAILED', 'CANCELLED')),
    CONSTRAINT chk_webhook_tentativas CHECK (tentativas >= 0),
    CONSTRAINT chk_webhook_max_tentativas CHECK (max_tentativas > 0)
);

-- Índices para otimização de consultas
CREATE INDEX idx_webhook_lojista ON webhook(lojista_id);
CREATE INDEX idx_webhook_transacao ON webhook(transacao_id);
CREATE INDEX idx_webhook_status ON webhook(status);
CREATE INDEX idx_webhook_evento ON webhook(evento);
CREATE INDEX idx_webhook_created_at ON webhook(created_at);
CREATE INDEX idx_webhook_proxima_tentativa ON webhook(proxima_tentativa);

-- Comentários da tabela
COMMENT ON TABLE webhook IS 'Tabela que armazena webhooks para notificação de eventos';
COMMENT ON COLUMN webhook.id IS 'Identificador único do webhook';
COMMENT ON COLUMN webhook.lojista_id IS 'Referência ao lojista destinatário do webhook';
COMMENT ON COLUMN webhook.transacao_id IS 'Referência à transação que gerou o evento';
COMMENT ON COLUMN webhook.evento IS 'Tipo do evento: TRANSACTION_AUTHORIZED, TRANSACTION_CAPTURED, etc.';
COMMENT ON COLUMN webhook.url IS 'URL de destino do webhook';
COMMENT ON COLUMN webhook.payload IS 'Payload JSON do webhook';
COMMENT ON COLUMN webhook.signature IS 'Assinatura HMAC-SHA256 do payload';
COMMENT ON COLUMN webhook.status IS 'Status: PENDING, SENDING, SUCCESS, FAILED, CANCELLED';
COMMENT ON COLUMN webhook.tentativas IS 'Número de tentativas de envio realizadas';
COMMENT ON COLUMN webhook.max_tentativas IS 'Número máximo de tentativas permitidas';
COMMENT ON COLUMN webhook.http_status IS 'Código HTTP da última resposta';
COMMENT ON COLUMN webhook.proxima_tentativa IS 'Data/hora da próxima tentativa de envio';
