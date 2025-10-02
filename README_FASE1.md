# Payment Integration Platform (PIP) - Fase 1

## Visão Geral

Este documento descreve a implementação da **Fase 1: Core Functionality** do PIP, desenvolvida ao longo de 4 semanas seguindo o roadmap estabelecido.

**Autor:** Luiz Gustavo Finotello

## Estrutura da Fase 1

### Semana 1: Endpoints Básicos ✅
- Implementação dos endpoints REST para pagamentos
- POST /api/payments/authorize - Autorização de pagamento
- POST /api/payments/capture - Captura de pagamento
- POST /api/payments/void - Cancelamento de pagamento
- GET /api/payments/{id} - Consulta de transação

### Semana 2: Entidades Core ✅
- **Lojista**: Entidade para clientes da plataforma
- **Gateway**: Entidade para gateways de pagamento
- **ApiKey**: Entidade para chaves de autenticação
- **Webhook**: Entidade para configuração de webhooks
- **WebhookEvent**: Entidade para eventos de webhook
- **LogTransacao**: Entidade para auditoria
- **Migrations Flyway**: Controle de versão do schema (V1 a V8)

### Semana 3: Roteamento Inteligente e Resiliência ✅
- **GatewayRoutingService**: Algoritmo de seleção baseado em múltiplos critérios
  - Taxa de sucesso (40%)
  - Tempo de resposta (30%)
  - Prioridade configurada (20%)
  - Capacidade disponível (10%)
- **GatewayIntegrationService**: Integração com padrões de resiliência
  - Circuit Breaker
  - Retry com backoff exponencial
  - Fallback automático
  - Timeout configurável
- **GatewayHealthCheckService**: Monitoramento periódico
  - Health check a cada 1 minuto
  - Rebalanceamento a cada 1 hora
  - Reset de volumes diários

### Semana 4: Sistema de Webhooks ✅
- **WebhookService**: Criação e envio de webhooks
  - Assinatura HMAC-SHA256
  - Retry com backoff exponencial
  - Registro de tentativas
- **WebhookScheduler**: Processamento periódico
  - Envio de pendentes (30s)
  - Retry de falhados (1min)
  - Limpeza de antigos (diária)
  - Relatório de falhas (horária)

## Tecnologias Utilizadas

- **Spring Boot 3.2.0**: Framework principal
- **Java 17**: Linguagem de programação
- **PostgreSQL**: Banco de dados relacional
- **Flyway**: Migrations de banco de dados
- **Resilience4j**: Padrões de resiliência
- **Spring Data JPA**: Persistência de dados
- **Jackson**: Serialização JSON
- **JUnit 5 + Mockito**: Testes unitários

## Pré-requisitos

- Java 17 ou superior
- Maven 3.8+
- PostgreSQL 14+
- Docker e Docker Compose (opcional)

## Configuração do Banco de Dados

### Opção 1: PostgreSQL Local

```bash
# Criar banco de dados
createdb pip_db

# Criar usuário
psql -c "CREATE USER pip_user WITH PASSWORD 'pip_password';"
psql -c "GRANT ALL PRIVILEGES ON DATABASE pip_db TO pip_user;"
```

### Opção 2: Docker Compose

```bash
# Criar arquivo docker-compose.yml
cat > docker-compose.yml << 'EOF'
version: '3.8'

services:
  postgres:
    image: postgres:14
    container_name: pip_postgres
    environment:
      POSTGRES_DB: pip_db
      POSTGRES_USER: pip_user
      POSTGRES_PASSWORD: pip_password
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data

volumes:
  postgres_data:
EOF

# Iniciar PostgreSQL
docker-compose up -d
```

## Executando a Aplicação

### 1. Compilar o projeto

```bash
mvn clean install
```

### 2. Executar testes

```bash
mvn test
```

### 3. Executar a aplicação

```bash
mvn spring-boot:run
```

A aplicação estará disponível em: `http://localhost:8080`

## Documentação da API

### Swagger UI

Acesse: `http://localhost:8080/swagger-ui.html`

### OpenAPI Spec

Acesse: `http://localhost:8080/api-docs`

## Estrutura do Projeto

