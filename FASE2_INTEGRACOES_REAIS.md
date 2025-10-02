# Payment Integration Platform (PIP) - Fase 2: Integrações Reais

## Visão Geral

A Fase 2 transforma o PIP de um framework funcional em uma plataforma de pagamentos **pronta para produção** com integrações reais de 9 gateways de pagamento.

## Gateways Implementados

### 1. Stone Pagamentos
**Código:** `STONE`  
**Documentação:** https://online.stone.com.br/reference/overview-da-api  
**Autenticação:** Bearer Token (JWT)  
**Ambientes:**
- Sandbox: `https://payments.stone.com.br/v1/charges` (Host: sdx-payments.stone.com.br)
- Produção: `https://payments.stone.com.br/v1/charges` (Host: payments.stone.com.br)

**Características:**
- Valores em centavos
- Idempotência via X-Stone-Idempotency-Key
- Suporte a parcelamento
- Autorização com captura posterior
- Cancelamento total e parcial

**Operações:**
- `POST /v1/charges` - Autorização
- `POST /v1/charges/{id}/capture` - Captura
- `POST /v1/charges/{id}/cancel` - Cancelamento

---

### 2. Cielo E-Commerce
**Código:** `CIELO`  
**Documentação:** https://desenvolvedores.cielo.com.br/api-portal/pt-br/product/e-commerce/api  
**Autenticação:** MerchantId + MerchantKey  
**Ambientes:**
- Sandbox: `https://apisandbox.cieloecommerce.cielo.com.br`
- Produção: `https://api.cieloecommerce.cielo.com.br`

**Características:**
- PCI-DSS Level 1 compliant
- 3DS 2.0 support
- Antifraude integrado
- Tokenização de cartões
- Valores em centavos

**Operações:**
- `POST /1/sales` - Criar transação
- `PUT /1/sales/{id}/capture` - Capturar
- `PUT /1/sales/{id}/void` - Cancelar

---

### 3. Rede E-Commerce
**Código:** `REDE`  
**Documentação:** https://developer.userede.com.br  
**Autenticação:** Basic Auth  
**Ambientes:**
- Sandbox: `https://api-sandbox.userede.com.br/erede`
- Produção: `https://api.userede.com.br/erede`

**Características:**
- Suporte a múltiplas bandeiras
- Parcelamento com e sem juros
- Captura automática ou manual
- Cancelamento total e parcial

---

### 4. PagSeguro
**Código:** `PAGSEGURO`  
**Documentação:** https://developers.international.pagseguro.com  
**Autenticação:** Bearer Token  
**Ambientes:**
- Sandbox: `https://sandbox.api.pagseguro.com`
- Produção: `https://api.pagseguro.com`

**Características:**
- Split de pagamentos
- Antifraude integrado
- Múltiplos métodos de pagamento
- Checkout transparente

---

### 5. Mercado Pago
**Código:** `MERCADOPAGO`  
**Documentação:** https://www.mercadopago.com.br/developers  
**Autenticação:** Bearer Token (Access Token)  
**Ambientes:**
- Sandbox: `https://api.mercadopago.com` (modo sandbox via header)
- Produção: `https://api.mercadopago.com`

**Características:**
- Marketplace support
- Parcelamento inteligente
- Checkout Pro e API
- Webhooks nativos
- Split de pagamentos

---

### 6. Visa Direct
**Código:** `VISA`  
**Documentação:** https://developer.visa.com/capabilities/visa_direct  
**Autenticação:** Certificado mTLS  
**Ambientes:**
- Sandbox: `https://sandbox.api.visa.com/visadirect`
- Produção: `https://api.visa.com/visadirect`

**Características:**
- Transferências P2P
- Push e Pull de fundos
- Processamento em tempo real
- Cobertura global

---

### 7. Mastercard Payment Gateway Services
**Código:** `MASTERCARD`  
**Documentação:** https://developer.mastercard.com/product/payment-gateway-services-mpgs  
**Autenticação:** OAuth 2.0  
**Ambientes:**
- Sandbox: `https://sandbox.api.mastercard.com/send`
- Produção: `https://api.mastercard.com/send`

**Características:**
- Mastercard Send
- Tokenização segura
- 3DS 2.0
- Múltiplas moedas

---

### 8. PIX (Banco Central do Brasil)
**Código:** `PIX`  
**Documentação:** https://www.bcb.gov.br/estabilidadefinanceira/pix  
**Autenticação:** Certificado Digital  
**Ambientes:**
- Homologação: `https://pix-h.bcb.gov.br`
- Produção: `https://pix.bcb.gov.br`

**Características:**
- Pagamento instantâneo 24/7
- QR Code estático e dinâmico
- PIX Copia e Cola
- Devolução automática
- Webhooks de notificação

---

### 9. Boleto Bancário
**Código:** `BOLETO`  
**Documentação:** https://developers.bb.com.br  
**Autenticação:** OAuth 2.0  
**Ambientes:**
- Sandbox: `https://sandbox.api.bb.com.br/cobrancas/v2`
- Produção: `https://api.bb.com.br/cobrancas/v2`

