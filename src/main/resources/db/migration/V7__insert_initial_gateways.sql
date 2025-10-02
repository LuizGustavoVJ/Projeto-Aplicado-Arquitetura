-- Migration V7: Inserção de gateways iniciais para testes
-- Autor: Luiz Gustavo Finotello
-- Data: 2025-10-02

-- Gateway Mock para testes
INSERT INTO gateway (
    id,
    codigo,
    nome,
    descricao,
    tipo,
    status,
    url_base,
    url_sandbox,
    prioridade,
    peso_roteamento,
    limite_diario,
    ambiente,
    health_status
) VALUES (
    gen_random_uuid(),
    'MOCK',
    'Mock Gateway',
    'Gateway simulado para testes e desenvolvimento',
    'FACILITATOR',
    'ACTIVE',
    'http://localhost:8080/mock',
    'http://localhost:8080/mock',
    1,
    100,
    100000000, -- R$ 1.000.000,00
    'SANDBOX',
    'UP'
);

-- Cielo (exemplo de gateway real - configuração de sandbox)
INSERT INTO gateway (
    id,
    codigo,
    nome,
    descricao,
    tipo,
    status,
    url_base,
    url_sandbox,
    prioridade,
    peso_roteamento,
    limite_diario,
    ambiente,
    health_status,
    suporta_parcelamento,
    max_parcelas
) VALUES (
    gen_random_uuid(),
    'CIELO',
    'Cielo',
    'Gateway Cielo - Adquirente',
    'ACQUIRER',
    'INACTIVE',
    'https://api.cieloecommerce.cielo.com.br',
    'https://apisandbox.cieloecommerce.cielo.com.br',
    2,
    50,
    50000000, -- R$ 500.000,00
    'SANDBOX',
    'UNKNOWN',
    TRUE,
    12
);

-- Rede (exemplo de gateway real - configuração de sandbox)
INSERT INTO gateway (
    id,
    codigo,
    nome,
    descricao,
    tipo,
    status,
    url_base,
    url_sandbox,
    prioridade,
    peso_roteamento,
    limite_diario,
    ambiente,
    health_status,
    suporta_parcelamento,
    max_parcelas
) VALUES (
    gen_random_uuid(),
    'REDE',
    'Rede',
    'Gateway Rede - Adquirente',
    'ACQUIRER',
    'INACTIVE',
    'https://api.userede.com.br',
    'https://sandbox.api.userede.com.br',
    3,
    50,
    50000000, -- R$ 500.000,00
    'SANDBOX',
    'UNKNOWN',
    TRUE,
    12
);

-- Stone (exemplo de gateway real - configuração de sandbox)
INSERT INTO gateway (
    id,
    codigo,
    nome,
    descricao,
    tipo,
    status,
    url_base,
    url_sandbox,
    prioridade,
    peso_roteamento,
    limite_diario,
    ambiente,
    health_status,
    suporta_parcelamento,
    max_parcelas
) VALUES (
    gen_random_uuid(),
    'STONE',
    'Stone',
    'Gateway Stone - Subadquirente',
    'SUBACQUIRER',
    'INACTIVE',
    'https://api.stone.com.br',
    'https://sandbox.api.stone.com.br',
    4,
    30,
    30000000, -- R$ 300.000,00
    'SANDBOX',
    'UNKNOWN',
    TRUE,
    12
);

-- Comentário
COMMENT ON TABLE gateway IS 'Gateways iniciais configurados para ambiente de desenvolvimento e testes';