```
src/
├── main/
│   ├── java/com/pip/
│   │   ├── config/              # Configurações
│   │   │   ├── JacksonConfig.java
│   │   │   ├── RestTemplateConfig.java
│   │   │   └── SchedulingConfig.java
│   │   ├── controller/          # Controllers REST
│   │   │   └── PagamentoController.java
│   │   ├── dto/                 # Data Transfer Objects
│   │   │   ├── AuthorizationRequest.java
│   │   │   ├── CaptureRequest.java
│   │   │   ├── VoidRequest.java
│   │   │   └── PaymentResponse.java
│   │   ├── model/               # Entidades JPA
│   │   │   ├── Lojista.java
│   │   │   ├── Gateway.java
│   │   │   ├── ApiKey.java
│   │   │   ├── Webhook.java
│   │   │   ├── WebhookEvent.java
│   │   │   ├── Transacao.java
│   │   │   └── LogTransacao.java
│   │   ├── repository/          # Repositories JPA
│   │   │   ├── LojistaRepository.java
│   │   │   ├── GatewayRepository.java
│   │   │   ├── ApiKeyRepository.java
│   │   │   ├── WebhookRepository.java
│   │   │   ├── WebhookEventRepository.java
│   │   │   ├── TransacaoRepository.java
│   │   │   └── LogTransacaoRepository.java
│   │   └── service/             # Serviços de negócio
│   │       ├── PagamentoService.java
│   │       ├── GatewayRoutingService.java
│   │       ├── GatewayIntegrationService.java
│   │       ├── GatewayHealthCheckService.java
│   │       ├── WebhookService.java
│   │       └── WebhookScheduler.java
│   └── resources/
│       ├── application.properties
│       ├── application-resilience.yml
│       └── db/migration/        # Migrations Flyway
│           ├── V1__create_lojista_table.sql
│           ├── V2__create_gateway_table.sql
│           ├── V3__create_api_key_table.sql
│           ├── V4__create_transacao_table.sql
│           ├── V5__create_webhook_table.sql
│           ├── V6__create_log_transacao_table.sql
│           ├── V7__insert_initial_gateways.sql
│           └── V8__create_webhook_event_table.sql
└── test/
    └── java/com/pip/service/
        ├── GatewayRoutingServiceTest.java
        └── WebhookServiceTest.java
```

## Testando a API

### 1. Autorizar Pagamento

```bash
curl -X POST http://localhost:8080/api/payments/authorize \
  -H "Content-Type: application/json" \
  -d '{
    "amount": 10000,
    "currency": "BRL",
    "card_token": "tok_test_123",
    "installments": 1,
    "description": "Compra teste",
    "customer": {
      "name": "João Silva",
      "email": "joao@email.com",
      "document": "12345678900"
    }
  }'
```

### 2. Capturar Pagamento

```bash
curl -X POST http://localhost:8080/api/payments/capture \
  -H "Content-Type: application/json" \
  -d '{
    "transaction_id": "TXN-xxx",
    "amount": 10000
  }'
```

### 3. Cancelar Pagamento

```bash
curl -X POST http://localhost:8080/api/payments/void \
  -H "Content-Type: application/json" \
  -d '{
    "transaction_id": "TXN-xxx",
    "reason": "Cancelamento solicitado pelo cliente"
  }'
```

### 4. Consultar Transação

```bash
curl http://localhost:8080/api/payments/TXN-xxx
```

## Monitoramento

### Métricas de Gateways

```bash
curl http://localhost:8080/api/gateways/stats
```

### Health Check

```bash
curl http://localhost:8080/actuator/health
```

## Próximos Passos (Fase 2)

- Autenticação e autorização (API Keys, OAuth2)
- Rate limiting com Redis
- Processamento assíncrono com RabbitMQ
- Dashboard de monitoramento
- Testes de carga e performance

## Commits da Fase 1

1. **Semana 1**: feat: Endpoints básicos de pagamento (authorize, capture, void, get)
2. **Semana 2**: feat: Implementação completa das entidades core e migrations
3. **Semana 3**: feat: Roteamento inteligente e resiliência
4. **Semana 4**: feat: Sistema completo de webhooks

## Licença

Este projeto é parte do Projeto Aplicado da PUC e é de propriedade de Luiz Gustavo Finotello.

## Contato

**Autor:** Luiz Gustavo Finotello  
**Email:** finotello22@hotmail.com  
**GitHub:** https://github.com/LuizGustavoVJ
