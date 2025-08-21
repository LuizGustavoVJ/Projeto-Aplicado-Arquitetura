# Payment Integration Platform (PIP)

## Descrição

A Payment Integration Platform (PIP) é uma plataforma de integração de pagamentos desenvolvida como projeto aplicado para o curso de Pós-Graduação em Arquitetura de Software e Soluções.

## Autor

**Luiz Gustavo Finotello**

## Tecnologias Utilizadas

- **Backend:** Java 17 com Spring Boot 3
- **Banco de Dados:** PostgreSQL 15
- **Documentação da API:** Swagger/OpenAPI 3.0
- **Testes:** JUnit 5, Mockito
- **Build:** Maven

## Estrutura do Projeto

```
src/
├── main/
│   ├── java/com/pip/
│   │   ├── controller/     # Controllers da API REST
│   │   ├── service/        # Lógica de negócio
│   │   ├── model/          # Entidades JPA
│   │   ├── repository/     # Repositórios de dados
│   │   ├── dto/            # Data Transfer Objects
│   │   └── PipApplication.java
│   └── resources/
│       ├── application.properties
│       └── api-spec.yaml   # Especificação OpenAPI
└── test/
    └── java/               # Testes unitários
```

## Como Executar

1. Certifique-se de ter o Java 17 e Maven instalados
2. Configure um banco PostgreSQL local
3. Execute: `mvn spring-boot:run`
4. Acesse a documentação da API em: `http://localhost:8080/swagger-ui.html`

## Endpoints Principais

- `POST /v1/payments/authorize` - Autorizar pagamento
- `POST /v1/payments/{id}/capture` - Capturar pagamento
- `POST /v1/payments/{id}/void` - Cancelar pagamento
- `GET /v1/payments/{id}` - Consultar pagamento

## Status do Projeto

Este projeto está sendo desenvolvido em 3 sprints:
- **Sprint 01:** ✅ Fundação e desenho arquitetural
- **Sprint 02:** ✅ Detalhamento técnico e contratos de API
- **Sprint 03:** 🚧 Prova de conceito e integração (em desenvolvimento)

