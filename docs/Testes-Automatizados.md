# Testes Automatizados - Payment Integration Platform

**Data:** 03/10/2025  
**Versão:** 1.0.0  
**Status:** ✅ Completo

---

## 📋 Visão Geral

Implementação completa de testes automatizados em 3 níveis:

1. **Testes de Integração End-to-End** - RestAssured + Testcontainers
2. **Testes de Stress/Carga** - Gatling
3. **Testes de Cenários BDD** - Cucumber

---

## 🧪 1. Testes de Integração End-to-End

### Arquivo
`src/test/java/com/pip/integration/PaymentFlowIntegrationTest.java`

### Tecnologias
- **RestAssured** - Testes de API REST
- **Testcontainers** - PostgreSQL real em container
- **JUnit 5** - Framework de testes

### Cenários Cobertos
1. ✅ Autorizar pagamento com sucesso
2. ✅ Capturar pagamento autorizado
3. ✅ Consultar transação por ID
4. ✅ Cancelar transação capturada
5. ✅ Listar transações com filtros
6. ✅ Rejeitar acesso não autorizado (sem API Key)
7. ✅ Validar campos obrigatórios

### Execução
```bash
mvn test -Dtest=PaymentFlowIntegrationTest
```

### Características
- **Isolamento total** - Cada teste usa banco PostgreSQL limpo
- **Ordem garantida** - Testes executam em sequência lógica
- **Dados reais** - Usa Testcontainers para PostgreSQL real
- **Validação completa** - Status HTTP, JSON response, dados persistidos

---

## 💪 2. Testes de Stress/Carga

### Arquivo
`src/test/java/com/pip/stress/PaymentStressTest.java`

### Tecnologia
- **Gatling** - Framework de testes de carga

### Cenários de Carga
1. **50 usuários/seg** - Autorizações
2. **30 usuários/seg** - Capturas
3. **10 usuários/seg** - Consultas
4. **10 usuários/seg** - Listagens

### Duração
- **1 minuto** - Ramp-up (aumento gradual)
- **4 minutos** - Carga constante
- **Total:** 5 minutos

### Métricas Coletadas
- Tempo de resposta (p50, p95, p99)
- Taxa de sucesso
- Throughput (requisições/seg)
- Erros por tipo

### SLAs Validados
- ✅ 95% das requisições com sucesso
- ✅ 95% das requisições < 2 segundos
- ✅ 99% das requisições < 5 segundos

### Execução
```bash
mvn gatling:test -Dgatling.simulationClass=com.pip.stress.PaymentStressTest
```

### Relatórios
Gerados em: `target/gatling/results/`

---

## 🎯 3. Testes de Cenários BDD

### Arquivos
- `src/test/resources/features/payment.feature` - Cenários em português
- `src/test/java/com/pip/scenarios/PaymentSteps.java` - Step definitions
- `src/test/java/com/pip/scenarios/CucumberTestRunner.java` - Runner

### Tecnologia
- **Cucumber** - Framework BDD (Behavior-Driven Development)

### Cenários Implementados

#### Cenários Simples
1. ✅ Autorizar pagamento com sucesso
2. ✅ Capturar pagamento autorizado
3. ✅ Cancelar pagamento capturado
4. ✅ Rejeitar pagamento com cartão inválido
5. ✅ Validar campos obrigatórios
6. ✅ Listar transações com filtros

#### Cenários com Exemplos (Data-Driven)
1. ✅ Processar pagamentos com diferentes valores
   - R$ 10,00 → autorizado
   - R$ 100,00 → autorizado
   - R$ 1.000,00 → autorizado
   - R$ 0,00 → rejeitado
   - R$ -10,00 → rejeitado

2. ✅ Testar diferentes bandeiras de cartão
   - Visa
   - Mastercard
   - Elo
   - Amex

### Execução
```bash
mvn test -Dtest=CucumberTestRunner
```

### Relatórios
- **HTML:** `target/cucumber-reports/cucumber.html`
- **JSON:** `target/cucumber-reports/cucumber.json`

---

## 📊 Cobertura de Testes

### Endpoints Testados
- ✅ `POST /v1/payments/authorize` - Autorização
- ✅ `POST /v1/payments/{id}/capture` - Captura
- ✅ `POST /v1/payments/{id}/void` - Cancelamento
- ✅ `GET /v1/payments/{id}` - Consulta por ID
- ✅ `GET /v1/payments` - Listagem com filtros

### Cenários de Segurança
- ✅ Acesso sem API Key (401)
- ✅ API Key inválida (401)
- ✅ Validação de campos obrigatórios (400)
- ✅ Valores inválidos (400)

### Cenários de Negócio
- ✅ Fluxo completo: autorizar → capturar → consultar → cancelar
- ✅ Diferentes valores de pagamento
- ✅ Diferentes bandeiras de cartão
- ✅ Listagem com filtros

---

## 🚀 Execução Completa

### Todos os Testes
```bash
mvn clean test
```

### Apenas Testes de Integração
```bash
mvn test -Dtest=*IntegrationTest
```

### Apenas Testes de Stress
```bash
mvn gatling:test
```

### Apenas Testes BDD
```bash
mvn test -Dtest=CucumberTestRunner
```

---

## 📈 Métricas e Relatórios

### JUnit Reports
- **Localização:** `target/surefire-reports/`
- **Formato:** XML, TXT, HTML

### Gatling Reports
- **Localização:** `target/gatling/results/`
- **Formato:** HTML interativo com gráficos

### Cucumber Reports
- **Localização:** `target/cucumber-reports/`
- **Formato:** HTML + JSON

### Cobertura de Código (JaCoCo)
```bash
mvn jacoco:report
```
- **Localização:** `target/site/jacoco/index.html`

---

## 🔧 Configuração

### Dependências Adicionadas

```xml
<!-- REST Assured -->
<dependency>
    <groupId>io.rest-assured</groupId>
    <artifactId>rest-assured</artifactId>
    <version>5.3.2</version>
</dependency>

<!-- Cucumber -->
<dependency>
    <groupId>io.cucumber</groupId>
    <artifactId>cucumber-java</artifactId>
    <version>7.14.0</version>
</dependency>

<!-- Gatling -->
<dependency>
    <groupId>io.gatling.highcharts</groupId>
    <artifactId>gatling-charts-highcharts</artifactId>
    <version>3.9.5</version>
</dependency>

<!-- Testcontainers -->
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>postgresql</artifactId>
    <version>1.19.3</version>
</dependency>
```

---

## ✅ Próximos Passos

1. **Integração com Jenkins** - Execução automática no CI/CD
2. **Testes de Gateways Reais** - Quando credenciais estiverem disponíveis
3. **Testes de Segurança** - OWASP ZAP, penetration testing
4. **Testes de Performance** - JMeter adicional
5. **Testes de Contrato** - Pact para contratos de API

---

## 📝 Notas Importantes

### Para Produção
- Configurar timeouts adequados
- Usar dados de teste válidos dos gateways
- Configurar rate limiting nos testes
- Executar testes de stress em horários de baixo tráfego

### Manutenção
- Atualizar cenários quando adicionar novos endpoints
- Revisar SLAs periodicamente
- Manter dados de teste atualizados
- Documentar novos cenários

---

**Testes automatizados completos e prontos para integração com CI/CD!** ✅
