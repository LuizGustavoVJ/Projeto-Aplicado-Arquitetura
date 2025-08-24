# Payment Integration Platform (PIP) - API Documentation

## Visão Geral

A API do PIP fornece endpoints para autorização, captura, cancelamento e consulta de pagamentos através de uma interface unificada que abstrai a complexidade de múltiplos gateways.

## Base URL
```
http://localhost:8080/v1
```

## Autenticação
Todas as requisições devem incluir o header:
```
X-API-Key: pip_test_12345
```

## Endpoints Implementados (PoC)

### POST /payments/authorize
Autoriza um novo pagamento.

**Request Body:**
```json
{
  "cardToken": "tkn_test_approved",
  "amount": 150.00,
  "currency": "BRL",
  "merchantId": "merchant_123",
  "orderId": "order_456"
}
```

**Response (200 OK):**
```json
{
  "id": "123e4567-e89b-12d3-a456-426614174000",
  "status": "authorized",
  "amount": 150.00,
  "currency": "BRL",
  "authorizationCode": "AUTH123456",
  "message": "Pagamento autorizado com sucesso",
  "createdAt": "2025-08-21T10:30:00"
}
```

## Cenários de Teste

### Tokens Especiais para Simulação:
- `tkn_test_approved`: Sempre aprovado
- `tkn_test_declined`: Sempre negado (saldo insuficiente)
- `tkn_test_error`: Sempre erro (cartão inválido)
- `tkn_test_premium`: Aprovado para valores altos

### Valores Especiais:
- `<= 0`: Erro de valor inválido
- `> 10000`: Negado por limite excedido
- `100-9999`: Aprovado normalmente

## Status Codes
- `200`: Transação autorizada
- `400`: Erro de validação
- `402`: Pagamento negado
- `502`: Erro do gateway
- `500`: Erro interno

## Próximas Implementações
- POST /payments/{id}/capture
- POST /payments/{id}/void  
- GET /payments/{id}

