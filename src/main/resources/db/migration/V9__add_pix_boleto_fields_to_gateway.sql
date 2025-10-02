-- Migration para adicionar campos específicos de PIX e Boleto na tabela gateway

-- Adicionar campo pix_key para chave PIX
ALTER TABLE gateway ADD COLUMN IF NOT EXISTS pix_key VARCHAR(200);

-- Adicionar campos de banco para boleto
ALTER TABLE gateway ADD COLUMN IF NOT EXISTS agencia VARCHAR(10);
ALTER TABLE gateway ADD COLUMN IF NOT EXISTS conta VARCHAR(20);

-- Comentários
COMMENT ON COLUMN gateway.pix_key IS 'Chave PIX do lojista (CPF, CNPJ, email, telefone ou chave aleatória)';
COMMENT ON COLUMN gateway.agencia IS 'Agência bancária para geração de boletos';
COMMENT ON COLUMN gateway.conta IS 'Conta bancária para geração de boletos';
