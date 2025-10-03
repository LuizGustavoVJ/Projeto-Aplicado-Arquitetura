# Jenkins + Docker + Kubernetes - Infraestrutura Completa

**Data:** 03/10/2025  
**Versão:** 1.0.0  
**Status:** ✅ Completo

---

## 📋 Visão Geral

Implementação completa de CI/CD com Jenkins, Docker e Kubernetes, cobrindo **20+ casos de uso** específicos para um sistema de pagamentos de nível empresarial.

---

## 🎯 Casos de Uso Implementados

### 1. ✅ Checkout e Validação
- Clone do repositório
- Validação de estrutura do projeto
- Extração de versão
- Auditoria de início de build

### 2. ✅ Build e Compilação
- Compilação Maven
- Resolução de dependências
- Geração de artefatos

### 3. ✅ Testes Unitários
- Execução de testes JUnit
- Publicação de relatórios
- Cobertura de código (JaCoCo)

### 4. ✅ Análise de Qualidade (SonarQube)
- Análise estática de código
- Quality Gate
- Métricas de qualidade

### 5. ✅ Security Scanning
- **OWASP Dependency Check** - Vulnerabilidades em dependências
- **Trivy** - Scan de imagens Docker

### 6. ✅ Compliance PCI-DSS
- Validação de conformidade
- Verificação de dados sensíveis
- Relatórios de compliance

### 7. ✅ Testes de Integração
- Testes end-to-end
- Testcontainers
- Validação de fluxos completos

### 8. ✅ Testes de Performance
- Gatling stress tests
- Validação de SLAs
- Métricas de performance

### 9. ✅ Build Docker Image
- Multi-stage build
- Otimização de camadas
- Labels e metadata

### 10. ✅ Push Docker Image
- Push para GitHub Container Registry
- Versionamento semântico
- Tag latest

### 11. ✅ Deploy para Desenvolvimento
- Deploy automático para DEV
- Rollout status
- Validação de pods

### 12. ✅ Smoke Tests (DEV)
- Health checks
- Validação de endpoints
- Testes básicos de funcionalidade

### 13. ✅ Deploy para Staging
- Deploy automático para STAGING
- Ambiente de pré-produção
- Validação completa

### 14. ✅ Smoke Tests (STAGING)
- Testes em ambiente staging
- Validação de integrações
- Performance testing

### 15. ✅ Aprovação para Produção
- Aprovação manual (4-eyes principle)
- Timeout de 24 horas
- Auditoria de aprovação

### 16. ✅ Canary Deployment
- Deploy gradual (10% do tráfego)
- Monitoramento de métricas
- Validação automática

### 17. ✅ Full Deployment (PROD)
- Expansão para 100% do tráfego
- Blue-green deployment
- Zero-downtime deployment

### 18. ✅ Post-Deploy Tests
- Testes pós-deploy em produção
- Validação de funcionalidades críticas
- Smoke tests finais

### 19. ✅ Health Checks
- Verificação de saúde da aplicação
- Liveness e readiness probes
- Monitoramento contínuo

### 20. ✅ Audit & Compliance Report
- Geração de relatórios de auditoria
- Arquivamento de logs
- Rastreabilidade completa

---

## 📁 Estrutura de Arquivos

```
pip-produto/
├── Jenkinsfile                          # Pipeline principal
├── docker/
│   └── Dockerfile                       # Multi-stage Docker build
├── docker-compose.yml                   # Ambiente local completo
├── kubernetes/
│   ├── base/
│   │   ├── deployment.yaml             # Deployment base
│   │   ├── service.yaml                # Service
│   │   ├── ingress.yaml                # Ingress com TLS
│   │   ├── configmap.yaml              # ConfigMap
│   │   ├── secret.yaml                 # Secrets
│   │   └── kustomization.yaml          # Kustomize base
│   └── overlays/
│       ├── dev/                        # Overlay DEV
│       ├── staging/                    # Overlay STAGING
│       └── prod/                       # Overlay PROD
└── scripts/
    ├── deployment/
    │   ├── smoke-tests.sh              # Smoke tests
    │   ├── canary-deploy.sh            # Canary deployment
    │   ├── validate-canary.sh          # Validação canary
    │   ├── health-checks.sh            # Health checks
    │   └── post-deploy-tests.sh        # Testes pós-deploy
    ├── compliance/
    │   ├── pci-dss-validation.sh       # Validação PCI-DSS
    │   └── generate-audit-report.sh    # Relatórios de auditoria
    └── security/
        └── (scripts de segurança)
```

---

## 🚀 Como Usar

### 1. Ambiente Local (Docker Compose)

```bash
# Subir ambiente completo (app + postgres + redis + rabbitmq + jenkins)
docker-compose up -d

# Acessar aplicação
http://localhost:8080

# Acessar Jenkins
http://localhost:8081

# Acessar RabbitMQ Management
http://localhost:15672
```

### 2. Build e Push Docker

```bash
# Build da imagem
docker build -t ghcr.io/luizgustavovj/pip:latest -f docker/Dockerfile .

# Push para registry
docker push ghcr.io/luizgustavovj/pip:latest
```

### 3. Deploy Kubernetes

```bash
# Deploy para DEV
kubectl apply -k kubernetes/overlays/dev

# Deploy para STAGING
kubectl apply -k kubernetes/overlays/staging

# Deploy para PROD (após aprovação)
kubectl apply -k kubernetes/overlays/prod
```

### 4. Executar Testes Manualmente

```bash
# Smoke tests
bash scripts/deployment/smoke-tests.sh pip-dev

# Validação PCI-DSS
bash scripts/compliance/pci-dss-validation.sh

# Health checks
bash scripts/deployment/health-checks.sh pip-prod
```