**Características:**
- Geração de boleto com código de barras
- Linha digitável
- Registro automático
- Notificação de pagamento
- Vencimento configurável

---

## Arquitetura de Segurança

### 1. Criptografia
- **TLS 1.2+** obrigatório para todas as comunicações
- **Certificados digitais** validados
- **mTLS** para Visa e PIX
- **Dados sensíveis** nunca em logs

### 2. Autenticação
- **Bearer Tokens** com expiração
- **API Keys** com hash SHA-256
- **OAuth 2.0** para Mastercard e Boleto
- **Certificados** para Visa e PIX

### 3. Validação
- **Input sanitization** em todos os endpoints
- **Validação de valores** (min/max)
- **Validação de formato** (CPF, email, etc)
- **Rate limiting** por gateway

### 4. Auditoria
- **Logs completos** de todas as transações
- **Sanitização** de dados sensíveis nos logs
- **Rastreamento** de tentativas falhadas
- **Alertas** de atividades suspeitas

### 5. Conformidade PCI-DSS
- **Tokenização** obrigatória de cartões
- **Sem armazenamento** de dados de cartão
- **Segregação** de ambientes
- **Criptografia** end-to-end

---

## Fluxo de Transação

### 1. Autorização
```
Cliente → PIP → GatewayRoutingService → GatewayAdapter → Gateway Externo
                                                              ↓
Cliente ← PIP ← PaymentResponse ← GatewayAdapter ← Gateway Externo
```

### 2. Captura
```
Cliente → PIP → PagamentoService → GatewayAdapter → Gateway Externo
                                                        ↓
Cliente ← PIP ← PaymentResponse ← GatewayAdapter ← Gateway Externo
```

### 3. Cancelamento
```
Cliente → PIP → PagamentoService → GatewayAdapter → Gateway Externo
                                                        ↓
Cliente ← PIP ← PaymentResponse ← GatewayAdapter ← Gateway Externo
```

---

## Configuração de Gateways

### Exemplo: Stone Sandbox
```sql
INSERT INTO gateway (nome, codigo, tipo, status, ambiente, api_url, merchant_id, merchant_key, prioridade, taxa_sucesso, tempo_resposta_medio)
VALUES (
    'Stone Pagamentos',
    'STONE',
    'CREDIT_CARD',
    'ATIVO',
    'SANDBOX',
    'https://payments.stone.com.br/v1/charges',
    'seu-merchant-id',
    'seu-bearer-token',
    1,
    99.5,
    250
);
```

### Exemplo: Cielo Sandbox
```sql
INSERT INTO gateway (nome, codigo, tipo, status, ambiente, api_url, merchant_id, merchant_key, prioridade, taxa_sucesso, tempo_resposta_medio)
VALUES (
    'Cielo E-Commerce',
    'CIELO',
    'CREDIT_CARD',
    'ATIVO',
    'SANDBOX',
    'https://apisandbox.cieloecommerce.cielo.com.br',
    'seu-merchant-id',
    'seu-merchant-key',
    2,
    99.8,
    200
);
```

### Exemplo: PIX
```sql
INSERT INTO gateway (nome, codigo, tipo, status, ambiente, api_url, pix_key, prioridade, taxa_sucesso, tempo_resposta_medio)
VALUES (
    'PIX Banco Central',
    'PIX',
    'PIX',
    'ATIVO',
    'SANDBOX',
    'https://pix-h.bcb.gov.br',
    'sua-chave-pix',
    1,
    99.9,
    100
);
```

---

## Testes

### Cartões de Teste - Stone Sandbox
Os valores em centavos determinam o código de resposta:
- R$ 1,00 (100 centavos) → Aprovado (0000)
- R$ 1,01 (101 centavos) → Negado (1007)
- R$ 1,05 (105 centavos) → Timeout

### Cartões de Teste - Cielo Sandbox
```
Visa: 4024007197692931
Mastercard: 5404434242930107
Elo: 6362970000457013
```

### PIX de Teste
```
Chave PIX: teste@pix.com.br
Valor: Qualquer valor acima de R$ 0,01
```

---

## Monitoramento

### Métricas por Gateway
- Taxa de sucesso
- Tempo médio de resposta
- Disponibilidade (uptime)
- Taxa de erro por tipo

### Alertas
- Taxa de sucesso < 95%
- Tempo de resposta > 5s
- Gateway indisponível
- Tentativas de fraude

---

## Roadmap Futuro

### Fase 3: Autenticação e Processamento Assíncrono
- OAuth 2.0 para clientes
- JWT tokens
- Processamento assíncrono de transações
- Filas de prioridade

### Fase 4: Analytics e Inteligência
- Dashboard de métricas
- Machine Learning para detecção de fraude
- Otimização automática de roteamento
- Predição de falhas

---

## Suporte

Para dúvidas ou problemas:
- Email: suporte@pip.com.br
- Documentação: https://docs.pip.com.br
- Status: https://status.pip.com.br

---

**Autor:** Luiz Gustavo Finotello  
**Versão:** 2.0  
**Data:** Outubro 2025  
**Licença:** Proprietária