---

## 🔒 Segurança

### Medidas Implementadas

1. **Container Security**
   - Usuário não-root
   - Read-only filesystem
   - Capabilities dropped
   - Security context restrito

2. **Network Security**
   - TLS obrigatório (Ingress)
   - Cert-manager para certificados
   - Network policies (opcional)

3. **Secrets Management**
   - Kubernetes Secrets
   - Não commitar secrets no Git
   - Rotação automática (futuro)

4. **Compliance**
   - Validação PCI-DSS automática
   - OWASP Dependency Check
   - Trivy container scanning
   - Auditoria completa

5. **Access Control**
   - Aprovação manual para produção
   - 4-eyes principle
   - RBAC no Kubernetes

---

## 📊 Monitoramento

### Métricas Expostas

- **Actuator Endpoints:**
  - `/actuator/health` - Health check
  - `/actuator/metrics` - Métricas
  - `/actuator/prometheus` - Métricas Prometheus

### Integração com Prometheus/Grafana

```yaml
# ServiceMonitor para Prometheus Operator
apiVersion: monitoring.coreos.com/v1
kind: ServiceMonitor
metadata:
  name: pip-metrics
spec:
  selector:
    matchLabels:
      app: pip
  endpoints:
  - port: http
    path: /actuator/prometheus
```

---

## 🔄 Pipeline Flow

```
┌─────────────────────────────────────────────────────────────┐
│ 1. Checkout & Validation                                    │
└──────────────────┬──────────────────────────────────────────┘
                   │
┌──────────────────▼──────────────────────────────────────────┐
│ 2. Build & Compile                                          │
└──────────────────┬──────────────────────────────────────────┘
                   │
┌──────────────────▼──────────────────────────────────────────┐
│ 3. Unit Tests + 4. Code Quality + 5. Security Scan          │
│    (Paralelo)                                               │
└──────────────────┬──────────────────────────────────────────┘
                   │
┌──────────────────▼──────────────────────────────────────────┐
│ 6. PCI-DSS Compliance                                       │
└──────────────────┬──────────────────────────────────────────┘
                   │
┌──────────────────▼──────────────────────────────────────────┐
│ 7. Integration Tests + 8. Performance Tests                 │
└──────────────────┬──────────────────────────────────────────┘
                   │
┌──────────────────▼──────────────────────────────────────────┐
│ 9. Build Docker + 10. Push Docker                           │
└──────────────────┬──────────────────────────────────────────┘
                   │
┌──────────────────▼──────────────────────────────────────────┐
│ 11. Deploy DEV + 12. Smoke Tests DEV                        │
└──────────────────┬──────────────────────────────────────────┘
                   │
┌──────────────────▼──────────────────────────────────────────┐
│ 13. Deploy STAGING + 14. Smoke Tests STAGING                │
└──────────────────┬──────────────────────────────────────────┘
                   │
┌──────────────────▼──────────────────────────────────────────┐
│ 15. Approval for PROD (Manual)                              │
└──────────────────┬──────────────────────────────────────────┘
                   │
┌──────────────────▼──────────────────────────────────────────┐
│ 16. Canary Deployment (10%)                                 │
│     - Wait 5 minutes                                        │
│     - Validate metrics                                      │
└──────────────────┬──────────────────────────────────────────┘
                   │
┌──────────────────▼──────────────────────────────────────────┐
│ 17. Full Deployment PROD (100%)                             │
└──────────────────┬──────────────────────────────────────────┘
                   │
┌──────────────────▼──────────────────────────────────────────┐
│ 18. Post-Deploy Tests + 19. Health Checks                   │
└──────────────────┬──────────────────────────────────────────┘
                   │
┌──────────────────▼──────────────────────────────────────────┐
│ 20. Audit & Compliance Report                               │
└─────────────────────────────────────────────────────────────┘
```

---

## 🛠️ Configuração do Jenkins

### 1. Instalar Plugins Necessários

- Docker Pipeline
- Kubernetes CLI
- SonarQube Scanner
- JUnit
- JaCoCo
- Gatling
- Slack Notification (opcional)

### 2. Configurar Credentials

```
github-token        : GitHub Personal Access Token
azure-sp            : Azure Service Principal
sonarqube-token     : SonarQube Token
```

### 3. Configurar Webhook GitHub

```
URL: https://jenkins.example.com/github-webhook/
Events: Push, Pull Request
```

---

## 📈 Próximos Passos

1. **Integrar com Azure Key Vault**
   - Rotação automática de chaves
   - Secrets management

2. **Adicionar Prometheus + Grafana**
   - Dashboards personalizados
   - Alertas

3. **Implementar GitOps com ArgoCD**
   - Sync automático
   - Rollback declarativo

4. **Adicionar Chaos Engineering**
   - Chaos Mesh
   - Testes de resiliência

5. **Implementar Service Mesh (Istio)**
   - Traffic management
   - Observability
   - Security

---

## ✅ Checklist de Produção

- [ ] Jenkins configurado e rodando
- [ ] Kubernetes cluster provisionado
- [ ] Credentials configuradas no Jenkins
- [ ] SonarQube instalado e configurado
- [ ] Trivy instalado
- [ ] Cert-manager instalado (para TLS)
- [ ] Ingress controller instalado (nginx)
- [ ] Prometheus + Grafana (opcional)
- [ ] Slack/Teams webhook configurado (opcional)
- [ ] Azure Key Vault provisionado
- [ ] Credenciais de gateways em Secrets

---

**Infraestrutura CI/CD completa e pronta para produção!** 🚀
